package com.talentflow.job;

import com.talentflow.application.ApplicationRepository;
import com.talentflow.audit.AuditService;
import com.talentflow.auth.CustomUserDetails;
import com.talentflow.common.BadRequestException;
import com.talentflow.common.PageRequests;
import com.talentflow.common.ResourceNotFoundException;
import com.talentflow.user.Role;
import com.talentflow.user.User;
import com.talentflow.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static com.talentflow.job.JobStatus.ARCHIVED;
import static com.talentflow.job.JobStatus.CLOSED;
import static com.talentflow.job.JobStatus.DRAFT;
import static com.talentflow.job.JobStatus.PUBLISHED;

@Service
@RequiredArgsConstructor
public class JobOfferService {

    private static final Map<JobStatus, Set<JobStatus>> TRANSITIONS = Map.of(
            DRAFT, EnumSet.of(PUBLISHED, ARCHIVED),
            PUBLISHED, EnumSet.of(CLOSED, ARCHIVED),
            CLOSED, EnumSet.of(PUBLISHED, ARCHIVED),
            ARCHIVED, EnumSet.noneOf(JobStatus.class));

    private final JobOfferRepository jobOfferRepository;
    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;
    private final JobOfferMapper mapper;
    private final AuditService auditService;

    // ---------- public ----------

    @Transactional(readOnly = true)
    public Page<JobOfferPublicDto> searchPublic(String keyword, String location, ContractType type,
                                                int page, int size) {
        Specification<JobOffer> spec = JobOfferSpecs.search(PUBLISHED, keyword, location, type)
                .and(JobOfferSpecs.openForApplications());
        return jobOfferRepository.findAll(spec, PageRequests.newestFirst(page, size))
                .map(mapper::toPublicDto);
    }

    @Transactional(readOnly = true)
    public JobOfferPublicDto getPublic(Long id) {
        return jobOfferRepository.findById(id)
                .filter(job -> job.getStatus() == PUBLISHED)
                .map(mapper::toPublicDto)
                .orElseThrow(() -> new ResourceNotFoundException("Job offer not found"));
    }

    // ---------- staff ----------

    @Transactional(readOnly = true)
    public Page<JobOfferDto> searchManage(CustomUserDetails user, JobStatus status, String keyword,
                                          String location, ContractType type, int page, int size) {
        Specification<JobOffer> spec = JobOfferSpecs.search(status, keyword, location, type);
        if (user.getRole() == Role.MANAGER) {
            spec = spec.and(JobOfferSpecs.managerIs(user.getId()));
        }
        return jobOfferRepository.findAll(spec, PageRequests.newestFirst(page, size))
                .map(mapper::toDto);
    }

    @Transactional(readOnly = true)
    public JobOfferDto getManage(Long id, CustomUserDetails user) {
        JobOffer job = find(id);
        if (user.getRole() == Role.MANAGER
                && (job.getManager() == null || !job.getManager().getId().equals(user.getId()))) {
            throw new ResourceNotFoundException("Job offer not found");
        }
        return mapper.toDto(job);
    }

    @Transactional
    public JobOfferDto create(JobOfferRequest request, Long creatorId) {
        JobOffer job = new JobOffer();
        apply(job, request);
        job.setStatus(DRAFT);
        job.setCreatedBy(userRepository.getReferenceById(creatorId));
        JobOffer saved = jobOfferRepository.save(job);
        auditService.log(creatorId, "JOB_CREATED", "JobOffer", saved.getId());
        return mapper.toDto(saved);
    }

    @Transactional
    public JobOfferDto update(Long id, JobOfferRequest request) {
        JobOffer job = find(id);
        if (job.getStatus() == ARCHIVED) {
            throw new BadRequestException("Archived job offers cannot be modified");
        }
        apply(job, request);
        return mapper.toDto(job);
    }

    @Transactional
    public JobOfferDto changeStatus(Long id, JobStatus newStatus, Long actorId) {
        JobOffer job = find(id);
        if (!TRANSITIONS.get(job.getStatus()).contains(newStatus)) {
            throw new BadRequestException(
                    "Cannot move a job offer from " + job.getStatus() + " to " + newStatus);
        }
        if (newStatus == PUBLISHED && job.getDeadline() != null
                && job.getDeadline().isBefore(LocalDate.now())) {
            throw new BadRequestException("The deadline is in the past: update it before publishing");
        }
        job.setStatus(newStatus);
        auditService.log(actorId, "JOB_STATUS_" + newStatus, "JobOffer", id);
        return mapper.toDto(job);
    }

    @Transactional
    public void delete(Long id, Long actorId) {
        JobOffer job = find(id);
        if (applicationRepository.countByJobOfferId(id) > 0) {
            throw new BadRequestException("This job offer has applications: archive it instead");
        }
        jobOfferRepository.delete(job);
        auditService.log(actorId, "JOB_DELETED", "JobOffer", id);
    }

    // ---------- helpers ----------

    private JobOffer find(Long id) {
        return jobOfferRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job offer not found"));
    }

    private void apply(JobOffer job, JobOfferRequest r) {
        if (r.salaryMin() != null && r.salaryMax() != null && r.salaryMin().compareTo(r.salaryMax()) > 0) {
            throw new BadRequestException("salaryMin must not exceed salaryMax");
        }
        job.setTitle(r.title().trim());
        job.setDescription(r.description().trim());
        job.setDepartment(r.department());
        job.setLocation(r.location());
        job.setContractType(r.contractType());
        job.setSalaryMin(r.salaryMin());
        job.setSalaryMax(r.salaryMax());
        job.setDeadline(r.deadline());
        job.setManager(resolveManager(r.managerId()));
    }

    private User resolveManager(Long managerId) {
        if (managerId == null) {
            return null;
        }
        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new BadRequestException("Manager not found"));
        if (manager.getRole() != Role.MANAGER) {
            throw new BadRequestException("The selected user does not have the MANAGER role");
        }
        return manager;
    }
}
