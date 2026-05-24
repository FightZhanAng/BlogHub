package com.blog.exception;

/**
 * 资源不存在异常
 */
public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String resource, String field, Object value) {
        super(404, resource + " 不存在: " + field + "=" + value);
    }

    public ResourceNotFoundException(String message) {
        super(404, message);
    }
}
