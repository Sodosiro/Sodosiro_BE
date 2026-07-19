package com.sodosiro.domain.user.service;

import com.sodosiro.domain.user.entity.User;
import com.sodosiro.domain.user.repository.UserRepository;
import com.sodosiro.global.payload.code.error.UserErrorCode;
import com.sodosiro.global.payload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public void clearFcmToken(Long userId) {
        User user = findUserOrThrow(userId);
        user.clearFcmToken();
    }

    private User findUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorCode._USER_NOT_FOUND));
    }
}
