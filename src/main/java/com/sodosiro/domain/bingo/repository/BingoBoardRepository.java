package com.sodosiro.domain.bingo.repository;

import com.sodosiro.domain.bingo.entity.BingoBoard;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BingoBoardRepository extends JpaRepository<BingoBoard, Long> {

    Optional<BingoBoard> findBySeasonIdAndSigunguId(Long seasonId, Long sigunguId);
}
