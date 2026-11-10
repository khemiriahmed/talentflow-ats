package com.talentflow.application;

import com.talentflow.user.User;
import org.mapstruct.InheritConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface ApplicationMapper {

    /** Vue staff (avec les tags). */
    @Mapping(source = "jobOffer.id", target = "jobOfferId")
    @Mapping(source = "jobOffer.title", target = "jobTitle")
    @Mapping(source = "candidate.id", target = "candidateId")
    @Mapping(source = "candidate", target = "candidateName", qualifiedByName = "fullName")
    @Mapping(source = "candidate.email", target = "candidateEmail")
    ApplicationDto toDto(Application application);

    /** Vue candidat : mêmes champs, sans les tags internes. */
    @InheritConfiguration(name = "toDto")
    @Mapping(target = "tags", ignore = true)
    ApplicationDto toCandidateDto(Application application);

    @Named("fullName")
    default String fullName(User user) {
        return user.getFirstName() + " " + user.getLastName();
    }
}
