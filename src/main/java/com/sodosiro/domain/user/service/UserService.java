package com.sodosiro.domain.user.service;

import com.sodosiro.domain.user.controller.dto.request.ProfileRequest;
import com.sodosiro.domain.user.controller.dto.response.ProfileResponse;
import com.sodosiro.domain.user.entity.User;
import com.sodosiro.domain.user.service.event.ProfileImageChangedEvent;
import com.sodosiro.domain.user.repository.UserRepository;
import com.sodosiro.global.payload.code.error.UserErrorCode;
import com.sodosiro.global.payload.exception.GeneralException;
import com.sodosiro.global.s3.constants.FileFolder;
import com.sodosiro.global.s3.service.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final S3Service s3Service;
    private final ApplicationEventPublisher eventPublisher;


    @Transactional
    public void clearFcmToken(Long userId) {
        User user = findUserOrThrow(userId);
        user.clearFcmToken();
    }

    private User findUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorCode._USER_NOT_FOUND));
    }

    @Transactional
    public void deleteUserData(Long userId) {
        User user = findUserOrThrow(userId);

        // 추가예정
        // 알림
        // 리뷰
        // 여행지 목록
        // 빙고 등등
        userRepository.delete(user);
    }

    @Transactional(readOnly = true)
    public ProfileResponse getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorCode._USER_NOT_FOUND));

        return ProfileResponse.from(user);
    }

    @Transactional
    public ProfileResponse updateProfile(Long userId, ProfileRequest request, MultipartFile image) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorCode._USER_NOT_FOUND));

        validateNicknameAvailable(userId, request.nickName());
        user.updateProfile(request.nickName(), request.introduction());

        if (image != null && !image.isEmpty()) {
            String newUrl = s3Service.uploadFile(image, FileFolder.PROFILES);
            String oldUrl = user.getProfileImageUrl();

            user.updateProfileImage(newUrl);
            eventPublisher.publishEvent(ProfileImageChangedEvent.updated(newUrl, oldUrl));
        } else if (request.removeImage()) {
            String oldUrl = user.getProfileImageUrl();

            if (oldUrl != null && !oldUrl.isBlank()) {
                user.updateProfileImage(null);
                eventPublisher.publishEvent(ProfileImageChangedEvent.removed(oldUrl));
            }
        }

        return ProfileResponse.from(user);
    }

    private void validateNicknameAvailable(Long userId, String nickName) {
        if (nickName != null && userRepository.existsByNickNameAndUserIdNot(nickName, userId)) {
            throw new GeneralException(UserErrorCode._DUPLICATE_NICKNAME);
        }
    }

}
