package com.talentflow.application;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PipelineMapper {

    @Mapping(source = "changedBy.id", target = "changedById")
    @Mapping(target = "changedByName",
            expression = "java(history.getChangedBy().getFirstName() + \" \" + history.getChangedBy().getLastName())")
    StageHistoryDto toDto(StageHistory history);

    @Mapping(source = "author.id", target = "authorId")
    @Mapping(target = "authorName",
            expression = "java(note.getAuthor().getFirstName() + \" \" + note.getAuthor().getLastName())")
    NoteDto toDto(Note note);
}
