package com.sodosiro.domain.user.entity;


import com.sodosiro.domain.auth.contoller.dto.response.SocialUserInfo;
import com.sodosiro.domain.user.constants.Provider;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 소셜 로그인 계정 연결 정보
 * provider + providerId 조합으로 유일성 보장 (uk_social_provider_id)
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Comment("소셜 로그인 계정 연결 정보")
@Table(
        name = "social_accounts",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_social_provider_id",
                columnNames = {"provider", "providerId"}
        )
)
public class SocialAccounts {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "social_id")
    @Comment("소셜 계정 PK")
    private Long socialId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @Comment("연결된 회원")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false)
    @Comment("소셜 플랫폼 (KAKAO)")
    private Provider provider;

    @Column(name = "provider_id", nullable = false)
    @Comment("소셜 플랫폼에서 발급한 사용자 고유 ID")
    private String providerId;

    @Column(name = "refresh_token")
    @Comment("소셜 플랫폼 리프레시 토큰")
    private String refreshToken;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    @Comment("연결일시")
    private LocalDateTime createdAt;

    public static SocialAccounts create(User user, SocialUserInfo info, String refreshToken) {
        return SocialAccounts.builder()
                .user(user)
                .provider(info.getProvider())
                .providerId(info.getProviderId())
                .refreshToken(refreshToken)
                .build();
    }

    public static SocialAccounts of(User user, SocialUserInfo info) {
        SocialAccounts social = new SocialAccounts();
        social.user = user;
        social.provider = info.getProvider();
        social.providerId = info.getProviderId();
        return social;
    }
}
