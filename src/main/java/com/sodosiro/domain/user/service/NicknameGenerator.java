package com.sodosiro.domain.user.service;

import com.sodosiro.domain.user.repository.UserRepository;
import com.sodosiro.global.payload.code.error.UserErrorCode;
import com.sodosiro.global.payload.exception.GeneralException;
import java.security.SecureRandom;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 최초 로그인 시 부여하는 기본 닉네임("소도시로#" + 영문자/숫자 5자리, 총 10자)을 DB 중복 없이 생성한다. */
@Component
@RequiredArgsConstructor
public class NicknameGenerator {

    private static final String NICKNAME_PREFIX = "소도시로#";
    private static final String SUFFIX_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int SUFFIX_LENGTH = 5;
    private static final int MAX_ATTEMPTS = 20;

    private final UserRepository userRepository;
    private final SecureRandom random = new SecureRandom();

    public String generateUnique() {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String candidate = NICKNAME_PREFIX + randomSuffix();
            if (!userRepository.existsByNickName(candidate)) {
                return candidate;
            }
        }
        throw new GeneralException(UserErrorCode._NICKNAME_GENERATION_FAILED);
    }

    private String randomSuffix() {
        StringBuilder suffix = new StringBuilder(SUFFIX_LENGTH);
        for (int i = 0; i < SUFFIX_LENGTH; i++) {
            suffix.append(SUFFIX_CHARS.charAt(random.nextInt(SUFFIX_CHARS.length())));
        }
        return suffix.toString();
    }
}
