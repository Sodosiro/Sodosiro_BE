package com.sodosiro.domain.user.service;

import com.sodosiro.domain.course.service.ActiveCourseCacheWriter;
import com.sodosiro.domain.like.service.dto.LikedSpotsGeoCache;
import com.sodosiro.domain.user.entity.User;
import com.sodosiro.domain.user.repository.UserRepository;
import com.sodosiro.domain.user.service.dto.PurgedUserFootprint;
import com.sodosiro.domain.user.service.dto.UserPurgeResult;
import com.sodosiro.global.s3.service.S3Service;
import com.sodosiro.global.service.RedisService;
import com.sodosiro.global.utils.TimeZones;
import com.sodosiro.global.utils.TokenKeys;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserPurgeService {

    private final UserRepository userRepository;
    private final UserPurgeExecutor userPurgeExecutor;
    private final ActiveCourseCacheWriter activeCourseCacheWriter;
    private final S3Service s3Service;
    private final RedisService redisService;

    /** 탈퇴 접수 후 데이터를 실제로 지우기까지 남겨두는 기간(일). */
    @Value("${user.withdrawal.retention-days:7}")
    private int retentionDays;

    /** 한 번의 배치에서 처리할 최대 인원. 남은 인원은 다음 실행이 이어서 처리한다. */
    @Value("${user.withdrawal.purge-batch-size:200}")
    private int purgeBatchSize;

    public UserPurgeResult purgeExpiredWithdrawals() {
        LocalDateTime threshold = LocalDateTime.now(TimeZones.KST).minusDays(retentionDays);
        List<User> targets = userRepository.findByWithdrawnAtLessThanEqualOrderByWithdrawnAtAsc(
                threshold, Limit.of(purgeBatchSize));

        int purged = 0;
        int failed = 0;
        int reviews = 0;
        int diggings = 0;
        int spotLikes = 0;
        int courses = 0;

        for (User target : targets) {
            Long userId = target.getUserId();
            try {
                PurgedUserFootprint footprint = userPurgeExecutor.purge(userId);
                purged++;
                reviews += footprint.deletedReviews();
                diggings += footprint.deletedDiggings();
                spotLikes += footprint.deletedSpotLikes();
                courses += footprint.deletedCourses();
                cleanUpExternalResources(footprint);
            } catch (Exception exception) {
                failed++;
                log.error("탈퇴 회원 데이터 삭제 실패, 다음 배치에서 재시도합니다. userId={}", userId, exception);
            }
        }

        UserPurgeResult result = new UserPurgeResult(
                targets.size(), purged, failed, reviews, diggings, spotLikes, courses);
        log.info("탈퇴 회원 데이터 삭제 배치 완료: retentionDays={}, {}", retentionDays, result);
        return result;
    }

    /**
     * DB 커밋이 끝난 뒤 S3 이미지와 Redis 잔여 키를 정리한다. 여기서 실패해도 DB 삭제는 유지하고 경고만 남긴다
     * (사용자 데이터는 이미 사라졌고, 남은 것은 참조되지 않는 고아 객체뿐이다).
     */
    private void cleanUpExternalResources(PurgedUserFootprint footprint) {
        try {
            if (!footprint.imageUrls().isEmpty()) {
                s3Service.delete(footprint.imageUrls());
            }
        } catch (Exception exception) {
            log.warn("탈퇴 회원 이미지 S3 삭제 실패, 고아 파일로 남음. userId={}, count={}",
                    footprint.userId(), footprint.imageUrls().size(), exception);
        }

        try {
            redisService.deleteKey(TokenKeys.withdrawnKey(footprint.userId()));
            redisService.deleteKey(LikedSpotsGeoCache.redisKey(footprint.userId()));
            activeCourseCacheWriter.evict(footprint.userId());
        } catch (Exception exception) {
            log.warn("탈퇴 회원 Redis 키 정리 실패. userId={}", footprint.userId(), exception);
        }
    }
}
