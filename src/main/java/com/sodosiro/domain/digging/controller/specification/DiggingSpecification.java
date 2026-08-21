package com.sodosiro.domain.digging.controller.specification;

import com.sodosiro.domain.digging.controller.dto.request.DiggingCreateRequest;
import com.sodosiro.domain.digging.controller.dto.request.DiggingUpdateRequest;
import com.sodosiro.domain.digging.controller.dto.response.DiggingBookmarkResponse;
import com.sodosiro.domain.digging.controller.dto.response.DiggingCandidateResponse;
import com.sodosiro.domain.digging.controller.dto.response.DiggingLikeResponse;
import com.sodosiro.domain.digging.controller.dto.response.DiggingListResponse;
import com.sodosiro.domain.digging.controller.dto.response.DiggingResponse;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

public interface DiggingSpecification {

    @Operation(summary = "디깅 후보 여행지 조회",
            description = "완료된(FINISHED) 코스에서 GPS 인증을 마친 여행지 목록을 반환합니다. "
                    + "이미 디깅을 작성한 여행지는 alreadyPosted=true 로 표시됩니다. "
                    + "본인 코스가 아니면 404, 코스가 완료 상태가 아니면 409를 반환합니다.")
    ResponseEntity<DiggingCandidateResponse> getCandidates(Long userId, Long courseId);

    @Operation(summary = "디깅 작성",
            description = "완료된 코스에서 GPS 인증된 여행지 1곳에 대해 사진(최대 5장)과 감성 한마디(최대 300자)를 남깁니다. "
                    + "같은 코스의 같은 여행지에는 1건만 작성할 수 있습니다. multipart/form-data 로 request(JSON)와 images 를 함께 전송합니다.")
    ResponseEntity<DiggingResponse> create(Long userId, DiggingCreateRequest request, List<MultipartFile> images);

    @Operation(summary = "전체 디깅 피드 조회",
            description = "모든 사용자가 작성한 디깅을 최신순으로 조회합니다. cursor 는 직전 응답의 nextCursor 를 그대로 전달합니다.")
    ResponseEntity<DiggingListResponse> getFeed(Long loginUserId, Long cursor, int size);

    @Operation(summary = "디깅 단건 조회")
    ResponseEntity<DiggingResponse> getOne(Long loginUserId, Long diggingId);

    @Operation(summary = "내가 쓴 디깅 목록 조회", description = "최신순 커서 기반 페이지네이션입니다.")
    ResponseEntity<DiggingListResponse> getMine(Long userId, Long cursor, int size);

    @Operation(summary = "여행지별 디깅 목록 조회", description = "특정 관광지(contentId)에 작성된 디깅을 최신순으로 조회합니다.")
    ResponseEntity<DiggingListResponse> getBySpot(Long loginUserId, Long contentId, Long cursor, int size);

    @Operation(summary = "내 즐겨찾기 디깅 목록 조회")
    ResponseEntity<DiggingListResponse> getMyBookmarks(Long userId, Long cursor, int size);

    @Operation(summary = "디깅 수정",
            description = "본인 디깅의 감성 한마디와 이미지를 수정합니다. keepImageUrls 에 없는 기존 이미지는 삭제되고, "
                    + "새 images 가 뒤에 추가됩니다. 유지분 + 신규가 5장을 넘으면 400을 반환합니다.")
    ResponseEntity<DiggingResponse> update(
            Long userId, Long diggingId, DiggingUpdateRequest request, List<MultipartFile> images);

    @Operation(summary = "디깅 삭제", description = "본인 디깅을 소프트 삭제합니다. 첨부 이미지는 커밋 후 비동기로 S3에서 정리됩니다.")
    ResponseEntity<Void> delete(Long userId, Long diggingId);

    @Operation(summary = "디깅 좋아요 토글",
            description = "호출할 때마다 좋아요 상태가 뒤집힙니다(누르면 등록, 다시 누르면 취소). "
                    + "응답의 liked 로 현재 상태를, likeCount 로 갱신된 좋아요 수를 확인합니다. "
                    + "타인의 디깅에 좋아요가 등록되는 순간에만 작성자에게 푸시 알림이 발송되며, 취소 시에는 발송되지 않습니다.")
    ResponseEntity<DiggingLikeResponse> toggleLike(Long userId, Long diggingId);

    @Operation(summary = "디깅 즐겨찾기 토글",
            description = "호출할 때마다 즐겨찾기 상태가 뒤집힙니다. 알림은 발송되지 않습니다.")
    ResponseEntity<DiggingBookmarkResponse> toggleBookmark(Long userId, Long diggingId);
}
