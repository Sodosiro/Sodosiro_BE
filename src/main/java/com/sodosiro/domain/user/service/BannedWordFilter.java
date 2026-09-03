package com.sodosiro.domain.user.service;

import com.sodosiro.global.payload.code.error.UserErrorCode;
import com.sodosiro.global.payload.exception.GeneralException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/** resources/banned-words.txt에 등록된 금칙어를 정규화하여 포함 여부를 검사한다. */
@Component
public class BannedWordFilter {

    private static final Pattern NON_ALLOWED_CHARS = Pattern.compile("[^a-z0-9가-힣]");
    private static final Set<String> BANNED_WORDS = loadBannedWords();

    public void validate(String content) {
        if (content == null || content.isBlank()) {
            return;
        }

        String normalized = normalize(content);
        boolean containsBannedWord = BANNED_WORDS.stream().anyMatch(normalized::contains);
        if (containsBannedWord) {
            throw new GeneralException(UserErrorCode._NICKNAME_CONTAINS_BANNED_WORD);
        }
    }

    private static Set<String> loadBannedWords() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ClassPathResource("banned-words.txt").getInputStream(), StandardCharsets.UTF_8))) {
            return reader.lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                    .map(BannedWordFilter::normalize)
                    .collect(Collectors.toUnmodifiableSet());
        } catch (IOException e) {
            throw new IllegalStateException("금칙어 목록을 불러오지 못했습니다.", e);
        }
    }

    private static String normalize(String text) {
        return NON_ALLOWED_CHARS.matcher(text.toLowerCase()).replaceAll("");
    }
}
