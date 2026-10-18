package com.blog.common;

import com.blog.exception.BusinessException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 限流切面 — 本地计数器实现。
 *
 * <p>之前的实现有两个致命问题：① {@code cleanup()} 的判断是 {@code now - windowStart > 0}，
 * 每次调用都为真，于是每次都把计数清零，限流永远不生效；② key 只用注解里的固定值，
 * 不区分来源，一旦真生效就会把所有用户算进同一个桶（一个人刷爆，全站登录被拒）。</p>
 *
 * <p>现在：窗口未过期时保留计数，过期才重置；key 拼接客户端 IP，做到按来源限流。</p>
 */
@Aspect
@Component
public class RateLimitAspect {

    private static final Logger log = LoggerFactory.getLogger(RateLimitAspect.class);

    private final Map<String, RateLimitBucket> buckets = new ConcurrentHashMap<>();

    private final HttpServletRequest request;

    public RateLimitAspect(HttpServletRequest request) {
        this.request = request;
    }

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint pjp, RateLimit rateLimit) throws Throwable {
        // key = 接口标识 + 客户端 IP，避免不同用户互相影响
        String key = rateLimit.key() + ":" + VisitorIdUtil.getClientIp(request);
        int max = rateLimit.max();
        long periodMs = rateLimit.period() * 1000L;

        // 简单防膨胀：桶数量超过阈值时清掉已过窗口的
        if (buckets.size() > 10000) {
            buckets.entrySet().removeIf(e -> e.getValue().isExpired(periodMs));
        }

        RateLimitBucket bucket = buckets.computeIfAbsent(key, k -> new RateLimitBucket());
        synchronized (bucket) {
            bucket.resetIfExpired(periodMs);
            if (bucket.count.get() >= max) {
                long wait = bucket.windowStart + periodMs - System.currentTimeMillis();
                log.warn("接口限流触发: key={}, count={}, max={}, wait={}ms",
                        key, bucket.count.get(), max, Math.max(wait, 0));
                throw new BusinessException(429, "请求过于频繁，请稍后再试");
            }
            bucket.count.incrementAndGet();
        }
        return pjp.proceed();
    }

    static class RateLimitBucket {
        final AtomicInteger count = new AtomicInteger(0);
        long windowStart = System.currentTimeMillis();

        /** 只有窗口真正过期才重置计数 */
        void resetIfExpired(long periodMs) {
            long now = System.currentTimeMillis();
            if (now - windowStart >= periodMs) {
                windowStart = now;
                count.set(0);
            }
        }

        boolean isExpired(long periodMs) {
            return System.currentTimeMillis() - windowStart >= periodMs;
        }
    }
}
