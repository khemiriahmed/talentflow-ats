package com.talentflow.application;

import com.talentflow.auth.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
public class ApplicationExportController {

    private final ApplicationExportService exportService;

    /** Mêmes filtres que la liste (jobOfferId, stage, q, tag, from, to). */
    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public ResponseEntity<byte[]> export(@RequestParam(required = false) Long jobOfferId,
                                         @RequestParam(required = false) Stage stage,
                                         @RequestParam(required = false) String q,
                                         @RequestParam(required = false) String tag,
                                         @RequestParam(required = false) Instant from,
                                         @RequestParam(required = false) Instant to,
                                         @AuthenticationPrincipal CustomUserDetails user) {
        byte[] csv = exportService.exportCsv(new ApplicationFilter(jobOfferId, stage, q, tag, from, to), user);
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("applications-" + LocalDate.now() + ".csv").build().toString())
                .body(csv);
    }
}
