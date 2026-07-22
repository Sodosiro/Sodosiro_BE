package com.sodosiro.global.payload.code.error;


import com.sodosiro.global.payload.code.BaseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum S3ErrorCode implements BaseCode {

    // s3 관련 응답
    _NOT_EXIST_FILE(HttpStatus.BAD_REQUEST, "FILE400-NOT_FOUND", "존재하지 않는 파일입니다."),
    _NOT_EXIST_FILE_NAME(HttpStatus.BAD_REQUEST, "FILE400-INVALID_FILE_NAME", "존재하지 않는 파일명입니다."),
    _NOT_EXIST_FILE_EXTENSION(HttpStatus.BAD_REQUEST, "FILE400-EXT_MISSING", "확장자가 존재하지 않습니다."),
    _INVALID_FILE_EXTENSION(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "FILE415-EXT_UNSUPPORTED", "허용되지 않는 확장자입니다."),

    _INVALID_URL_FORMAT(HttpStatus.BAD_REQUEST, "FILE400-URL_INVALID", "잘못된 URL 형식입니다."),
    _EXTERNAL_URL_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "FILE400-EXTERNAL_URL", "우리 서비스에서 발급한 URL만 처리할 수 있습니다."),
    _EMPTY_FILE_PATH(HttpStatus.BAD_REQUEST, "FILE400-EMPTY_PATH", "URL에 파일 경로가 포함되어 있지 않습니다."),

    _FILE_SIZE_EXCEEDED(HttpStatus.PAYLOAD_TOO_LARGE, "FILE413-SIZE_EXCEEDED", "파일 크기는 2MB를 초과할 수 없습니다."),
    _IO_EXCEPTION_UPLOAD_FILE(HttpStatus.INTERNAL_SERVER_ERROR, "FILE500-UPLOAD_IO", "업로드 중 오류가 발생했습니다."),
    _IO_EXCEPTION_DELETE_FILE(HttpStatus.INTERNAL_SERVER_ERROR, "FILE500-DELETE_IO", "파일삭제에 실패했습니다");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

}
