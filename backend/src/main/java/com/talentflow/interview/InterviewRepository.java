package com.talentflow.interview;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface InterviewRepository extends JpaRepository<Interview, Long> {

    List<Interview> findByApplicationId(Long applicationId);

    List<Interview> findByStartAtBetweenOrderByStartAt(Instant from, Instant to);

    @EntityGraph(attributePaths = {"application", "application.jobOffer", "application.candidate", "participants"})
    List<Interview> findByApplicationIdOrderByStartAtAsc(Long applicationId);

    /** Calendrier RH / Admin : tous les entretiens de la période [from, to[. */
    @EntityGraph(attributePaths = {"application", "application.jobOffer", "application.candidate", "participants"})
    @Query("""
            SELECT i FROM Interview i
            WHERE i.startAt >= :from AND i.startAt < :to
            ORDER BY i.startAt
            """)
    List<Interview> findInRange(@Param("from") Instant from, @Param("to") Instant to);

    /** Calendrier Manager : entretiens où il participe ou sur les offres dont il est responsable. */
    @EntityGraph(attributePaths = {"application", "application.jobOffer", "application.candidate", "participants"})
    @Query("""
            SELECT i FROM Interview i
              JOIN i.application a
              JOIN a.jobOffer j
              LEFT JOIN j.manager m
            WHERE i.startAt >= :from AND i.startAt < :to
              AND (m.id = :userId
                   OR EXISTS (SELECT 1 FROM i.participants p WHERE p.id = :userId))
            ORDER BY i.startAt
            """)
    List<Interview> findInRangeForUser(@Param("from") Instant from, @Param("to") Instant to,
                                       @Param("userId") Long userId);

    /** Nombre d'entretiens planifiés qui chevauchent [start, end[ pour au moins un des participants. */
    @Query(value = """
            SELECT COUNT(*) FROM interviews i
              JOIN interview_participants ip ON ip.interview_id = i.id
            WHERE ip.user_id IN (:userIds)
              AND i.status = 'SCHEDULED'
              AND i.start_at < :end
              AND i.start_at + (i.duration_min * INTERVAL '1 minute') > :start
            """, nativeQuery = true)
    long countOverlapping(@Param("userIds") Collection<Long> userIds,
                          @Param("start") Instant start,
                          @Param("end") Instant end);

    /** Entretiens planifiés dans les prochaines 24 h dont le rappel n'a pas encore été envoyé. */
    @EntityGraph(attributePaths = {"application", "application.jobOffer", "application.candidate", "participants"})
    @Query("""
            SELECT i FROM Interview i
            WHERE i.status = com.talentflow.interview.InterviewStatus.SCHEDULED
              AND i.reminderSentAt IS NULL
              AND i.startAt > :now AND i.startAt <= :horizon
            ORDER BY i.startAt
            """)
    List<Interview> findDueForReminder(@Param("now") Instant now, @Param("horizon") Instant horizon);

    /** Réserve atomiquement le rappel : renvoie 1 si cette instance l'a pris, 0 sinon. */
    @Modifying
    @Query("UPDATE Interview i SET i.reminderSentAt = :now WHERE i.id = :id AND i.reminderSentAt IS NULL")
    int claimReminder(@Param("id") Long id, @Param("now") Instant now);
}
