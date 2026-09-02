package com.sodosiro.domain.badge.repository;

import com.sodosiro.domain.badge.entity.Badge;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BadgeRepository extends JpaRepository<Badge, Long> {

    Optional<Badge> findBySigunguId(Long sigunguId);
}
