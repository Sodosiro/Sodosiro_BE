package com.sodosiro.domain.user.entity;

import com.sodosiro.domain.auth.contoller.dto.response.SocialUserInfo;
import com.sodosiro.domain.user.constants.Role;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.*;
import org.hibernate.annotations.Comment;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 서비스 회원 (소셜 로그인 기반)
 * 실명/닉네임/이메일은 소셜 플랫폼에서 제공받으며, 닉네임 미설정 시 실명을 표시명으로 사용 (미정)
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
@Comment("서비스 회원 (소셜 로그인 기반)")
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_users_email", columnNames = {"email"}),
                @UniqueConstraint(name = "uk_users_nick_name", columnNames = {"nick_name"})
        }
)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    @Comment("회원 PK")
    private Long userId;

    @Column(name = "user_name", nullable = false)
    @Comment("사용자 실명 (소셜에서 가져온 이름)")
    private String name;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    @Comment("가입일시")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @Comment("정보 수정일시")
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    @Comment("권한 (USER / ADMIN)")
    private Role role;

    @Column(name = "stamp_num")
    @Comment("보유 스탬프 수")
    private Byte stampNum;

    @Column(name = "profile_image_url")
    @Comment("프로필 이미지 URL")
    private String profileImageUrl;

    @Column(name = "nick_name")
    @Comment("닉네임 (미설정 시 실명 사용)")
    private String nickName;

    @Email
    @Column(name = "email", nullable = true)
    @Comment("이메일 (소셜 제공 여부에 따라 nullable)")
    private String email;

    @Column(name = "fcm_token")
    @Comment("FCM 푸시 토큰")
    private String fcmToken;

    @Column(name = "introduction", length = 100)
    @Comment("한줄소개")
    private String introduction;

    @Builder.Default
    @Column(name = "exp", nullable = false)
    @Comment("경험치")
    private Integer exp = 0;

    @Builder.Default
    @Column(name = "level", nullable = false)
    @Comment("레벨")
    private Integer level = 1;

    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Comment("연결된 소셜 계정 목록")
    private List<SocialAccounts> socialAccounts = new ArrayList<>();

    public void updateProfile(String nickName, String introduction) {
        if (nickName != null) {
            this.nickName = nickName;
        }
        if (introduction != null) {
            this.introduction = introduction;
        }
    }

    public String getUserIdAsString() {
        return String.valueOf(userId);
    }

    public void updateProfileImage(String url) {
        this.profileImageUrl = url;
    }

    public static User createUser(SocialUserInfo info, String nickName) {

        return User.builder()
                .email(info.getEmail())
                .name(info.getNickname())
                .nickName(nickName)
                .role(Role.USER)
                .socialAccounts(new ArrayList<>())
                .build();
    }

    public void updateFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    public void clearFcmToken() {
        this.fcmToken = null;
    }

    public String getDisplayName() {
        return nickName != null ? nickName : name;
    }
}
