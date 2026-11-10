package com.talentflow.interview;

import com.talentflow.auth.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewService interviewService;

    @PostMapping("/applications/{applicationId}/interviews")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public InterviewDto schedule(@PathVariable Long applicationId,
                                 @Valid @RequestBody InterviewRequest request,
                                 @AuthenticationPrincipal CustomUserDetails user) {
        return interviewService.schedule(applicationId, request, user);
    }

    @GetMapping("/applications/{applicationId}/interviews")
    @PreAuthorize("hasAnyRole('HR','ADMIN','MANAGER')")
    public List<InterviewDto> forApplication(@PathVariable Long applicationId,
                                             @AuthenticationPrincipal CustomUserDetails user) {
        return interviewService.listForApplication(applicationId, user);
    }

    /** Calendrier : ?from=2026-11-10T00:00:00Z&to=2026-11-17T00:00:00Z (91 jours max). */
    @GetMapping("/interviews")
    @PreAuthorize("hasAnyRole('HR','ADMIN','MANAGER')")
    public List<InterviewDto> calendar(@RequestParam Instant from, @RequestParam Instant to,
                                       @AuthenticationPrincipal CustomUserDetails user) {
        return interviewService.calendar(from, to, user);
    }

    @PatchMapping("/interviews/{id}/cancel")
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public InterviewDto cancel(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user) {
        return interviewService.cancel(id, user);
    }

    @PostMapping("/interviews/{id}/feedback")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('HR','ADMIN','MANAGER')")
    public FeedbackDto submitFeedback(@PathVariable Long id, @Valid @RequestBody FeedbackRequest request,
                                      @AuthenticationPrincipal CustomUserDetails user) {
        return interviewService.submitFeedback(id, request, user);
    }

    @GetMapping("/interviews/{id}/feedback")
    @PreAuthorize("hasAnyRole('HR','ADMIN','MANAGER')")
    public FeedbackDto feedback(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user) {
        return interviewService.getFeedback(id, user);
    }
}
