package com.talentflow.job;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface JobOfferMapper {

    JobOfferPublicDto toPublicDto(JobOffer job);

    @Mapping(source = "createdBy.id", target = "createdById")
    @Mapping(source = "manager.id", target = "managerId")
    JobOfferDto toDto(JobOffer job);
}
