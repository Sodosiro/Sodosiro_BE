package com.sodosiro.domain.auth.service;

import com.sodosiro.domain.auth.dto.response.ReissueResponse;
import com.sodosiro.domain.auth.dto.response.SocialLoginResponse;
import com.sodosiro.domain.auth.dto.response.SocialUserInfo;
import com.sodosiro.domain.auth.oauth.vaildator.SocialVerifier;
import com.sodosiro.domain.jwt.JwtGenerator;
import com.sodosiro.domain.jwt.JwtProvider;
import com.sodosiro.domain.jwt.JwtToken;
import com.sodosiro.domain.user.constants.Provider;
import com.sodosiro.domain.user.entity.SocialAccounts;
import com.sodosiro.domain.user.entity.User;
import com.sodosiro.domain.user.repository.SocialRepository;
import com.sodosiro.domain.user.repository.UserRepository;
import com.sodosiro.domain.user.service.UserService;
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

        User user = userOptional.orElseGet(() -> userRepository.save(User.createUser(socialUser)));

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
    public ReissueResponse appReissueToken(String refreshToken) {

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


}
