package com.sodosiro.global.s3.service;

import com.sodosiro.global.payload.code.error.S3ErrorCode;
import com.sodosiro.global.payload.exception.GeneralException;
import com.sodosiro.global.s3.constants.FileFolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.InputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class S3Service {
    private final S3Client s3Client;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucketName;

    @Value("${spring.cloud.aws.cloudfront.domain:}")
    private String cloudFrontDomain;

    @Value("${image.upload.max-size:2097152}")
    private long maxFileSize;

    private static final List<String> ALLOWED_EXTENSIONS = List.of("jpg", "jpeg", "png", "webp");
    private static final String CACHE_CONTROL_YEAR = "public, max-age=31536000";

    public String uploadFile(MultipartFile file, FileFolder folder) {
        validateFile(file);
        return uploadToS3(file, folder.getPath());
    }

    public List<String> uploadFiles(List<MultipartFile> files, FileFolder folder) {

        if (files == null || files.isEmpty()) {
            return Collections.emptyList();
        }

        return files.stream()
                .map(file -> uploadFile(file, folder))
                .toList();
    }

    private void validateFile(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new GeneralException(S3ErrorCode._NOT_EXIST_FILE);
        }

        String filename = file.getOriginalFilename();
        if (filename == null || filename.isEmpty()) {
            throw new GeneralException(S3ErrorCode._NOT_EXIST_FILE_Name);
        }
        String extension = extractExtension(filename);

        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new GeneralException(S3ErrorCode._INVALID_FILE_EXTENSION);
        }

        if (file.getSize() > maxFileSize) {
            throw new GeneralException(S3ErrorCode._FILE_SIZE_EXCEEDED);
        }
    }

    private String extractExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf(".");
        if (lastDotIndex == -1) {
            throw new GeneralException(S3ErrorCode._NOT_EXIST_FILE_EXTENSION);
        }
        return filename.substring(lastDotIndex + 1);
    }

    private String uploadToS3(MultipartFile file, String folder) {

        String s3Key = generateS3Key(file, folder);

        try (InputStream inputStream = file.getInputStream()) {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .cacheControl(CACHE_CONTROL_YEAR)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(inputStream, file.getSize()));
            return buildImageUrl(s3Key);
        } catch (Exception exception) {
            log.error(exception.getMessage(), exception);
            throw new GeneralException(S3ErrorCode._IO_EXCEPTION_UPLOAD_FILE);
        }
    }


    private String generateS3Key(MultipartFile file, String folder) {
        String filename = file.getOriginalFilename();
        String extension = extractExtension(filename);
        String uuid = UUID.randomUUID().toString().substring(0, 10);

        return String.format("%s/%s.%s", folder, uuid, extension);
    }

    public void delete(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) return;
        delete(Collections.singletonList(imageUrl));
    }

    public void delete(List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) return;

        List<ObjectIdentifier> identifiers = imageUrls.stream()
                .map(url -> {
                    if (url == null || url.isBlank()) {
                        throw new GeneralException(S3ErrorCode._INVALID_URL_FORMAT);
                    }
                    String key = getKeyFromImageUrl(url);
                    return ObjectIdentifier.builder().key(key).build();
                })
                .toList();

        try {
            DeleteObjectsRequest deleteRequest = DeleteObjectsRequest.builder()
                    .bucket(bucketName)
                    .delete(d -> d.objects(identifiers).quiet(true))
                    .build();
            s3Client.deleteObjects(deleteRequest);
        } catch (Exception e) {
            log.error("S3 파일 삭제 중 오류 발생, 대상: {}", identifiers, e);
            throw new GeneralException(S3ErrorCode._IO_EXCEPTION_DELETE_FILE);
        }
    }

    private String getKeyFromImageUrl(String imageUrl) {
        try {
            if (!imageUrl.contains(cloudFrontDomain) && !imageUrl.contains(bucketName)) {
                throw new GeneralException(S3ErrorCode._EXTERNAL_URL_NOT_ALLOWED);
            }
            URI uri = new URI(imageUrl);
            String path = uri.getPath();

            if (path == null || path.isBlank() || path.equals("/")) {
                throw new GeneralException(S3ErrorCode._EMPTY_FILE_PATH);
            }

            String decodedPath = URLDecoder.decode(path, StandardCharsets.UTF_8);

            return decodedPath.startsWith("/") ? decodedPath.substring(1) : decodedPath;
        } catch (URISyntaxException e) {
            throw new GeneralException(S3ErrorCode._INVALID_URL_FORMAT,e);
        } catch (Exception e) {
            throw new GeneralException(S3ErrorCode._IO_EXCEPTION_DELETE_FILE,e);
        }
    }

    private String buildImageUrl(String s3Key) {
        if (cloudFrontDomain != null && !cloudFrontDomain.isBlank()) {
            return String.format("https://%s/%s", cloudFrontDomain, s3Key);
        }
        return s3Client.utilities().getUrl(url -> url.bucket(bucketName).key(s3Key)).toString();
    }


    public String uploadFromExternalUrl(String externalUrl, FileFolder folder){
        if (externalUrl == null || externalUrl.isBlank()) {
            return null;
        }

        if (isInternalUrl(externalUrl))  {
            return externalUrl;
        }

        try {
            URL url = new URI(externalUrl).toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(10000);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            connection.setInstanceFollowRedirects(true);

            int responseCode = connection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
            responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                    responseCode == 307 || responseCode == 300
            ) {
                String redirectUrl = connection.getHeaderField("Location");

                if (redirectUrl != null && !redirectUrl.isBlank()) {
                    return uploadFromExternalUrl(redirectUrl, folder);
                }
            }

            if (responseCode != 200) {
                return externalUrl;
            }

            String contentType = connection.getContentType();
            String extension = getExtensionFromContentType(contentType);
            if (extension == null) {
                extension = getExtensionFromUrl(externalUrl);
            }

            String s3Key = String.format("%s/%s.%s", folder, UUID.randomUUID().toString().substring(0, 10), extension);
            try (InputStream inputStream = connection.getInputStream()){
                byte[] imageBytes = inputStream.readAllBytes();

                PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(s3Key)
                        .contentType(contentType != null ? contentType :  "image/" + extension)
                        .contentLength((long) imageBytes.length)
                        .cacheControl("public, max-age=31536000")
                        .build();

                s3Client.putObject(putObjectRequest, RequestBody.fromBytes(imageBytes));

                return buildImageUrl(s3Key);

            }
        } catch (Exception e) {
            log.error("외부 이미지 업로드 실패 - URL: {}, 에러: {}", externalUrl, e.getMessage());
            return externalUrl;
        }
    }

    private String getExtensionFromUrl(String url) {
        try {
            String path = new URI(url).getPath();
            int lastDot = path.lastIndexOf('.');
            if (lastDot > 0) {
                return path.substring(lastDot + 1).toLowerCase();
            }
        } catch (Exception ignored) {}
        return "jpg";
    }

    private String getExtensionFromContentType(String contentType) {
        if (contentType == null) return null;
        return switch (contentType.toLowerCase()) {
            case "image/jpeg", "image/jpg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            case "image/gif" -> "gif";
            default -> null;
        };
    }

    public boolean isInternalUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        return url.contains("s3.ap-northeast-2.amazonaws.com") ||
                url.contains("cloudfront.net") ||
                url.contains(bucketName);
    }

}