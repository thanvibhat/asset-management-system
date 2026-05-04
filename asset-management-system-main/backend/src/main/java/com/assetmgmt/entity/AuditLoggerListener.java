package com.assetmgmt.entity;

import com.assetmgmt.repository.AuditLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.*;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AuditLoggerListener implements ApplicationContextAware {

    private static ApplicationContext context;
    private static final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        context = applicationContext;
    }

    @PostPersist
    public void onPostPersist(Object entity) {
        logAudit(entity, "CREATE");
    }

    @PostUpdate
    public void onPostUpdate(Object entity) {
        logAudit(entity, "UPDATE");
    }

    @PostRemove
    public void onPostRemove(Object entity) {
        logAudit(entity, "DELETE");
    }

    private void logAudit(Object entity, String action) {
        if (entity instanceof AuditLog) {
            return; // Don't audit the audit log
        }
        
        if (context == null) {
            return; // Context not ready yet (e.g. during startup data seeding)
        }

        try {
            AuditLogRepository auditLogRepository = context.getBean(AuditLogRepository.class);
            
            String entityType = entity.getClass().getSimpleName();
            Long entityId = extractId(entity);
            
            if (entityId == null) return;
            
            String username = "SYSTEM";
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
                username = auth.getName();
            }

            String details = "{}";
            try {
                details = objectMapper.writeValueAsString(entity);
            } catch (JsonProcessingException e) {
                // Keep default empty JSON object if serialization fails
            }

            AuditLog log = AuditLog.builder()
                .entityType(entityType)
                .entityId(entityId)
                .action(action)
                .performedBy(username)
                .details(details)
                .build();
                
            auditLogRepository.save(log);
            
        } catch (Exception e) {
            System.err.println("Failed to log audit for " + entity.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }
    
    private Long extractId(Object entity) {
        try {
            return (Long) entity.getClass().getMethod("getId").invoke(entity);
        } catch (Exception e) {
            return null;
        }
    }
}
