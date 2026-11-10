package com.talentflow.job;

import com.talentflow.auth.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
public class JobOfferController {

    private final JobOfferService jobOfferService;

    // ---------- public ----------

    @GetMapping
    public Page<JobOfferPublicDto> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) ContractType contractType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return jobOfferService.searchPublic(keyword, location, contractType, page, size);
    }

    @GetMapping("/{id}")
    public JobOfferPublicDto get(@PathVariable Long id) {
        return jobOfferService.getPublic(id);
    }

    // ---------- staff ----------

    @GetMapping("/manage")
    @PreAuthorize("hasAnyRole('HR','ADMIN','MANAGER')")
    public Page<JobOfferDto> manage(
            @RequestParam(required = false) JobStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) ContractType contractType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal CustomUserDetails user) {
        return jobOfferService.searchManage(user, status, keyword, location, contractType, page, size);
    }

    @GetMapping("/manage/{id}")
    @PreAuthorize("hasAnyRole('HR','ADMIN','MANAGER')")
    public JobOfferDto getManage(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user) {
        return jobOfferService.getManage(id, user);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public JobOfferDto create(@Valid @RequestBody JobOfferRequest request,
                              @AuthenticationPrincipal CustomUserDetails user) {
        return jobOfferService.create(request, user.getId());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public JobOfferDto update(@PathVariable Long id, @Valid @RequestBody JobOfferRequest request) {
        return jobOfferService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public JobOfferDto changeStatus(@PathVariable Long id, @Valid @RequestBody ChangeStatusRequest request,
                                    @AuthenticationPrincipal CustomUserDetails user) {
        return jobOfferService.changeStatus(id, request.status(), user.getId());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public void delete(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user) {
        jobOfferService.delete(id, user.getId());
    }
}
