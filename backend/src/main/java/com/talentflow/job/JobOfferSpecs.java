package com.talentflow.job;

import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;

/**
 * Filtres dynamiques pour la recherche d'offres.
 * Chaque méthode renvoie null si le filtre est vide (ignoré par Spring Data).
 */
public final class JobOfferSpecs {

    private JobOfferSpecs() {
    }

    public static Specification<JobOffer> hasStatus(JobStatus status) {
        return status == null ? null : (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<JobOffer> hasContractType(ContractType type) {
        return type == null ? null : (root, query, cb) -> cb.equal(root.get("contractType"), type);
    }

    public static Specification<JobOffer> locationContains(String location) {
        if (location == null || location.isBlank()) {
            return null;
        }
        String pattern = like(location);
        return (root, query, cb) -> cb.like(cb.lower(root.get("location")), pattern);
    }

    public static Specification<JobOffer> keyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        String pattern = like(keyword);
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("title")), pattern),
                cb.like(cb.lower(root.get("description")), pattern));
    }

    public static Specification<JobOffer> search(JobStatus status, String keyword,
                                                 String location, ContractType type) {
        return Specification.where(hasStatus(status))
                .and(keyword(keyword))
                .and(locationContains(location))
                .and(hasContractType(type));
    }

    private static String like(String value) {
        return "%" + value.trim().toLowerCase(Locale.ROOT) + "%";
    }
}
