package com.talentflow.application;

import com.talentflow.auth.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    /** Postuler : multipart/form-data avec "cv" (PDF, 5 Mo max), "coverLetter" et "source" optionnels. */
    @PostMapping(value = "/jobs/{jobId}/applications", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CANDIDATE')")
    public ApplicationDto apply(@PathVariable Long jobId,
                                @RequestPart("cv") MultipartFile cv,
                                @RequestParam(required = false) String coverLetter,
                                @RequestParam(required = false) String source,
                                @AuthenticationPrincipal CustomUserDetails user) {
        return applicationService.apply(jobId, cv, coverLetter, source, user.getId());
    }

    @GetMapping("/applications/mine")
    @PreAuthorize("hasRole('CANDIDATE')")
    public Page<ApplicationDto> mine(@RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "20") int size,
                                     @AuthenticationPrincipal CustomUserDetails user) {
        return applicationService.mine(user.getId(), page, size);
    }

    @GetMapping("/applications")
    @PreAuthorize("hasAnyRole('HR','ADMIN','MANAGER')")
    public Page<ApplicationDto> list(@RequestParam(required = false) Long jobOfferId,
                                     @RequestParam(required = false) Stage stage,
                                     @RequestParam(required = false) String q,
                                     @RequestParam(required = false) String tag,
                                     @RequestParam(required = false) Instant from,
                                     @RequestParam(required = false) Instant to,
                                     @RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "20") int size,
                                     @AuthenticationPrincipal CustomUserDetails user) {
        ApplicationFilter filter = new ApplicationFilter(jobOfferId, stage, q, tag, from, to);
        return applicationService.list(user, filter, page, size);
    }

    @GetMapping("/applications/{id}")
    public ApplicationDto get(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user) {
        return applicationService.get(id, user);
    }

    @GetMapping("/applications/{id}/cv")
    public ResponseEntity<byte[]> cv(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user) {
        CvFile cv = applicationService.getCv(id, user);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(cv.filename()).build().toString())
                .body(cv.content());
    }

    @DeleteMapping("/applications/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('CANDIDATE')")
    public void withdraw(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user) {
        applicationService.withdraw(id, user.getId());
    }
}
