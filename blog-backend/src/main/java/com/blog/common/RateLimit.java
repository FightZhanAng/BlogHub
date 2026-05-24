package com.blog.common;

import java.lang.annotation.*;

/**
 * 接口限流注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /** 限流 key（用于区分不同接口） */
    String key() default "default";

    /** 时间窗口内最大请求次数 */
    int max() default 5;

    /** 时间窗口（秒） */
    int period() default 60;
}
