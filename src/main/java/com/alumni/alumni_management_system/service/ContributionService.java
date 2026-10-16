package com.alumni.alumni_management_system.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import com.alumni.alumni_management_system.dto.AdminContributionResponse;
import com.alumni.alumni_management_system.dto.ContributionRequest;
import com.alumni.alumni_management_system.dto.ContributionResponse;
import com.alumni.alumni_management_system.dto.PageResponse;
import com.alumni.alumni_management_system.exception.BadRequestException;
import com.alumni.alumni_management_system.exception.ResourceNotFoundException;
import com.alumni.alumni_management_system.model.AlumniProfile;
import com.alumni.alumni_management_system.model.Contribution;
import com.alumni.alumni_management_system.model.ContributionStatus;
import com.alumni.alumni_management_system.model.ContributionType;
import com.alumni.alumni_management_system.repository.AlumniProfileRepository;
import com.alumni.alumni_management_system.repository.ContributionRepository;

@Service
public class ContributionService {

    private static final Sort NEWEST_FIRST =
            Sort.by(Sort.Order.desc("contributionDate"), Sort.Order.desc("createdAt"));
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("100000000");

    private final ContributionRepository contributionRepository;
    private final AlumniProfileRepository profileRepository;
    private final MongoTemplate mongoTemplate;

    public ContributionService(ContributionRepository contributionRepository,
                               AlumniProfileRepository profileRepository,
                               MongoTemplate mongoTemplate) {
        this.contributionRepository = contributionRepository;
        this.profileRepository = profileRepository;
        this.mongoTemplate = mongoTemplate;
    }

    /* ---------------- alumni ---------------- */

    public ContributionResponse create(String userId, ContributionRequest request) {
        if (!profileRepository.existsByUserId(userId)) {
            throw new BadRequestException("Only alumni accounts can record contributions.");
        }

        Map<String, String> errors = new LinkedHashMap<>();

        LocalDate date = null;
        try {
            date = LocalDate.parse(request.contributionDate());
            // One day of tolerance so a browser in an earlier timezone is not wrongly rejected.
            if (date.isAfter(LocalDate.now().plusDays(1))) {
                errors.put("contributionDate", "The date cannot be in the future.");
            }
        } catch (DateTimeParseException e) {
            errors.put("contributionDate", "Enter a valid date.");
        }

        BigDecimal amount = request.amount();
        if (request.type() == ContributionType.DONATION) {
            if (amount == null) {
                errors.put("amount", "Enter the donation amount.");
            } else if (amount.signum() <= 0) {
                errors.put("amount", "Amount must be greater than zero.");
            } else if (amount.compareTo(MAX_AMOUNT) > 0) {
                errors.put("amount", "That amount is too large.");
            } else if (amount.stripTrailingZeros().scale() > 2) {
                errors.put("amount", "Use at most two decimal places.");
            } else {
                amount = amount.setScale(2, RoundingMode.HALF_UP);
            }
        } else if (amount != null) {
            errors.put("amount", "An amount can only be recorded for donations.");
        }

        InputChecks.throwIfAny(errors);

        Contribution contribution = new Contribution();
        contribution.setUserId(userId);
        contribution.setType(request.type());
        contribution.setDescription(request.description());
        contribution.setAmount(request.type() == ContributionType.DONATION ? amount : null);
        contribution.setContributionDate(date);
        contribution.setStatus(ContributionStatus.PENDING);
        contribution.setCreatedAt(Instant.now());

        return ContributionResponse.from(contributionRepository.save(contribution));
    }

    public List<ContributionResponse> listMine(String userId) {
        return contributionRepository.findByUserId(userId, NEWEST_FIRST).stream()
                .map(ContributionResponse::from)
                .toList();
    }

    public List<ContributionResponse> recent(String userId, int limit) {
        Pageable pageable = PageRequest.of(0, limit, NEWEST_FIRST);
        return contributionRepository.findByUserId(userId, pageable).stream()
                .map(ContributionResponse::from)
                .toList();
    }

    /* ---------------- admin ---------------- */

    public PageResponse<AdminContributionResponse> adminList(ContributionType type, ContributionStatus status,
                                                             String alumniId, int page, int size) {
        Pageable pageable = InputChecks.pageable(page, size, NEWEST_FIRST);
        List<Criteria> filters = new ArrayList<>();

        if (type != null) {
            filters.add(Criteria.where("type").is(type));
        }
        if (status != null) {
            filters.add(Criteria.where("status").is(status));
        }
        if (alumniId != null && !alumniId.isBlank()) {
            Optional<AlumniProfile> profile = profileRepository.findById(alumniId.trim());
            if (profile.isEmpty()) {
                return PageResponse.of(List.of(), pageable.getPageNumber(), pageable.getPageSize(), 0);
            }
            filters.add(Criteria.where("userId").is(profile.get().getUserId()));
        }

        Query query = filters.isEmpty()
                ? new Query()
                : new Query(new Criteria().andOperator(filters.toArray(new Criteria[0])));

        long total = mongoTemplate.count(query, Contribution.class);
        query.with(pageable);
        List<Contribution> items = mongoTemplate.find(query, Contribution.class);

        Map<String, AlumniProfile> owners = ownersOf(items);
        List<AdminContributionResponse> content = items.stream()
                .map(c -> AdminContributionResponse.of(c, owners.get(c.getUserId())))
                .toList();

        return PageResponse.of(content, pageable.getPageNumber(), pageable.getPageSize(), total);
    }

    public AdminContributionResponse updateStatus(String contributionId, ContributionStatus newStatus) {
        Contribution contribution = contributionRepository.findById(contributionId)
                .orElseThrow(() -> new ResourceNotFoundException("Contribution not found."));
        contribution.setStatus(newStatus);
        contribution = contributionRepository.save(contribution);
        AlumniProfile owner = profileRepository.findByUserId(contribution.getUserId()).orElse(null);
        return AdminContributionResponse.of(contribution, owner);
    }

    private Map<String, AlumniProfile> ownersOf(List<Contribution> items) {
        Set<String> userIds = items.stream().map(Contribution::getUserId).collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return profileRepository.findByUserIdIn(userIds).stream()
                .collect(Collectors.toMap(AlumniProfile::getUserId, Function.identity(), (a, b) -> a));
    }
}