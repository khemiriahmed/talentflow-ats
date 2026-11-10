package com.talentflow.application;

import com.talentflow.auth.CustomUserDetails;
import com.talentflow.common.ResourceNotFoundException;
import com.talentflow.user.Role;
import com.talentflow.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Notes internes sur une candidature (jamais visibles par le candidat). */
@Service
@RequiredArgsConstructor
public class NoteService {

    private final ApplicationAccess access;
    private final NoteRepository noteRepository;
    private final UserRepository userRepository;
    private final PipelineMapper mapper;

    @Transactional
    public NoteDto add(Long applicationId, String content, CustomUserDetails user) {
        Application application = access.findVisible(applicationId, user);
        Note note = new Note();
        note.setApplication(application);
        note.setAuthor(userRepository.getReferenceById(user.getId()));
        note.setContent(content.trim());
        Note saved = noteRepository.saveAndFlush(note);
        return mapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<NoteDto> list(Long applicationId, CustomUserDetails user) {
        access.findVisible(applicationId, user);
        return noteRepository.findByApplicationIdOrderByCreatedAtDescIdDesc(applicationId)
                .stream().map(mapper::toDto).toList();
    }

    /** Seul l'auteur (ou un Admin) peut supprimer une note. */
    @Transactional
    public void delete(Long applicationId, Long noteId, CustomUserDetails user) {
        access.findVisible(applicationId, user);
        Note note = noteRepository.findByIdAndApplicationId(noteId, applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Note not found"));
        if (user.getRole() != Role.ADMIN && !note.getAuthor().getId().equals(user.getId())) {
            throw new AccessDeniedException("Only the author can delete this note");
        }
        noteRepository.delete(note);
    }
}
