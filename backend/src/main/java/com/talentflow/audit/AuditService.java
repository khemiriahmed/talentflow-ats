package com.talentflow.audit;

import com.talentflow.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    @Transactional
    public void log(Long actorId, String action, String entityType, Long entityId) {
        AuditLog entry = new AuditLog();
        if (actorId != null) {
            entry.setActor(userRepository.getReferenceById(actorId));
        }
        entry.setAction(action);
        entry.setEntityType(entityType);
        entry.setEntityId(entityId);
        auditLogRepository.save(entry);
    }
}
