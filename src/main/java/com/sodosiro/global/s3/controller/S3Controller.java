package com.sodosiro.global.s3.controller;

import com.sodosiro.global.s3.constants.FileFolder;
import com.sodosiro.global.s3.service.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/s3")
public class S3Controller {

    private final S3Service s3Service;

    @PostMapping("/upload")
    public ResponseEntity<List<String>> s3Upload(@RequestPart(value = "image") List<MultipartFile> multipartFile) {
        List<String> upload = s3Service.uploadFiles(multipartFile, FileFolder.TEST);
        return ResponseEntity.ok(upload);
    }

    @DeleteMapping("/delete")
    public ResponseEntity<String> s3Delete(@RequestBody List<String> imageDeleteRequest) {
        s3Service.delete(imageDeleteRequest);
        return ResponseEntity.ok("이미지 삭제 성공");
    }

}