package com.sodosiro.domain.user.service.dto;

import java.util.List;

public record PurgedUserFootprint(
        Long userId,
        List<String> imageUrls,
        int deletedReviews,
        int deletedDiggings,
        int deletedSpotLikes,
        int deletedCourses
) {
}
