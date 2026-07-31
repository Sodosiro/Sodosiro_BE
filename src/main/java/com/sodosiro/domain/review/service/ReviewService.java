package com.sodosiro.domain.review.service;

import com.sodosiro.domain.review.constants.ReviewSort;
import com.sodosiro.domain.review.controller.dto.request.ReviewCreateRequest;
import com.sodosiro.domain.review.controller.dto.request.ReviewUpdateRequest;
import com.sodosiro.domain.review.controller.dto.response.MyReviewListResponse;
import com.sodosiro.domain.review.controller.dto.response.MyReviewListResponse.MyReviewResponse;
import com.sodosiro.domain.review.controller.dto.response.ReviewListResponse;
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

    /**
     * Creates a review for a tourist spot and updates its rating statistics.
     *
     * @param userId  the ID of the user submitting the review
     * @param request the review content and rating
     * @param images  images attached to the review
     * @return the created review response
     */
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

        Review review = Review.create(request.contentId(), userId, request.rating().shortValue(), request.body());
        reviewRepository.save(review);

        List<ReviewImage> reviewImages = saveImages(review.getId(), uploadedUrls);

        refreshRatingStats(spot.getContentId());

        User author = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorCode._USER_NOT_FOUND));

        return ReviewResponse.of(review, author, reviewImages, userId);
    }

    /**
     * Retrieves paginated reviews for a tourist spot.
     *
     * @param contentId    the tourist spot identifier
     * @param cursor       the identifier used to continue pagination, or {@code null} to start from the latest reviews
     * @param size         the maximum number of reviews to return
     * @param sort         the review ordering
     * @param loginUserId  the identifier of the logged-in user, if available
     * @return             the reviews, rating statistics, and pagination information
     */
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
                spot.getAvgRating(),
                responses,
                nextCursor,
                hasNext
        );
    }

    /**
     * Retrieves the authenticated user's reviews using cursor-based pagination.
     *
     * @param userId the user whose reviews are retrieved
     * @param cursor the identifier from which to continue retrieval
     * @param size the maximum number of reviews to include
     * @return the user's reviews with pagination information
     */
    @Transactional(readOnly = true)
    public MyReviewListResponse getMyReviews(Long userId, Long cursor, int size) {
        long effectiveCursor = (cursor == null) ? CURSOR_START : cursor;

        List<Review> fetched = reviewRepository.findByUserId(userId, effectiveCursor, size + 1);

        boolean hasNext = fetched.size() > size;
        List<Review> reviews = hasNext ? fetched.subList(0, size) : fetched;
        Long nextCursor = (hasNext && !reviews.isEmpty()) ? reviews.getLast().getId() : null;

        List<Long> contentIds = reviews.stream().map(Review::getContentId).distinct().toList();
        Map<Long, TouristSpot> spotMap = touristSpotRepository.findAllById(contentIds).stream()
                .collect(Collectors.toMap(TouristSpot::getContentId, Function.identity()));

        List<Long> reviewIds = reviews.stream().map(Review::getId).toList();
        Map<Long, List<ReviewImage>> imageMap = groupImagesByReviewId(reviewIds);

        List<MyReviewResponse> responses = reviews.stream()
                .map(r -> MyReviewResponse.of(r, spotMap.get(r.getContentId()), imageMap.getOrDefault(r.getId(), List.of())))
                .toList();

        return new MyReviewListResponse(responses, nextCursor, hasNext);
    }

    /**
     * Updates a review owned by the specified user, including its rating, content, and images.
     *
     * @param userId  the ID of the user updating the review
     * @param reviewId the ID of the review to update
     * @param request the updated rating and review content
     * @param images the replacement images
     * @return the updated review response
     */
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

        review.update(request.rating().shortValue(), request.body());

        refreshRatingStats(review.getContentId());

        User author = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorCode._USER_NOT_FOUND));

        return ReviewResponse.of(review, author, reviewImages, userId);
    }

    /**
     * Deletes a user's review and refreshes the tourist spot's rating statistics.
     *
     * @param userId   the ID of the user requesting deletion
     * @param reviewId the ID of the review to delete
     */
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

    /**
     * Assembles review response objects with their authors, images, and viewer-specific data.
     *
     * @param reviews      the reviews to convert
     * @param loginUserId  the ID of the logged-in user
     * @return             the assembled review responses
     */
    private List<ReviewResponse> assembleReviewResponses(List<Review> reviews, Long loginUserId) {
        if (reviews.isEmpty()) return List.of();

        List<Long> userIds = reviews.stream().map(Review::getUserId).distinct().toList();
        Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getUserId, Function.identity()));

        List<Long> reviewIds = reviews.stream().map(Review::getId).toList();
        Map<Long, List<ReviewImage>> imageMap = groupImagesByReviewId(reviewIds);

        return reviews.stream()
                .map(r -> ReviewResponse.of(
                        r,
                        userMap.get(r.getUserId()),
                        imageMap.getOrDefault(r.getId(), List.of()),
                        loginUserId))
                .toList();
    }

    /**
     * Groups review images by their review ID in display order.
     *
     * @param reviewIds the review IDs whose images are retrieved
     * @return a map from each review ID to its ordered images
     */
    private Map<Long, List<ReviewImage>> groupImagesByReviewId(List<Long> reviewIds) {
        return reviewImageRepository.findAllByReviewIdInOrderByReviewIdAscDisplayOrderAsc(reviewIds).stream()
                .collect(Collectors.groupingBy(ReviewImage::getReviewId));
    }

    /**
     * Uploads review images and returns their stored URLs.
     *
     * @param images the images to upload
     * @return the stored image URLs, or an empty list when no images are provided
     */
    private List<String> uploadImages(List<MultipartFile> images) {
        if (images == null || images.isEmpty()) return List.of();
        return s3Service.uploadFiles(images, FileFolder.REVIEWS);
    }

    /**
     * Persists review images with their display order.
     *
     * @param reviewId the identifier of the review associated with the images
     * @param urls     the image URLs to persist
     * @return the saved review image entities, or an empty list when no URLs are provided
     */
    private List<ReviewImage> saveImages(Long reviewId, List<String> urls) {
        if (urls.isEmpty()) return List.of();
        List<ReviewImage> entities = IntStream.range(0, urls.size())
                .mapToObj(i -> ReviewImage.of(reviewId, urls.get(i), i))
                .toList();
        return reviewImageRepository.saveAll(entities);
    }

    /**
     * Recalculates and updates the tourist spot's average rating and active review count.
     *
     * @param contentId the tourist spot identifier
     */
    private void refreshRatingStats(Long contentId) {
        Double avg   = reviewRepository.avgRatingByContentId(contentId);
        long count   = reviewRepository.countActiveByContentId(contentId);
        BigDecimal rounded = avg == null
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP);
        touristSpotRepository.updateRatingStats(contentId, rounded, (int) count);
    }

    /**
     * Validates that the supplied image list does not exceed the maximum image count.
     *
     * @param images the images to validate
     * @throws GeneralException if the image count exceeds the maximum
     */
    private void validateImageCount(List<MultipartFile> images) {
        if (images != null && images.size() > MAX_IMAGE_COUNT) {
            throw new GeneralException(ReviewErrorCode._REVIEW_IMAGE_LIMIT_EXCEEDED);
        }
    }
}
