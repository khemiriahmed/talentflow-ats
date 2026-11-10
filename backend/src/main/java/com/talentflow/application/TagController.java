package com.talentflow.application;

import com.talentflow.auth.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    @PutMapping("/{id}/tags")
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public ApplicationDto replaceTags(@PathVariable Long id, @Valid @RequestBody TagsRequest request,
                                      @AuthenticationPrincipal CustomUserDetails user) {
        return tagService.replaceTags(id, request.tags(), user);
    }
}
