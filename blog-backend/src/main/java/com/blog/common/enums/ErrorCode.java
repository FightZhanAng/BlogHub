package com.blog.common.enums;

/**
 * 全局错误码枚举
 */
public enum ErrorCode {

    // ====== 404 资源不存在 ======
    POST_NOT_FOUND(40401, "文章不存在"),
    COMMENT_NOT_FOUND(40402, "评论不存在"),
    USER_NOT_FOUND(40403, "用户不存在"),

    // ====== 403 无权操作 ======
    FORBIDDEN(40300, "无权操作"),
    LOGIN_REQUIRED(40301, "请先登录"),

    // ====== 400 参数/业务错误 ======
    BAD_REQUEST(40000, "请求参数错误"),
    USERNAME_EXISTS(40001, "用户名已存在"),
    INVALID_PASSWORD(40002, "原密码错误"),
    VALIDATION_ERROR(40003, "参数校验失败"),
    SLUG_EXISTS(40004, "URL 标识已存在"),

    // ====== 429 频率限制 ======
    RATE_LIMIT(42900, "请求过于频繁，请稍后再试"),
    ;

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() { return code; }
    public String getMessage() { return message; }
}
