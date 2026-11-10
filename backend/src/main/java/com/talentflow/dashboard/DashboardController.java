package com.talentflow.dashboard;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * Indicateurs de recrutement (RF-60 à RF-63) sur les candidatures créées dans [from, to[.
     * Par défaut : les 90 derniers jours, toutes offres confondues, découpage automatique.
     */
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public DashboardStatsDto stats(@RequestParam(required = false) Instant from,
                                   @RequestParam(required = false) Instant to,
                                   @RequestParam(required = false) Long jobOfferId,
                                   @RequestParam(required = false) Granularity granularity) {
        return dashboardService.stats(from, to, jobOfferId, granularity);
    }
}
