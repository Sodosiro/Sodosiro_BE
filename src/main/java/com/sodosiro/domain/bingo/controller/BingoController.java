package com.sodosiro.domain.bingo.controller;

import com.sodosiro.domain.bingo.constants.SeasonType;
import com.sodosiro.domain.bingo.controller.dto.BingoBoardResponse;
import com.sodosiro.domain.bingo.controller.dto.BingoGpsRequest;
import com.sodosiro.domain.bingo.controller.dto.BingoGpsVerifyResponse;
import com.sodosiro.domain.bingo.controller.dto.BingoSeasonResponse;
import com.sodosiro.domain.bingo.controller.specification.BingoSpecification;
import com.sodosiro.domain.bingo.service.BingoGpsService;
import com.sodosiro.domain.bingo.service.BingoQueryService;
import com.sodosiro.global.resolver.LoginUser;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/bingo")
public class BingoController implements BingoSpecification {

    private final BingoQueryService bingoQueryService;
    private final BingoGpsService bingoGpsService;

    @GetMapping("/seasons")
    @Override
    public ResponseEntity<List<BingoSeasonResponse>> listSeasons() {
        return ResponseEntity.ok(bingoQueryService.listSeasons());
    }

    @GetMapping("/regions/{sigunguId}")
    @Override
    public ResponseEntity<BingoBoardResponse> getBoard(
            @LoginUser Long userId,
            @PathVariable Long sigunguId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) SeasonType seasonType) {
        return ResponseEntity.ok(bingoQueryService.getBoardStatus(userId, sigunguId, year, seasonType));
    }

    @PostMapping("/gps")
    @Override
    public ResponseEntity<BingoGpsVerifyResponse> verifyGps(@LoginUser Long userId, @RequestBody @Valid BingoGpsRequest request) {
        return ResponseEntity.ok(bingoGpsService.verify(userId, request));
    }
}
