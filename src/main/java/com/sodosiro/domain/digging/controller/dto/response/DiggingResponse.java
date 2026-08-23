package com.sodosiro.domain.digging.controller.dto.response;

import com.sodosiro.domain.digging.entity.Digging;
import com.sodosiro.domain.digging.entity.DiggingImage;
import com.sodosiro.domain.travel.entity.TouristSpot;
import com.sodosiro.domain.user.entity.User;
import java.time.LocalDateTime;
import java.util.List;

public record DiggingResponse(
        Long diggingId,
        Long courseId,
        AuthorInfo author,
        SpotSummary spot,
        String body,
        List<DiggingImageResponse> images,
        int likeCount,
        boolean isLikedByMe,
        boolean isSpotLikedByMe,
        boolean isMyDigging,
        boolean isGpsVerified,
        LocalDateTime createdAt
) {
    public record AuthorInfo(Long userId, String displayName, String profileImageUrl) {
        public static AuthorInfo from(User user) {
            if (user == null) {
                return null;
            }
            return new AuthorInfo(user.getUserId(), user.getDisplayName(), user.getProfileImageUrl());
        }
    }

    public record SpotSummary(Long contentId, String title, String firstImage, Integer category, int likeCount) {
        public static SpotSummary from(TouristSpot spot) {
            if (spot == null) {
                return null;
            }
            return new SpotSummary(
                    spot.getContentId(), spot.getTitle(), spot.getFirstImage(), spot.getCategory(), spot.getLikeCount());
        }
    }

    public record DiggingImageResponse(String imageUrl, int order) {
        public static DiggingImageResponse from(DiggingImage image) {
            return new DiggingImageResponse(image.getImageUrl(), image.getDisplayOrder());
        }
    }

    public static DiggingResponse of(
            Digging digging,
            User author,
            TouristSpot spot,
            List<DiggingImage> images,
            boolean liked,
            boolean spotLiked,
            boolean gpsVerified,
            Long loginUserId) {
        return new DiggingResponse(
                digging.getId(),
                digging.getCourseId(),
                AuthorInfo.from(author),
                SpotSummary.from(spot),
                digging.getBody(),
                images.stream().map(DiggingImageResponse::from).toList(),
                digging.getLikeCount(),
                liked,
                spotLiked,
                digging.getUserId().equals(loginUserId),
                gpsVerified,
                digging.getCreatedAt()
        );
    }
}
