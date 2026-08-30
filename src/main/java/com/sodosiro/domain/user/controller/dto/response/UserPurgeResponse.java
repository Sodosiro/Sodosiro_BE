package com.sodosiro.domain.user.controller.dto.response;

import com.sodosiro.domain.user.service.dto.UserPurgeResult;

public record UserPurgeResponse(
        int targetedUsers,
        int purgedUsers,
        int failedUsers,
        int deletedReviews,
        int deletedDiggings,
        int deletedSpotLikes,
        int deletedCourses
) {
    public static UserPurgeResponse from(UserPurgeResult result) {
        return new UserPurgeResponse(
                result.targetedUsers(),
                result.purgedUsers(),
                result.failedUsers(),
                result.deletedReviews(),
                result.deletedDiggings(),
                result.deletedSpotLikes(),
                result.deletedCourses()
        );
    }
}
