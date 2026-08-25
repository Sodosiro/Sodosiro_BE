package com.sodosiro.domain.bingo.repository;

import com.sodosiro.domain.bingo.constants.BingoSeasonStatus;
import com.sodosiro.domain.bingo.constants.SeasonType;
import com.sodosiro.domain.bingo.entity.BingoSeason;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BingoSeasonRepository extends JpaRepository<BingoSeason, Long> {

    boolean existsByYearAndSeasonType(Integer year, SeasonType seasonType);

    Optional<BingoSeason> findByYearAndSeasonType(Integer year, SeasonType seasonType);

    List<BingoSeason> findByStatus(BingoSeasonStatus status);

    Optional<BingoSeason> findFirstByStatusOrderByIdDesc(BingoSeasonStatus status);

    List<BingoSeason> findAllByOrderByStartDateDesc();
}
