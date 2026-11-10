package com.talentflow.application;

import com.talentflow.user.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface ApplicationMapper {

    @Mapping(source = "jobOffer.id", target = "jobOfferId")
    @Mapping(source = "jobOffer.title", target = "jobTitle")
    @Mapping(source = "candidate.id", target = "candidateId")
    @Mapping(source = "candidate", target = "candidateName", qualifiedByName = "fullName")
    @Mapping(source = "candidate.email", target = "candidateEmail")
    ApplicationDto toDto(Application application);

    @Named("fullName")
    default String fullName(User user) {
        return user.getFirstName() + " " + user.getLastName();
    }
}
