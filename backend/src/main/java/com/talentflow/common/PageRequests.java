package com.talentflow.common;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Pagination sûre : taille bornée et tri fixe (le client ne choisit jamais la colonne de tri).
 */
public final class PageRequests {

    private static final int MAX_SIZE = 100;

    private PageRequests() {
    }

    public static Pageable newestFirst(int page, int size) {
        return PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_SIZE),
                Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}
