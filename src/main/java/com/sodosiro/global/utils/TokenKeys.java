package com.sodosiro.global.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class TokenKeys {

    private TokenKeys() {}

    public static String refreshKey(String refreshToken) {
        return "RT:" + sha256Hex(refreshToken);
    }

    public static String blacklistKey(String accessToken) {
        return "BL:" + sha256Hex(accessToken);
    }

    /**
     * 탈퇴한 회원의 userId 마커. 탈퇴 시점에 발급돼 있던 다른 기기의 access token은 블랙리스트에 담기지 않으므로,
     * JWT 검증 단계에서 이 키로 탈퇴 여부를 확인해 차단한다.
     */
    public static String withdrawnKey(Long userId) {
        return withdrawnKey(String.valueOf(userId));
    }

    /** JWT subject를 그대로 받는 오버로드. 필터에서 숫자 파싱 실패로 인증 흐름이 깨지지 않게 한다. */
    public static String withdrawnKey(String userId) {
        return "WITHDRAWN:" + userId;
    }

    private static String sha256Hex(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest){
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not found", e);
        }
    }

}
