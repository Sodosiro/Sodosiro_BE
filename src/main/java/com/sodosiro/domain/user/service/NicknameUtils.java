package com.sodosiro.domain.user.service;

import com.sodosiro.global.payload.code.error.UserErrorCode;
import com.sodosiro.global.payload.exception.GeneralException;
import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** 닉네임이 한글/영문/숫자로만 구성되어 있는지 검사한다. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class NicknameUtils {

    private static final Pattern NICKNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9가-힣]+$");

    public static void validateFormat(String nickName) {
        if (!NICKNAME_PATTERN.matcher(nickName).matches()) {
            throw new GeneralException(UserErrorCode._NICKNAME_BAD_REQUEST);
        }
    }
}
