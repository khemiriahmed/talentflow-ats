package com.talentflow.dashboard;

import com.talentflow.application.Application;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

/**
 * Requêtes d'agrégation en lecture seule. Période = candidatures créées dans [from, to[.
 * jobId = 0 signifie "toutes les offres" (évite les paramètres nuls, mal typés par PostgreSQL).
 */
public interface DashboardRepository extends Repository<Application, Long> {

    /** Répartition actuelle des candidatures par étape. */
    @Query("""
            SELECT new com.talentflow.dashboard.StageCount(a.currentStage, COUNT(a))
            FROM Application a
            WHERE a.createdAt >= :from AND a.createdAt < :to
              AND (:jobId = 0L OR a.jobOffer.id = :jobId)
            GROUP BY a.currentStage
            """)
    List<StageCount> countByCurrentStage(@Param("from") Instant from, @Param("to") Instant to,
                                         @Param("jobId") long jobId);

    /** Entonnoir : nombre de candidatures ayant atteint chaque étape (via l'historique). */
    @Query("""
            SELECT new com.talentflow.dashboard.StageCount(h.toStage, COUNT(DISTINCT a.id))
            FROM StageHistory h JOIN h.application a
            WHERE a.createdAt >= :from AND a.createdAt < :to
              AND (:jobId = 0L OR a.jobOffer.id = :jobId)
            GROUP BY h.toStage
            """)
    List<StageCount> countReachedStage(@Param("from") Instant from, @Param("to") Instant to,
                                       @Param("jobId") long jobId);

    @Query("""
            SELECT new com.talentflow.dashboard.LabelCount(a.source, COUNT(a))
            FROM Application a
            WHERE a.createdAt >= :from AND a.createdAt < :to
              AND (:jobId = 0L OR a.jobOffer.id = :jobId)
            GROUP BY a.source
            ORDER BY COUNT(a) DESC
            """)
    List<LabelCount> countBySource(@Param("from") Instant from, @Param("to") Instant to,
                                   @Param("jobId") long jobId);

    @Query("""
            SELECT new com.talentflow.dashboard.JobCount(j.id, j.title, COUNT(a))
            FROM Application a JOIN a.jobOffer j
            WHERE a.createdAt >= :from AND a.createdAt < :to
              AND (:jobId = 0L OR j.id = :jobId)
            GROUP BY j.id, j.title
            ORDER BY COUNT(a) DESC
            """)
    List<JobCount> countByJob(@Param("from") Instant from, @Param("to") Instant to,
                              @Param("jobId") long jobId, Pageable pageable);

    /** Délai candidature, embauche pour les candidatures embauchées. */
    @Query("""
            SELECT new com.talentflow.dashboard.HireTiming(a.createdAt, h.changedAt)
            FROM StageHistory h JOIN h.application a
            WHERE h.toStage = com.talentflow.application.Stage.HIRED
              AND a.createdAt >= :from AND a.createdAt < :to
              AND (:jobId = 0L OR a.jobOffer.id = :jobId)
            """)
    List<HireTiming> hireTimings(@Param("from") Instant from, @Param("to") Instant to,
                                 @Param("jobId") long jobId);

    @Query("""
            SELECT a.createdAt FROM Application a
            WHERE a.createdAt >= :from AND a.createdAt < :to
              AND (:jobId = 0L OR a.jobOffer.id = :jobId)
            """)
    List<Instant> applicationDates(@Param("from") Instant from, @Param("to") Instant to,
                                   @Param("jobId") long jobId);
}
