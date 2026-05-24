package com.blog.common;

import java.lang.annotation.*;

/**
 * 操作日志注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditLog {

    /** 操作描述，支持 SpEL（如 #id） */
    String action() default "";

    /** 操作资源标识，支持 SpEL */
    String resource() default "";
}
