package com.talentflow.application;

import com.talentflow.job.JobOffer;
import com.talentflow.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "applications")
@Getter
@Setter
@NoArgsConstructor
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_offer_id", nullable = false)
    private JobOffer jobOffer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidate_id", nullable = false)
    private User candidate;

    @Column(name = "cover_letter", columnDefinition = "TEXT")
    private String coverLetter;

    @Column(name = "cv_file_key", nullable = false, length = 500)
    private String cvFileKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_stage", nullable = false, length = 30)
    private Stage currentStage = Stage.RECEIVED;

    @Column(length = 50)
    private String source;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Tags internes (visibles du staff uniquement). */
    @ElementCollection
    @CollectionTable(name = "application_tags", joinColumns = @JoinColumn(name = "application_id"))
    @Column(name = "tag", nullable = false, length = 50)
    @BatchSize(size = 100)
    private Set<String> tags = new HashSet<>();
}
