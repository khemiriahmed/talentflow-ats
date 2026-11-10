package com.talentflow.application;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NoteRepository extends JpaRepository<Note, Long> {

    @EntityGraph(attributePaths = {"author"})
    List<Note> findByApplicationIdOrderByCreatedAtDescIdDesc(Long applicationId);

    Optional<Note> findByIdAndApplicationId(Long id, Long applicationId);
}
