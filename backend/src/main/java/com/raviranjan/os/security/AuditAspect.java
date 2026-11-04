package com.raviranjan.os.security;

import com.raviranjan.os.domain.entity.AuditLog;
import com.raviranjan.os.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;

@Aspect
@Component
public class AuditAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);
    private final AuditLogRepository auditLogRepository;

    public AuditAspect(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Pointcut("execution(* com.raviranjan.os.controller..*(..)) && " +
              "(@annotation(org.springframework.web.bind.annotation.PostMapping) || " +
              "@annotation(org.springframework.web.bind.annotation.PutMapping) || " +
              "@annotation(org.springframework.web.bind.annotation.DeleteMapping) || " +
              "@annotation(org.springframework.web.bind.annotation.PatchMapping))")
    public void mutatingEndpoints() {}

    @AfterReturning(pointcut = "mutatingEndpoints()", returning = "result")
    public void auditMutatingCall(JoinPoint joinPoint, Object result) {
        try {
            String principal = SecurityUtils.getCurrentUserLogin().orElse("ANONYMOUS");
            String action = joinPoint.getSignature().getName();
            String entityName = joinPoint.getTarget().getClass().getSimpleName().replace("Controller", "");

            HttpServletRequest request = null;
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                request = attributes.getRequest();
            }

            AuditLog auditLog = new AuditLog();
            auditLog.setPrincipal(principal);
            auditLog.setAction(action.toUpperCase());
            auditLog.setEntityName(entityName);
            auditLog.setClientIp(request != null ? request.getRemoteAddr() : "UNKNOWN");
            auditLog.setUserAgent(request != null ? request.getHeader("User-Agent") : "UNKNOWN");
            auditLog.setCreatedAt(Instant.now());

            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Failed to persist audit log entry", e);
        }
    }
}
