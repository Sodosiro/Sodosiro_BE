package com.sodosiro.global.aop;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** REST controller 요청의 사용자·처리 결과·지연 시간을 남긴다. */
@Slf4j
@Aspect
@Component
public class ApiRequestLoggingAspect {

    @Around("@within(org.springframework.web.bind.annotation.RestController)")
    public Object logRequest(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = currentRequest();
        long startedAt = System.nanoTime();
        String userId = currentUserId();
        String method = request == null ? "N/A" : request.getMethod();
        String uri = request == null ? "N/A" : request.getRequestURI();
        String handler = joinPoint.getSignature().getDeclaringTypeName()
                + "." + joinPoint.getSignature().getName();

        try {
            Object result = joinPoint.proceed();
            int status = result instanceof ResponseEntity<?> response
                    ? response.getStatusCode().value() : 200;
            log.info(
                    "API request completed: userId={}, method={}, uri={}, handler={}, status={}, durationMs={}",
                    userId, method, uri, handler, status, elapsedMillis(startedAt));
            return result;
        } catch (Throwable exception) {
            log.warn(
                    "API request failed: userId={}, method={}, uri={}, handler={}, error={}, durationMs={}",
                    userId, method, uri, handler, exception.getClass().getSimpleName(), elapsedMillis(startedAt));
            throw exception;
        }
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }

    private String currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return "anonymous";
        }
        if (authentication.getPrincipal() instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        return String.valueOf(authentication.getName());
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }
}
