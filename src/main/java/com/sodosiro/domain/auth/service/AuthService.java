package com.sodosiro.domain.auth.service;

import com.sodosiro.domain.auth.contoller.dto.response.ReissueResponse;
import com.sodosiro.domain.auth.contoller.dto.response.SocialLoginResponse;
import com.sodosiro.domain.auth.contoller.dto.response.SocialUserInfo;
import com.sodosiro.domain.auth.oauth.validator.SocialVerifier;
import com.sodosiro.domain.jwt.JwtGenerator;
import com.sodosiro.domain.jwt.JwtProvider;
import com.sodosiro.domain.jwt.JwtToken;
import com.sodosiro.domain.user.constants.Provider;
import com.sodosiro.domain.user.entity.SocialAccounts;
import com.sodosiro.domain.user.entity.User;
import com.sodosiro.domain.user.repository.SocialRepository;
import com.sodosiro.domain.user.repository.UserRepository;
import com.sodosiro.domain.user.service.NicknameGenerator;
import com.sodosiro.domain.user.service.UserService;
import com.sodosiro.domain.user.service.event.WithdrawEvent;
import com.sodosiro.global.payload.code.error.AuthErrorCode;
import com.sodosiro.global.payload.code.error.UserErrorCode;
import com.sodosiro.global.payload.exception.GeneralException;
import com.sodosiro.global.service.RedisService;
import com.sodosiro.global.utils.TokenKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtProvider jwtProvider;
    private final JwtGenerator jwtGenerator;
    private final UserRepository userRepository;
    private final List<SocialVerifier> socialVerifiers;
    private final RedisService redisService;
    private final SocialRepository socialRepository;
    private final UserService userService;
    private final NicknameGenerator nicknameGenerator;
    private final ApplicationEventPublisher eventPublisher;


    @Value("${spring.jwt.refresh-token-expiration-millis}")
    private long refreshTokenExpirationMillis;

    @Value("${spring.jwt.access-token-expiration-millis}")
    private int accessTokenExpirationMillis;

    @Transactional
    public SocialLoginResponse loginWithSocial(String providerName, String idToken, String authorizationCode) {

        Provider provider = Provider.from(providerName);

        SocialVerifier verifier = socialVerifiers.stream()
                .filter(v -> v.getProvider() == provider)
                .findFirst()
                .orElseThrow(() -> new GeneralException(AuthErrorCode._UNSUPPORTED_SOCIAL_PROVIDER));

        String actualIdToken = idToken;
        String refreshToken = null;

        if (authorizationCode != null && !authorizationCode.isBlank()) {
            Map<String, String> tokens = verifier.exchangeCodeForTokens(authorizationCode);
            actualIdToken = tokens.get("id_token");
            refreshToken = tokens.get("refresh_token");
        }

        if (actualIdToken == null || actualIdToken.isBlank()) {
            throw new GeneralException(AuthErrorCode._SOCIAL_ID_TOKEN_MISSING);
        }

        SocialUserInfo socialUser = verifier.verify(actualIdToken);

        String email = socialUser.getEmail();
        if (email == null) {
            throw new GeneralException(AuthErrorCode._SOCIAL_EMAIL_NOT_PROVIDED);
        }
        Optional<User> userOptional = userRepository.findByEmail(email);

        User user = userOptional.orElseGet(() ->
                userRepository.save(User.createUser(socialUser, nicknameGenerator.generateUnique())));

        SocialAccounts socialAccount = socialRepository
                .findByProviderAndProviderId(provider, socialUser.getProviderId())
                .orElse(null);

        if (socialAccount == null) {
            socialAccount = SocialAccounts.create(user, socialUser, refreshToken);
            socialRepository.save(socialAccount);
        } else {
            if (refreshToken != null && !refreshToken.isBlank()) {
                socialAccount.setRefreshToken(refreshToken);
            }
        }
        JwtToken jwt = jwtGenerator.generateToken(user, user.getRole());

        redisService.save(TokenKeys.refreshKey(jwt.getRefreshToken()),
                String.valueOf(user.getUserId()),
                refreshTokenExpirationMillis);

        return SocialLoginResponse.of(jwt.getAccessToken(), jwt.getRefreshToken());
    }

    @Transactional
    public ReissueResponse reissueToken(String refreshToken) {

        User user = getUserFromRefreshToken(refreshToken);
        String newAccessToken = jwtGenerator.createAccessToken(user, user.getRole());

        return ReissueResponse.of(newAccessToken,refreshToken);
    }

    private User getUserFromRefreshToken(String refreshToken) {

        if (refreshToken == null || refreshToken.isEmpty()) {
            throw new GeneralException(UserErrorCode._INVALID_REFRESH_TOKEN);
        }

        jwtProvider.validateRefreshToken(refreshToken);
        String userId = redisService.getValue(TokenKeys.refreshKey(refreshToken));

        if (userId == null) {
            throw new GeneralException(UserErrorCode._INVALID_USER_REFRESH_TOKEN);
        }

        return userRepository.findById(Long.valueOf(userId))
                .orElseThrow(() -> new GeneralException(UserErrorCode._USER_NOT_FOUND));
    }

    @Transactional
    public void logout(Long userId, String accessToken, String refreshToken) {

        validateRefreshTokenOwner(userId, refreshToken);
        userService.clearFcmToken(userId);
        clearSession(accessToken, refreshToken);
    }


    private void validateRefreshTokenOwner(Long userId, String refreshToken) {
        if (refreshToken == null || refreshToken.isEmpty()) {
            throw new GeneralException(UserErrorCode._INVALID_REFRESH_TOKEN);
        }
        jwtProvider.validateRefreshToken(refreshToken);
        String storedUserId = redisService.getValue(TokenKeys.refreshKey(refreshToken));
        if (storedUserId == null || !storedUserId.equals(String.valueOf(userId))) {
            throw new GeneralException(UserErrorCode._INVALID_USER_REFRESH_TOKEN);
        }
    }

    private void clearSession(String accessToken, String refreshToken) {
        long remainTime = jwtProvider.getExpiration(accessToken);
        if (remainTime > 0) {
            redisService.save(TokenKeys.blacklistKey(accessToken), "logout", remainTime);
        }
        redisService.deleteKey(TokenKeys.refreshKey(refreshToken));
    }

    @Transactional
    public void withdraw(Long userId, String accessToken, String refreshToken) {
        validateRefreshTokenOwner(userId, refreshToken);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorCode._USER_NOT_FOUND));

        List<WithdrawEvent.SocialInfo> socialInfos = socialRepository.findAllByUser(user).stream()
                .map(s -> new WithdrawEvent.SocialInfo(s.getProvider(), s.getProviderId(), s.getRefreshToken()))
                .toList();

        userService.deleteUserData(userId);
        eventPublisher.publishEvent(new WithdrawEvent(userId, accessToken, refreshToken, socialInfos));
    }


}
