package com.sodosiro.domain.user.controller;

import com.sodosiro.domain.dataextract.service.InternalEtlTokenVerifier;
import com.sodosiro.domain.user.controller.dto.request.UserPurgeRequest;
import com.sodosiro.domain.user.controller.dto.response.UserPurgeResponse;
import com.sodosiro.domain.user.service.UserPurgeService;
import com.sodosiro.domain.user.service.dto.UserPurgeResult;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ETL(Airflow) 전용 탈퇴 회원 완전 삭제 트리거. 유예기간이 지난 회원만 대상으로 하며 하루 한 번 호출된다.
 */
@Hidden
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/etl/users")
public class UserPurgeEtlController {

    private final UserPurgeService userPurgeService;
    private final InternalEtlTokenVerifier internalEtlTokenVerifier;

    @PostMapping("/purge-withdrawn")
    public ResponseEntity<UserPurgeResponse> purgeWithdrawn(
            @RequestHeader(value = "X-Internal-ETL-Token", required = false) String internalEtlToken,
            @RequestBody(required = false) UserPurgeRequest request) {
        internalEtlTokenVerifier.verify(internalEtlToken);
        UserPurgeResult result = userPurgeService.purgeExpiredWithdrawals();
        return ResponseEntity.ok(UserPurgeResponse.from(result));
    }
}
