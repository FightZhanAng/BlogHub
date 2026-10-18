package com.blog.common;

import java.lang.annotation.*;

/**
 * 标记接口需要管理员角色。可标注在方法或整个 Controller 上。
 * 由 AuthInterceptor 统一校验，避免各 Controller 手写角色判断时漏掉。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireAdmin {
}
