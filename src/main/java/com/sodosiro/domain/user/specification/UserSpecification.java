package com.sodosiro.domain.user.specification;

import com.sodosiro.domain.user.dto.request.ProfileRequest;
import com.sodosiro.domain.user.dto.response.ProfileResponse;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

public interface UserSpecification {

    @Operation(summary = "프로필 조회", description = "로그인한 유저의 프로필 정보(닉네임, 이미지, 한줄소개)를 조회합니다.")
    ResponseEntity<ProfileResponse> getProfile(Long userId);

    @Operation(summary = "프로필 수정", description = "닉네임, 한줄소개 및 프로필 이미지(선택)를 수정합니다. image 파트 없이 removeImage=true를 보내면 기존 프로필 이미지를 제거합니다. (multipart/form-data)")
    ResponseEntity<ProfileResponse> updateProfile(
            Long userId,
            ProfileRequest request,
            MultipartFile image
    );


}
