package com.sodosiro.domain.user.service;

import com.sodosiro.domain.course.repository.CourseRepository;
import com.sodosiro.domain.digging.entity.Digging;
import com.sodosiro.domain.digging.entity.DiggingImage;
import com.sodosiro.domain.digging.entity.DiggingLike;
import com.sodosiro.domain.digging.repository.DiggingImageRepository;
import com.sodosiro.domain.digging.repository.DiggingLikeRepository;
import com.sodosiro.domain.digging.repository.DiggingRepository;
import com.sodosiro.domain.gps.repository.GpsRepository;
import com.sodosiro.domain.like.entity.SpotLike;
import com.sodosiro.domain.like.repository.SpotLikeRepository;
import com.sodosiro.domain.notification.repository.NotificationDeliveryGuardRepository;
import com.sodosiro.domain.notification.repository.NotificationPreferenceRepository;
import com.sodosiro.domain.notification.repository.NotificationRepository;
import com.sodosiro.domain.notification.repository.UserDeviceRepository;
import com.sodosiro.domain.review.entity.Review;
import com.sodosiro.domain.review.entity.ReviewImage;
import com.sodosiro.domain.review.repository.ReviewImageRepository;
import com.sodosiro.domain.review.repository.ReviewRepository;
import com.sodosiro.domain.review.service.SpotRatingStatsUpdater;
import com.sodosiro.domain.travel.repository.TouristSpotRepository;
import com.sodosiro.domain.user.entity.User;
import com.sodosiro.domain.user.repository.UserRepository;
import com.sodosiro.domain.user.service.dto.PurgedUserFootprint;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class UserPurgeExecutor {

    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewImageRepository reviewImageRepository;
    private final DiggingRepository diggingRepository;
    private final DiggingImageRepository diggingImageRepository;
    private final DiggingLikeRepository diggingLikeRepository;
    private final SpotLikeRepository spotLikeRepository;
    private final CourseRepository courseRepository;
    private final GpsRepository gpsRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationDeliveryGuardRepository notificationDeliveryGuardRepository;
    private final NotificationPreferenceRepository notificationPreferenceRepository;
    private final UserDeviceRepository userDeviceRepository;
    private final TouristSpotRepository touristSpotRepository;
    private final SpotRatingStatsUpdater spotRatingStatsUpdater;

    @Transactional
    public PurgedUserFootprint purge(Long userId) {
        List<String> imageUrls = new ArrayList<>();
        userRepository.findById(userId)
                .map(User::getProfileImageUrl)
                .filter(url -> !url.isBlank())
                .ifPresent(imageUrls::add);

        int deletedReviews = purgeReviews(userId, imageUrls);
        int deletedDiggings = purgeDiggings(userId, imageUrls);
        purgeDiggingLikes(userId);
        int deletedSpotLikes = purgeSpotLikes(userId);
        int deletedCourses = purgeTravelRecords(userId);
        purgeNotifications(userId);

        userRepository.deleteById(userId);

        return new PurgedUserFootprint(
                userId, imageUrls, deletedReviews, deletedDiggings, deletedSpotLikes, deletedCourses);
    }

    /** 리뷰를 지운 뒤, 영향을 받은 관광지의 평점·리뷰 수 집계를 살아 있는 리뷰 기준으로 다시 계산한다. */
    private int purgeReviews(Long userId, List<String> imageUrls) {
        List<Review> reviews = reviewRepository.findAllByUserId(userId);
        if (reviews.isEmpty()) {
            return 0;
        }

        List<Long> reviewIds = reviews.stream().map(Review::getId).toList();
        Set<Long> affectedContentIds = new LinkedHashSet<>(reviews.stream().map(Review::getContentId).toList());

        reviewImageRepository.findAllByReviewIdInOrderByReviewIdAscDisplayOrderAsc(reviewIds).stream()
                .map(ReviewImage::getImageUrl)
                .forEach(imageUrls::add);

        reviewImageRepository.deleteAllByReviewIdIn(reviewIds);
        reviewRepository.deleteAllByUserId(userId);
        reviewRepository.flush();

        spotRatingStatsUpdater.refreshAll(affectedContentIds);
        return reviews.size();
    }

    /** 탈퇴 회원이 쓴 디깅과 그 디깅에 달린 다른 사용자의 좋아요를 함께 지운다. */
    private int purgeDiggings(Long userId, List<String> imageUrls) {
        List<Digging> diggings = diggingRepository.findAllByUserId(userId);
        if (diggings.isEmpty()) {
            return 0;
        }

        List<Long> diggingIds = diggings.stream().map(Digging::getId).toList();

        diggingImageRepository.findAllByDiggingIdInOrderByDiggingIdAscDisplayOrderAsc(diggingIds).stream()
                .map(DiggingImage::getImageUrl)
                .forEach(imageUrls::add);

        diggingLikeRepository.deleteAllByDiggingIdIn(diggingIds);
        diggingImageRepository.deleteAllByDiggingIdIn(diggingIds);
        diggingRepository.deleteAllByUserId(userId);
        return diggings.size();
    }

    /** 탈퇴 회원이 다른 사람 디깅에 남긴 좋아요. 지우기 전에 해당 디깅의 like_count를 되돌린다. */
    private void purgeDiggingLikes(Long userId) {
        List<DiggingLike> likes = diggingLikeRepository.findAllByUserId(userId);
        if (likes.isEmpty()) {
            return;
        }

        likes.stream()
                .map(DiggingLike::getDiggingId)
                .forEach(diggingId -> diggingRepository.findById(diggingId).ifPresent(Digging::decreaseLikeCount));

        diggingLikeRepository.deleteAllByUserId(userId);
    }

    /** 관광지 좋아요. 지우기 전에 tourist_spot.like_count를 되돌린다. */
    private int purgeSpotLikes(Long userId) {
        List<SpotLike> likes = spotLikeRepository.findAllByUserId(userId);
        if (likes.isEmpty()) {
            return 0;
        }

        // spot_like에는 (user_id, content_id) unique 제약이 없어 이론상 중복 행이 가능하다.
        // 좋아요 한 건마다 하나씩 되돌려야 like_count가 어긋나지 않는다(음수는 쿼리 쪽에서 막는다).
        likes.stream()
                .map(SpotLike::getContentId)
                .forEach(touristSpotRepository::decrementLikeCount);

        spotLikeRepository.deleteAllByUserId(userId);
        return likes.size();
    }

    private int purgeTravelRecords(Long userId) {
        gpsRepository.deleteAllByUserId(userId);
        return (int) courseRepository.deleteAllByUserId(userId);
    }

    private void purgeNotifications(Long userId) {
        notificationRepository.deleteAllByUserId(userId);
        notificationDeliveryGuardRepository.deleteAllByUserId(userId);
        notificationPreferenceRepository.deleteById(userId);
        userDeviceRepository.deleteAllByUserId(userId);
    }
}
