package com.company.release.common.exception;

import com.company.release.release.domain.state.IllegalStateTransitionException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * 全局异常处理（规范 §28）：统一错误模型，绝不返回 Java 堆栈。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    public record ErrorBody(String code, String message, String requestId, List<String> details) {
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorBody> handleBusiness(BusinessException ex, HttpServletRequest request) {
        return build(ex.getErrorCode(), ex.getMessage(), request, null);
    }

    @ExceptionHandler(IllegalStateTransitionException.class)
    public ResponseEntity<ErrorBody> handleIllegalTransition(IllegalStateTransitionException ex,
                                                             HttpServletRequest request) {
        return build(ErrorCode.CONFLICT, "Illegal release state transition",
                request, List.of(ex.getMessage()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ErrorBody> handleValidation(Exception ex, HttpServletRequest request) {
        String msg = ex instanceof MethodArgumentNotValidException manv
                ? manv.getBindingResult().getAllErrors().stream()
                        .findFirst()
                        .map(e -> e.getDefaultMessage())
                        .orElse("validation failed")
                : "malformed request body";
        return build(ErrorCode.VALIDATION_ERROR, msg, request, null);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorBody> handleIllegalArgument(IllegalArgumentException ex,
                                                           HttpServletRequest request) {
        return build(ErrorCode.VALIDATION_ERROR, ex.getMessage(), request, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorBody> handleUnknown(Exception ex, HttpServletRequest request) {
        log.error("unhandled exception", ex);
        // 不向前端泄露内部异常细节与堆栈
        return build(ErrorCode.SYSTEM_ERROR, "internal system error", request, null);
    }

    private ResponseEntity<ErrorBody> build(ErrorCode code, String message,
                                            HttpServletRequest request, List<String> details) {
        String requestId = MDC.get("requestId");
        if (requestId == null || requestId.isBlank()) {
            requestId = (String) request.getAttribute("requestId");
        }
        var body = new ErrorBody(code.name(), message, requestId, details);
        return ResponseEntity.status(code.httpStatus()).body(body);
    }
}
