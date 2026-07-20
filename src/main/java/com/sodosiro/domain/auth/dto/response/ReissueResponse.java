package com.sodosiro.domain.auth.dto.response;

public record ReissueResponse(
        String newAccessToken,
        String refreshToken
) {
    public static ReissueResponse of(String newAccessToken,String refreshToken){
        return new ReissueResponse(newAccessToken, refreshToken);
    }
}
