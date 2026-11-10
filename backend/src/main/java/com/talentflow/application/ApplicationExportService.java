package com.talentflow.application;

import com.talentflow.audit.AuditService;
import com.talentflow.auth.CustomUserDetails;
import com.talentflow.common.BadRequestException;
import com.talentflow.common.CsvFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.TreeSet;

/** Export CSV des candidatures (RF-64) : données personnelles, donc RH / Admin et tracé dans l'audit. */
@Service
@RequiredArgsConstructor
public class ApplicationExportService {

    static final int MAX_ROWS = 10_000;
    private static final int PAGE_SIZE = 500;

    private final ApplicationRepository applicationRepository;
    private final AuditService auditService;

    @Transactional
    public byte[] exportCsv(ApplicationFilter filter, CustomUserDetails actor) {
        Specification<Application> spec = ApplicationSpecs.filtered(filter);
        long total = applicationRepository.count(spec);
        if (total > MAX_ROWS) {
            throw new BadRequestException(
                    "Too many applications to export (" + total + "): narrow your filters (max " + MAX_ROWS + ")");
        }

        StringBuilder csv = new StringBuilder(CsvFormat.BOM);
        CsvFormat.appendRow(csv, List.of("id", "jobOfferId", "jobTitle", "candidateName",
                "candidateEmail", "stage", "source", "tags", "createdAt"));

        int page = 0;
        Page<Application> chunk;
        do {
            chunk = applicationRepository.findAll(spec, PageRequest.of(page++, PAGE_SIZE, Sort.by("id")));
            for (Application a : chunk) {
                CsvFormat.appendRow(csv, List.of(
                        String.valueOf(a.getId()),
                        String.valueOf(a.getJobOffer().getId()),
                        a.getJobOffer().getTitle(),
                        a.getCandidate().getFirstName() + " " + a.getCandidate().getLastName(),
                        a.getCandidate().getEmail(),
                        a.getCurrentStage().name(),
                        a.getSource() == null ? "" : a.getSource(),
                        String.join(", ", new TreeSet<>(a.getTags())),
                        a.getCreatedAt().toString()));
            }
        } while (chunk.hasNext());

        auditService.log(actor.getId(), "APPLICATIONS_EXPORTED", "Application", null);
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }
}
