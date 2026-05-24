package com.blog.common;

import com.blog.config.JwtUtil;
import com.blog.entity.SysLog;
import com.blog.mapper.SysLogMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

/**
 * 操作日志切面
 */
@Aspect
@Component
public class AuditLogAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditLogAspect.class);

    @Autowired
    private SysLogMapper sysLogMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Around("@annotation(auditLog)")
    public Object around(ProceedingJoinPoint pjp, AuditLog auditLog) throws Throwable {
        Object result = pjp.proceed();

        try {
            HttpServletRequest req = ((ServletRequestAttributes) RequestContextHolder
                    .getRequestAttributes()).getRequest();

            Long userId = null;
            String username = null;
            String auth = req.getHeader("Authorization");
            if (auth != null && auth.startsWith("Bearer ")) {
                String token = auth.substring(7);
                userId = jwtUtil.getUserId(token);
                username = jwtUtil.getUsername(token);
            }

            SysLog sysLog = new SysLog();
            sysLog.setUserId(userId);
            sysLog.setUsername(username);
            sysLog.setAction(auditLog.action());
            sysLog.setResource(auditLog.resource());
            sysLog.setIp(req.getRemoteAddr());
            sysLog.setCreatedAt(LocalDateTime.now());
            sysLogMapper.insert(sysLog);
        } catch (Exception e) {
            log.warn("操作日志记录失败", e);
        }

        return result;
    }
}
