package com.sodosiro.domain.user.service.dto;

public record UserPurgeResult(
        int targetedUsers,
        int purgedUsers,
        int failedUsers,
        int deletedReviews,
        int deletedDiggings,
        int deletedSpotLikes,
        int deletedCourses
) {
}
