package com.sodosiro.domain.user.dto.response;

import com.sodosiro.domain.user.entity.User;

public record ProfileResponse(
        String nickName,
        String profileImage,
        String introduction
) {
    public static ProfileResponse from(User user) {
        return new ProfileResponse(
                user.getNickName(),
                user.getProfileImageUrl(),
                user.getIntroduction()
        );
    }
}
