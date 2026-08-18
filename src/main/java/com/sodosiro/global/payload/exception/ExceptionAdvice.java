package com.sodosiro.global.payload.exception;

import com.sodosiro.global.payload.code.ReasonDTO;
import com.sodosiro.global.payload.code.error.CommonErrorCode;
import com.sodosiro.global.payload.code.error.S3ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.http.InvalidParameterException;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@RestControllerAdvice(annotations = {RestController.class})
public class ExceptionAdvice extends ResponseEntityExceptionHandler {

    @ExceptionHandler(GeneralException.class)
    public ResponseEntity<ReasonDTO> onGeneralException(GeneralException e, HttpServletRequest request) {
        ReasonDTO reason = e.getErrorReasonHttpStatus();
        log.warn("[{}] {} - {}", request.getMethod(), request.getRequestURI(), reason.getCode());
        return ResponseEntity.status(reason.getHttpStatus()).body(reason);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ReasonDTO> handleConstraintViolation(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .findFirst()
                .map(v -> v.getMessage())
                .orElse(CommonErrorCode._BAD_REQUEST.getMessage());

        ReasonDTO body = ReasonDTO.builder()
                .httpStatus(HttpStatus.BAD_REQUEST)
                .code(CommonErrorCode._BAD_REQUEST.getCode())
                .message(message)
                .build();

        return ResponseEntity.badRequest().body(body);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException e, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fe -> {
                    if (fe.getCode() != null && fe.getCode().contains("typeMismatch")) {
                        return fe.getField() + " 필드의 형식이 올바르지 않습니다.";
                    }
                    return fe.getDefaultMessage();
                })
                .orElse(CommonErrorCode._BAD_REQUEST.getMessage());

        ReasonDTO body = ReasonDTO.builder()
                .httpStatus(HttpStatus.BAD_REQUEST)
                .code(CommonErrorCode._BAD_REQUEST.getCode())
                .message(errorMessage)
                .build();

        return super.handleExceptionInternal(e, body, headers, status, request);
    }


    /**
     * 잘못된 인코딩의 쿼리 파라미터(예: UTF-8 이 아닌 바이트)로 톰캣 파라미터 파싱이 실패한 경우.
     * 클라이언트(봇·크롤러 포함) 오류이므로 500 대신 400 으로 응답하고 WARN 으로만 남긴다.
     */
    @ExceptionHandler(InvalidParameterException.class)
    public ResponseEntity<ReasonDTO> onInvalidParameter(InvalidParameterException e, HttpServletRequest request) {
        log.warn("[{}] {} - 잘못된 요청 파라미터 인코딩", request.getMethod(), request.getRequestURI());
        return ResponseEntity.badRequest().body(CommonErrorCode._BAD_REQUEST.getReasonHttpStatus());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ReasonDTO> onDataIntegrityViolation(DataIntegrityViolationException e) {
        log.warn("Data Integrity Violation: {}", e.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(CommonErrorCode._CONFLICT.getReasonHttpStatus());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ReasonDTO> handleException(Exception e) {
        log.error("Unhandled exception", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(CommonErrorCode._INTERNAL_SERVER_ERROR.getReasonHttpStatus());
    }

}
