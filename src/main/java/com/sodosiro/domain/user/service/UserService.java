package com.sodosiro.domain.user.service;

import com.sodosiro.domain.user.controller.dto.request.ProfileRequest;
import com.sodosiro.domain.user.controller.dto.response.ProfileResponse;
import com.sodosiro.domain.notification.repository.UserDeviceRepository;
import com.sodosiro.domain.user.entity.User;
import com.sodosiro.domain.user.service.event.ProfileImageChangedEvent;
import com.sodosiro.domain.user.repository.UserRepository;
import com.sodosiro.global.payload.code.error.UserErrorCode;
import com.sodosiro.global.payload.exception.GeneralException;
import com.sodosiro.global.s3.constants.FileFolder;
import com.sodosiro.global.s3.service.S3Service;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserDeviceRepository userDeviceRepository;
    private final S3Service s3Service;
    private final ApplicationEventPublisher eventPublisher;
    private final BannedWordFilter bannedWordFilter;


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
    public void withdraw(Long userId) {
        User user = findUserOrThrow(userId);
        if (user.isWithdrawn()) {
            throw new GeneralException(UserErrorCode._USER_ALREADY_WITHDRAWN);
        }

        userDeviceRepository.deleteAllByUserId(userId);
        user.withdraw(LocalDateTime.now());
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

        validateNicknameAvailable(userId, user.getNickName(), request.nickName());
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

    private void validateNicknameAvailable(Long userId, String currentNickName, String newNickName) {
        if (newNickName == null || newNickName.equals(currentNickName)) {
            return;
        }

        NicknameUtils.validateFormat(newNickName);
        bannedWordFilter.validate(newNickName);

        if (userRepository.existsByNickNameAndUserIdNot(newNickName, userId)) {
            throw new GeneralException(UserErrorCode._DUPLICATE_NICKNAME);
        }
    }

}
