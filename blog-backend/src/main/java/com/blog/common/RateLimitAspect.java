package com.blog.common;

import com.blog.exception.BusinessException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 限流切面 — 本地计数器实现
 */
@Aspect
@Component
public class RateLimitAspect {

    private static final Logger log = LoggerFactory.getLogger(RateLimitAspect.class);

    private final Map<String, RateLimitBucket> buckets = new ConcurrentHashMap<>();

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint pjp, RateLimit rateLimit) throws Throwable {
        String key = rateLimit.key();
        int max = rateLimit.max();
        long periodMs = rateLimit.period() * 1000L;

        RateLimitBucket bucket = buckets.computeIfAbsent(key, k -> new RateLimitBucket());
        synchronized (bucket) {
            bucket.cleanup();
            if (bucket.count.get() >= max) {
                long wait = bucket.windowStart + periodMs - System.currentTimeMillis();
                log.warn("接口限流触发: key={}, count={}, max={}, wait={}ms",
                        key, bucket.count.get(), max, wait);
                throw new BusinessException(429, "请求过于频繁，请稍后再试");
            }
            bucket.count.incrementAndGet();
        }
        return pjp.proceed();
    }

    static class RateLimitBucket {
        final AtomicInteger count = new AtomicInteger(0);
        long windowStart = System.currentTimeMillis();

        void cleanup() {
            long now = System.currentTimeMillis();
            if (now - windowStart > 0) {
                windowStart = now;
                count.set(0);
            }
        }
    }
}
