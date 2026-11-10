package com.talentflow.application;

import com.talentflow.auth.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.TreeSet;

@Service
@RequiredArgsConstructor
public class TagService {

    private final ApplicationAccess access;
    private final ApplicationMapper mapper;

    /** Remplace l'ensemble des tags (normalisés, sans doublon). */
    @Transactional
    public ApplicationDto replaceTags(Long applicationId, Set<String> rawTags, CustomUserDetails user) {
        Application application = access.findVisible(applicationId, user);
        Set<String> normalized = new TreeSet<>();
        for (String raw : rawTags) {
            normalized.add(Tags.normalizeAndValidate(raw));
        }
        application.getTags().clear();
        application.getTags().addAll(normalized);
        return mapper.toDto(application);
    }
}
