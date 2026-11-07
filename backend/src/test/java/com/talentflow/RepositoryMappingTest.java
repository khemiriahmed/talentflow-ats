package com.talentflow;

import com.talentflow.application.Application;
import com.talentflow.application.ApplicationRepository;
import com.talentflow.application.Stage;
import com.talentflow.job.ContractType;
import com.talentflow.job.JobOffer;
import com.talentflow.job.JobOfferRepository;
import com.talentflow.job.JobOfferSpecs;
import com.talentflow.job.JobStatus;
import com.talentflow.user.Role;
import com.talentflow.user.User;
import com.talentflow.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Vérifie que les entités JPA correspondent au schéma Flyway (ddl-auto=validate)
 * et que les requêtes de base fonctionnent. Nécessite Docker.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class RepositoryMappingTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    UserRepository users;
    @Autowired
    JobOfferRepository jobs;
    @Autowired
    ApplicationRepository applications;

    @Test
    void persistsAndQueriesTheRecruitmentFlow() {
        User hr = users.saveAndFlush(newUser("hr@test.com", Role.HR));
        User candidate = users.saveAndFlush(newUser("candidate@test.com", Role.CANDIDATE));

        JobOffer job = new JobOffer();
        job.setTitle("Développeur Spring Boot");
        job.setDescription("API REST, PostgreSQL, Docker");
        job.setLocation("Paris");
        job.setContractType(ContractType.CDI);
        job.setStatus(JobStatus.PUBLISHED);
        job.setCreatedBy(hr);
        job = jobs.saveAndFlush(job);

        Application application = new Application();
        application.setJobOffer(job);
        application.setCandidate(candidate);
        application.setCvFileKey("cv/candidate-1.pdf");
        application = applications.saveAndFlush(application);

        assertThat(application.getCurrentStage()).isEqualTo(Stage.RECEIVED);
        assertThat(application.getCreatedAt()).isNotNull();
        assertThat(users.findByEmail("hr@test.com")).isPresent();
        assertThat(applications.existsByJobOfferIdAndCandidateId(job.getId(), candidate.getId())).isTrue();

        Page<JobOffer> found = jobs.findAll(
                JobOfferSpecs.search(JobStatus.PUBLISHED, "spring", "paris", ContractType.CDI),
                PageRequest.of(0, 10));
        assertThat(found.getTotalElements()).isEqualTo(1);

        Page<JobOffer> none = jobs.findAll(
                JobOfferSpecs.search(JobStatus.PUBLISHED, "cobol", null, null),
                PageRequest.of(0, 10));
        assertThat(none.getTotalElements()).isZero();
    }

    private static User newUser(String email, Role role) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash("hash");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setRole(role);
        return user;
    }
}
