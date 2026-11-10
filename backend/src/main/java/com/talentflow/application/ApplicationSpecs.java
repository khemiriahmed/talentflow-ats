package com.talentflow.application;

import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.Locale;
import java.util.Set;

public final class ApplicationSpecs {

    private ApplicationSpecs() {
    }

    public static Specification<Application> forJobOffer(Long jobOfferId) {
        return jobOfferId == null ? null
                : (root, query, cb) -> cb.equal(root.get("jobOffer").get("id"), jobOfferId);
    }

    public static Specification<Application> inStage(Stage stage) {
        return stage == null ? null : (root, query, cb) -> cb.equal(root.get("currentStage"), stage);
    }

    public static Specification<Application> ofCandidate(Long candidateId) {
        return (root, query, cb) -> cb.equal(root.get("candidate").get("id"), candidateId);
    }

    /** Candidatures sur les offres dont le manager est l'utilisateur donné. */
    public static Specification<Application> managedBy(Long managerId) {
        return (root, query, cb) ->
                cb.equal(root.get("jobOffer").get("manager").get("id"), managerId);
    }

    /** Recherche par nom ou email du candidat. */
    public static Specification<Application> candidateMatches(String q) {
        if (q == null || q.isBlank()) {
            return null;
        }
        String pattern = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("candidate").get("email")), pattern),
                cb.like(cb.lower(root.get("candidate").get("firstName")), pattern),
                cb.like(cb.lower(root.get("candidate").get("lastName")), pattern));
    }

    public static Specification<Application> hasTag(String tag) {
        if (tag == null || tag.isBlank()) {
            return null;
        }
        String normalized = Tags.normalize(tag);
        return (root, query, cb) -> cb.isMember(normalized, root.<Set<String>>get("tags"));
    }

    public static Specification<Application> createdFrom(Instant from) {
        return from == null ? null
                : (root, query, cb) -> cb.greaterThanOrEqualTo(root.<Instant>get("createdAt"), from);
    }

    public static Specification<Application> createdBefore(Instant to) {
        return to == null ? null
                : (root, query, cb) -> cb.lessThan(root.<Instant>get("createdAt"), to);
    }

    public static Specification<Application> filtered(ApplicationFilter filter) {
        return Specification.where(forJobOffer(filter.jobOfferId()))
                .and(inStage(filter.stage()))
                .and(candidateMatches(filter.q()))
                .and(hasTag(filter.tag()))
                .and(createdFrom(filter.from()))
                .and(createdBefore(filter.to()));
    }
}
