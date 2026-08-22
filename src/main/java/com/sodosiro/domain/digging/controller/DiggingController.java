package com.sodosiro.domain.digging.controller;

import com.sodosiro.domain.digging.controller.dto.request.DiggingCreateRequest;
import com.sodosiro.domain.digging.controller.dto.request.DiggingUpdateRequest;
import com.sodosiro.domain.digging.controller.dto.response.DiggingBookmarkResponse;
import com.sodosiro.domain.digging.controller.dto.response.DiggingCandidateResponse;
import com.sodosiro.domain.digging.controller.dto.response.DiggingLikeResponse;
import com.sodosiro.domain.digging.controller.dto.response.DiggingListResponse;
import com.sodosiro.domain.digging.controller.dto.response.DiggingResponse;
import com.sodosiro.domain.digging.controller.specification.DiggingSpecification;
import com.sodosiro.domain.digging.service.DiggingBookmarkService;
import com.sodosiro.domain.digging.service.DiggingLikeService;
import com.sodosiro.domain.digging.service.DiggingService;
import com.sodosiro.global.resolver.LoginUser;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class DiggingController implements DiggingSpecification {

    private final DiggingService diggingService;
    private final DiggingLikeService diggingLikeService;
    private final DiggingBookmarkService diggingBookmarkService;

    @GetMapping("/courses/{courseId}/digging-candidates")
    public ResponseEntity<DiggingCandidateResponse> getCandidates(
            @LoginUser Long userId,
            @PathVariable Long courseId) {
        return ResponseEntity.ok(diggingService.getCandidates(userId, courseId));
    }

    @PostMapping(value = "/diggings", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DiggingResponse> create(
            @LoginUser Long userId,
            @RequestPart @Valid DiggingCreateRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {
        return ResponseEntity.status(HttpStatus.CREATED).body(diggingService.create(userId, request, images));
    }

    @GetMapping("/diggings")
    public ResponseEntity<DiggingListResponse> getFeed(
            @LoginUser Long loginUserId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(diggingService.getFeed(loginUserId, cursor, size));
    }

    @GetMapping("/diggings/me")
    public ResponseEntity<DiggingListResponse> getMine(
            @LoginUser Long userId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(diggingService.getMine(userId, cursor, size));
    }

    @GetMapping("/diggings/bookmarks")
    public ResponseEntity<DiggingListResponse> getMyBookmarks(
            @LoginUser Long userId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(diggingService.getMyBookmarks(userId, cursor, size));
    }

    @GetMapping("/spots/{contentId}/diggings")
    public ResponseEntity<DiggingListResponse> getBySpot(
            @LoginUser Long loginUserId,
            @PathVariable Long contentId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(diggingService.getBySpot(loginUserId, contentId, cursor, size));
    }

    @GetMapping("/diggings/{diggingId}")
    public ResponseEntity<DiggingResponse> getOne(
            @LoginUser Long loginUserId,
            @PathVariable Long diggingId) {
        return ResponseEntity.ok(diggingService.getOne(loginUserId, diggingId));
    }

    @PatchMapping(value = "/diggings/{diggingId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DiggingResponse> update(
            @LoginUser Long userId,
            @PathVariable Long diggingId,
            @RequestPart @Valid DiggingUpdateRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {
        return ResponseEntity.ok(diggingService.update(userId, diggingId, request, images));
    }

    @DeleteMapping("/diggings/{diggingId}")
    public ResponseEntity<Void> delete(@LoginUser Long userId, @PathVariable Long diggingId) {
        diggingService.delete(userId, diggingId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/diggings/{diggingId}/like")
    public ResponseEntity<DiggingLikeResponse> toggleLike(@LoginUser Long userId, @PathVariable Long diggingId) {
        return ResponseEntity.ok(diggingLikeService.toggle(userId, diggingId));
    }

    @PostMapping("/diggings/{diggingId}/bookmark")
    public ResponseEntity<DiggingBookmarkResponse> toggleBookmark(
            @LoginUser Long userId, @PathVariable Long diggingId) {
        return ResponseEntity.ok(diggingBookmarkService.toggle(userId, diggingId));
    }
}
