package com.talentflow.interview;

import com.talentflow.user.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InterviewMapper {

    @Mapping(source = "application.id", target = "applicationId")
    @Mapping(source = "application.jobOffer.title", target = "jobTitle")
    @Mapping(source = "application.candidate.id", target = "candidateId")
    @Mapping(target = "candidateName",
            expression = "java(interview.getApplication().getCandidate().getFirstName() + \" \" + interview.getApplication().getCandidate().getLastName())")
    InterviewDto toDto(Interview interview);

    @Mapping(target = "name", expression = "java(user.getFirstName() + \" \" + user.getLastName())")
    ParticipantDto toParticipant(User user);

    @Mapping(source = "interview.id", target = "interviewId")
    @Mapping(source = "author.id", target = "authorId")
    @Mapping(target = "authorName",
            expression = "java(feedback.getAuthor().getFirstName() + \" \" + feedback.getAuthor().getLastName())")
    FeedbackDto toDto(Feedback feedback);
}
