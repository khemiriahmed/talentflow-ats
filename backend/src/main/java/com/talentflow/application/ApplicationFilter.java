package com.talentflow.application;

import java.time.Instant;

/** Critères communs à la liste et à l'export des candidatures. */
public record ApplicationFilter(
        Long jobOfferId,
        Stage stage,
        String q,
        String tag,
        Instant from,
        Instant to) {
}
