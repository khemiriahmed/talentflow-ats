package com.talentflow.audit;

import com.talentflow.common.PageRequests;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditQueryService {

    private final AuditLogRepository auditLogRepository;

    @Transactional(readOnly = true)
    public Page<AuditLogDto> search(String action, String entityType, Long actorId, int page, int size) {
        Specification<AuditLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (action != null && !action.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("action")), "%" + action.trim().toLowerCase() + "%"));
            }
            if (entityType != null && !entityType.isBlank()) {
                predicates.add(cb.equal(root.get("entityType"), entityType.trim()));
            }
            if (actorId != null) {
                predicates.add(cb.equal(root.get("actor").get("id"), actorId));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        return auditLogRepository.findAll(spec, PageRequests.newestFirst(page, size))
                .map(this::toDto);
    }

    private AuditLogDto toDto(AuditLog log) {
        String actorName = log.getActor() == null
                ? null
                : log.getActor().getFirstName() + " " + log.getActor().getLastName();
        return new AuditLogDto(
                log.getId(),
                log.getAction(),
                log.getEntityType(),
                log.getEntityId(),
                log.getActor() == null ? null : log.getActor().getId(),
                actorName,
                log.getCreatedAt());
    }
}
