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
