package com.sodosiro.domain.badge.service;

import com.sodosiro.domain.badge.controller.dto.BadgeListResponse;
import com.sodosiro.domain.badge.entity.Badge;
import com.sodosiro.domain.badge.entity.UserBadge;
import com.sodosiro.domain.badge.repository.BadgeRepository;
import com.sodosiro.domain.badge.repository.UserBadgeRepository;
import com.sodosiro.domain.region.repository.SigunguCodeRepository;
import com.sodosiro.domain.travel.entity.SigunguCode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 소도시 지역 뱃지 조회 및 GPS 인증 연동 지급. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BadgeService {

    private final BadgeRepository badgeRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final SigunguCodeRepository sigunguCodeRepository;

    public BadgeListResponse getBadges(Long userId) {
        List<Badge> allBadges = badgeRepository.findAll();

        Map<Long, java.time.LocalDateTime> earnedAtByBadgeId = userBadgeRepository.findAllByUserId(userId).stream()
                .collect(Collectors.toMap(UserBadge::getBadgeId, UserBadge::getEarnedAt));

        List<BadgeListResponse.BadgeItem> items = allBadges.stream()
                .map(badge -> new BadgeListResponse.BadgeItem(
                        badge.getId(),
                        badge.getName(),
                        earnedAtByBadgeId.containsKey(badge.getId()),
                        earnedAtByBadgeId.get(badge.getId())))
                .toList();

        return new BadgeListResponse(earnedAtByBadgeId.size(), allBadges.size(), items);
    }

    /** 해당 시군구에 뱃지가 있고 아직 획득하지 않았다면 최초 1회 지급한다. 없으면 아무 처리도 하지 않는다. */
    @Transactional
    public void awardIfFirstVisit(Long userId, String ldongSignguCode) {
        if (ldongSignguCode == null) {
            return;
        }

        sigunguCodeRepository.findFirstBySigunguCode(ldongSignguCode)
                .map(SigunguCode::getId)
                .flatMap(badgeRepository::findBySigunguId)
                .ifPresent(badge -> {
                    if (!userBadgeRepository.existsByUserIdAndBadgeId(userId, badge.getId())) {
                        userBadgeRepository.save(UserBadge.create(userId, badge.getId()));
                    }
                });
    }
}
