package com.teachhelper.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 统一API响应格式
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    
    /**
     * 响应状态码
     */
    private int code;
    
    /**
     * 响应消息
     */
    private String message;
    
    /**
     * 响应数据
     */
    private T data;
    
    /**
     * 是否成功
     */
    private boolean success;
    
    public ApiResponse() {}
    
    public ApiResponse(int code, String message, T data, boolean success) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.success = success;
    }
    
    /**
     * 成功响应
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "操作成功", data, true);
    }
    
    /**
     * 成功响应带消息
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(200, message, data, true);
    }
    
    /**
     * 失败响应
     */
    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null, false);
    }
    
    /**
     * 失败响应（默认400错误码）
     */
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(400, message, null, false);
    }
    
    /**
     * 验证失败响应（422错误码）
     */
    public static <T> ApiResponse<T> validationError(String message) {
        return new ApiResponse<>(422, message, null, false);
    }
    
    // Getters and Setters
    public int getCode() {
        return code;
    }
    
    public void setCode(int code) {
        this.code = code;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public T getData() {
        return data;
    }
    
    public void setData(T data) {
        this.data = data;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
}
