package com.talentflow.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Journal d'audit (RF-70), consultable uniquement par un administrateur. */
@RestController
@RequestMapping("/api/v1/admin/audit")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AuditController {

    private final AuditQueryService auditQueryService;

    @GetMapping
    public Page<AuditLogDto> list(@RequestParam(required = false) String action,
                                  @RequestParam(required = false) String entityType,
                                  @RequestParam(required = false) Long actorId,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "30") int size) {
        return auditQueryService.search(action, entityType, actorId, page, size);
    }
}
