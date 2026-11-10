package com.talentflow.application;

import com.talentflow.auth.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
public class PipelineController {

    private final PipelineService pipelineService;
    private final NoteService noteService;

    @PatchMapping("/{id}/stage")
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public ApplicationDto changeStage(@PathVariable Long id,
                                      @Valid @RequestBody StageChangeRequest request,
                                      @AuthenticationPrincipal CustomUserDetails user) {
        return pipelineService.changeStage(id, request.stage(), request.reason(), user);
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("hasAnyRole('HR','ADMIN','MANAGER')")
    public List<StageHistoryDto> history(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user) {
        return pipelineService.history(id, user);
    }

    @PostMapping("/{id}/notes")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('HR','ADMIN','MANAGER')")
    public NoteDto addNote(@PathVariable Long id, @Valid @RequestBody NoteRequest request,
                           @AuthenticationPrincipal CustomUserDetails user) {
        return noteService.add(id, request.content(), user);
    }

    @GetMapping("/{id}/notes")
    @PreAuthorize("hasAnyRole('HR','ADMIN','MANAGER')")
    public List<NoteDto> notes(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user) {
        return noteService.list(id, user);
    }

    @DeleteMapping("/{id}/notes/{noteId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('HR','ADMIN','MANAGER')")
    public void deleteNote(@PathVariable Long id, @PathVariable Long noteId,
                           @AuthenticationPrincipal CustomUserDetails user) {
        noteService.delete(id, noteId, user);
    }
}
