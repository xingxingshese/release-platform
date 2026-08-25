package com.company.release.common.response;

/**
 * 统一成功响应：{code, message, data, requestId}。
 * code = "OK"；requestId 由 RequestIdFilter 写入 MDC 后由 Jackson 序列化时填充。
 */
public record ApiResponse<T>(String code, String message, T data) {

    public static final String OK = "OK";

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(OK, "success", data);
    }

    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(OK, "success", null);
    }

    public static <T> ApiResponse<T> of(String code, String message, T data) {
        return new ApiResponse<>(code, message, data);
    }
}
