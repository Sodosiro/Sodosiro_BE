package com.sodosiro.domain.review.service;

import com.sodosiro.domain.review.constants.ReviewSort;
import com.sodosiro.domain.review.controller.dto.request.ReviewCreateRequest;
import com.sodosiro.domain.review.controller.dto.request.ReviewGpsVerificationRequest;
import com.sodosiro.domain.review.controller.dto.request.ReviewUpdateRequest;
import com.sodosiro.domain.review.controller.dto.response.MyReviewListResponse;
import com.sodosiro.domain.review.controller.dto.response.ReviewListResponse;
import com.sodosiro.domain.review.controller.dto.response.ReviewGpsVerificationResponse;
import com.sodosiro.domain.review.controller.dto.response.ReviewResponse;
import com.sodosiro.domain.review.entity.Review;
import com.sodosiro.domain.review.entity.ReviewImage;
import com.sodosiro.domain.review.repository.ReviewImageRepository;
import com.sodosiro.domain.review.repository.ReviewRepository;
import com.sodosiro.domain.review.service.event.ReviewImageChangedEvent;
import com.sodosiro.domain.travel.entity.TouristSpot;
import com.sodosiro.domain.travel.repository.TouristSpotRepository;
import com.sodosiro.domain.user.entity.User;
import com.sodosiro.domain.user.repository.UserRepository;
import com.sodosiro.global.payload.code.error.ReviewErrorCode;
import com.sodosiro.global.payload.code.error.UserErrorCode;
import com.sodosiro.global.payload.exception.GeneralException;
import com.sodosiro.global.s3.constants.FileFolder;
import com.sodosiro.global.s3.service.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private static final int MAX_IMAGE_COUNT = 5;
    private static final Long CURSOR_START = Long.MAX_VALUE;

    private final ReviewRepository reviewRepository;
    private final ReviewImageRepository reviewImageRepository;
    private final TouristSpotRepository touristSpotRepository;
    private final UserRepository userRepository;
    private final S3Service s3Service;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ReviewResponse createReview(Long userId, ReviewCreateRequest request, List<MultipartFile> images) {
        validateImageCount(images);

        TouristSpot spot = touristSpotRepository.findById(request.contentId())
                .orElseThrow(() -> new GeneralException(ReviewErrorCode._SPOT_NOT_FOUND));

        if (reviewRepository.existsByContentIdAndUserIdAndIsDeletedFalse(request.contentId(), userId)) {
            throw new GeneralException(ReviewErrorCode._REVIEW_ALREADY_EXISTS);
        }

        List<String> uploadedUrls = uploadImages(images);
        eventPublisher.publishEvent(ReviewImageChangedEvent.onlyNew(uploadedUrls));

        Review review = Review.create(request.contentId(), userId, request.rating(), request.body());
        reviewRepository.save(review);

        List<ReviewImage> reviewImages = saveImages(review.getId(), uploadedUrls);

        refreshRatingStats(spot.getContentId());

        User author = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorCode._USER_NOT_FOUND));

        return ReviewResponse.of(review, author, spot, reviewImages, userId);
    }

    @Transactional
    public ReviewGpsVerificationResponse verifyGpsVisit(
            Long userId, Long reviewId, ReviewGpsVerificationRequest request) {
        Review review = reviewRepository.findByIdAndIsDeletedFalse(reviewId)
                .orElseThrow(() -> new GeneralException(ReviewErrorCode._REVIEW_NOT_FOUND));
        if (!review.getUserId().equals(userId)) {
            throw new GeneralException(ReviewErrorCode._REVIEW_FORBIDDEN);
        }
        TouristSpot spot = touristSpotRepository.findById(review.getContentId())
                .orElseThrow(() -> new GeneralException(ReviewErrorCode._SPOT_NOT_FOUND));
        if (spot.getMapY() == null || spot.getMapX() == null) {
            throw new GeneralException(ReviewErrorCode._SPOT_LOCATION_UNAVAILABLE);
        }
        if (ReviewGpsVerifier.isWithinVerificationRadius(
                spot.getMapY(), spot.getMapX(), request.latitude(), request.longitude())) {
            review.verifyGpsVisit();
        }
        return ReviewGpsVerificationResponse.from(review);
    }

    @Transactional(readOnly = true)
    public ReviewListResponse getReviews(Long contentId, Long cursor, int size, ReviewSort sort, Long loginUserId) {
        TouristSpot spot = touristSpotRepository.findById(contentId)
                .orElseThrow(() -> new GeneralException(ReviewErrorCode._SPOT_NOT_FOUND));

        long effectiveCursor = (cursor == null) ? CURSOR_START : cursor;

        List<Review> fetched = reviewRepository.findByContentId(contentId, effectiveCursor, size + 1, sort);

        boolean hasNext = fetched.size() > size;
        List<Review> reviews = hasNext ? fetched.subList(0, size) : fetched;
        Long nextCursor = (hasNext && !reviews.isEmpty()) ? reviews.getLast().getId() : null;

        List<ReviewResponse> responses = assembleReviewResponses(reviews, loginUserId);

        return new ReviewListResponse(
                spot.getReviewCount(),
                spot.getAvgRating().setScale(1, RoundingMode.HALF_UP),
                responses,
                nextCursor,
                hasNext
        );
    }

    @Transactional(readOnly = true)
    public MyReviewListResponse getMyReviews(Long userId, Long cursor, int size) {
        long effectiveCursor = (cursor == null) ? CURSOR_START : cursor;

        List<Review> fetched = reviewRepository.findByUserId(userId, effectiveCursor, size + 1);

        boolean hasNext = fetched.size() > size;
        List<Review> reviews = hasNext ? fetched.subList(0, size) : fetched;
        Long nextCursor = (hasNext && !reviews.isEmpty()) ? reviews.getLast().getId() : null;

        return new MyReviewListResponse(assembleReviewResponses(reviews, userId), nextCursor, hasNext);
    }

    @Transactional
    public ReviewResponse updateReview(Long userId, Long reviewId, ReviewUpdateRequest request, List<MultipartFile> images) {
        validateImageCount(images);

        Review review = reviewRepository.findByIdAndIsDeletedFalse(reviewId)
                .orElseThrow(() -> new GeneralException(ReviewErrorCode._REVIEW_NOT_FOUND));

        if (!review.getUserId().equals(userId)) {
            throw new GeneralException(ReviewErrorCode._REVIEW_FORBIDDEN);
        }

        List<String> oldUrls = reviewImageRepository.findAllByReviewIdOrderByDisplayOrderAsc(reviewId).stream()
                .map(ReviewImage::getImageUrl)
                .toList();

        List<String> newUrls = uploadImages(images);
        eventPublisher.publishEvent(ReviewImageChangedEvent.replace(newUrls, oldUrls));

        reviewImageRepository.deleteAllByReviewId(reviewId);
        List<ReviewImage> reviewImages = saveImages(reviewId, newUrls);

        review.update(request.rating(), request.body());

        refreshRatingStats(review.getContentId());

        User author = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorCode._USER_NOT_FOUND));
        TouristSpot spot = touristSpotRepository.findById(review.getContentId())
                .orElseThrow(() -> new GeneralException(ReviewErrorCode._SPOT_NOT_FOUND));

        return ReviewResponse.of(review, author, spot, reviewImages, userId);
    }

    @Transactional
    public void deleteReview(Long userId, Long reviewId) {
        Review review = reviewRepository.findByIdAndIsDeletedFalse(reviewId)
                .orElseThrow(() -> new GeneralException(ReviewErrorCode._REVIEW_NOT_FOUND));

        if (!review.getUserId().equals(userId)) {
            throw new GeneralException(ReviewErrorCode._REVIEW_FORBIDDEN);
        }

        List<String> imageUrls = reviewImageRepository.findAllByReviewIdOrderByDisplayOrderAsc(reviewId).stream()
                .map(ReviewImage::getImageUrl)
                .toList();

        eventPublisher.publishEvent(ReviewImageChangedEvent.onlyOld(imageUrls));

        reviewImageRepository.deleteAllByReviewId(reviewId);
        review.delete();

        refreshRatingStats(review.getContentId());
    }

    private List<ReviewResponse> assembleReviewResponses(List<Review> reviews, Long loginUserId) {
        if (reviews.isEmpty()) return List.of();

        List<Long> userIds = reviews.stream().map(Review::getUserId).distinct().toList();
        Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getUserId, Function.identity()));
        List<Long> contentIds = reviews.stream().map(Review::getContentId).distinct().toList();
        Map<Long, TouristSpot> spotMap = touristSpotRepository.findAllById(contentIds).stream()
                .collect(Collectors.toMap(TouristSpot::getContentId, Function.identity()));

        List<Long> reviewIds = reviews.stream().map(Review::getId).toList();
        Map<Long, List<ReviewImage>> imageMap = groupImagesByReviewId(reviewIds);

        return reviews.stream()
                .map(r -> ReviewResponse.of(
                        r,
                        userMap.get(r.getUserId()),
                        spotMap.get(r.getContentId()),
                        imageMap.getOrDefault(r.getId(), List.of()),
                        loginUserId))
                .toList();
    }

    private Map<Long, List<ReviewImage>> groupImagesByReviewId(List<Long> reviewIds) {
        return reviewImageRepository.findAllByReviewIdInOrderByReviewIdAscDisplayOrderAsc(reviewIds).stream()
                .collect(Collectors.groupingBy(ReviewImage::getReviewId));
    }

    private List<String> uploadImages(List<MultipartFile> images) {
        if (images == null || images.isEmpty()) return List.of();
        return s3Service.uploadFiles(images, FileFolder.REVIEWS);
    }

    private List<ReviewImage> saveImages(Long reviewId, List<String> urls) {
        if (urls.isEmpty()) return List.of();
        List<ReviewImage> entities = IntStream.range(0, urls.size())
                .mapToObj(i -> ReviewImage.of(reviewId, urls.get(i), i))
                .toList();
        return reviewImageRepository.saveAll(entities);
    }

    private void refreshRatingStats(Long contentId) {
        Double avg   = reviewRepository.avgRatingByContentId(contentId);
        long count   = reviewRepository.countActiveByContentId(contentId);
        BigDecimal rounded = avg == null
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP);
        touristSpotRepository.updateRatingStats(contentId, rounded, (int) count);
    }

    private void validateImageCount(List<MultipartFile> images) {
        if (images != null && images.size() > MAX_IMAGE_COUNT) {
            throw new GeneralException(ReviewErrorCode._REVIEW_IMAGE_LIMIT_EXCEEDED);
        }
    }
}
