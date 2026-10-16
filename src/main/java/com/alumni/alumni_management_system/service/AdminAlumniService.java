package com.alumni.alumni_management_system.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import com.alumni.alumni_management_system.dto.AdminAlumniRequest;
import com.alumni.alumni_management_system.dto.AdminAlumniResponse;
import com.alumni.alumni_management_system.dto.PageResponse;
import com.alumni.alumni_management_system.dto.TextUtil;
import com.alumni.alumni_management_system.exception.BadRequestException;
import com.alumni.alumni_management_system.exception.DuplicateEmailException;
import com.alumni.alumni_management_system.exception.ResourceNotFoundException;
import com.alumni.alumni_management_system.model.AlumniProfile;
import com.alumni.alumni_management_system.model.Role;
import com.alumni.alumni_management_system.model.User;
import com.alumni.alumni_management_system.repository.AlumniProfileRepository;
import com.alumni.alumni_management_system.repository.ContributionRepository;
import com.alumni.alumni_management_system.repository.UserRepository;

@Service
public class AdminAlumniService {

    private static final Sort BY_NAME = Sort.by(Sort.Order.asc("fullName"), Sort.Order.asc("id"));

    private final AlumniProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final ContributionRepository contributionRepository;
    private final AccountService accountService;
    private final ProfileService profileService;
    private final MongoTemplate mongoTemplate;

    public AdminAlumniService(AlumniProfileRepository profileRepository,
                              UserRepository userRepository,
                              ContributionRepository contributionRepository,
                              AccountService accountService,
                              ProfileService profileService,
                              MongoTemplate mongoTemplate) {
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
        this.contributionRepository = contributionRepository;
        this.accountService = accountService;
        this.profileService = profileService;
        this.mongoTemplate = mongoTemplate;
    }

    public PageResponse<AdminAlumniResponse> search(String q, String department, int page, int size) {
        List<Criteria> filters = new ArrayList<>();

        String term = TextUtil.clean(q);
        if (term != null) {
            Pattern pattern = InputChecks.containsIgnoreCase(term);
            List<Criteria> anyOf = new ArrayList<>();
            anyOf.add(Criteria.where("fullName").regex(pattern));
            anyOf.add(Criteria.where("company").regex(pattern));
            anyOf.add(Criteria.where("jobTitle").regex(pattern));

            // Email lives on the user, so find matching alumni users first.
            Query userQuery = new Query(Criteria.where("role").is(Role.ALUMNI).and("email").regex(pattern));
            List<String> matchingUserIds = mongoTemplate.find(userQuery, User.class).stream()
                    .map(User::getId)
                    .toList();
            if (!matchingUserIds.isEmpty()) {
                anyOf.add(Criteria.where("userId").in(matchingUserIds));
            }
            filters.add(new Criteria().orOperator(anyOf.toArray(new Criteria[0])));
        }

        String dept = TextUtil.clean(department);
        if (dept != null) {
            filters.add(Criteria.where("department").is(dept));
        }

        Query query = filters.isEmpty()
                ? new Query()
                : new Query(new Criteria().andOperator(filters.toArray(new Criteria[0])));
        Pageable pageable = InputChecks.pageable(page, size, BY_NAME);

        long total = mongoTemplate.count(query, AlumniProfile.class);
        query.with(pageable);
        List<AlumniProfile> profiles = mongoTemplate.find(query, AlumniProfile.class);

        Set<String> userIds = profiles.stream().map(AlumniProfile::getUserId).collect(Collectors.toSet());
        Map<String, String> emails = new LinkedHashMap<>();
        userRepository.findAllById(userIds).forEach(u -> emails.put(u.getId(), u.getEmail()));

        List<AdminAlumniResponse> content = profiles.stream()
                .map(p -> AdminAlumniResponse.of(p, emails.getOrDefault(p.getUserId(), "")))
                .toList();

        return PageResponse.of(content, pageable.getPageNumber(), pageable.getPageSize(), total);
    }

    public AdminAlumniResponse get(String id) {
        AlumniProfile profile = findProfile(id);
        return AdminAlumniResponse.of(profile, emailOf(profile.getUserId()));
    }

    public AdminAlumniResponse create(AdminAlumniRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        InputChecks.checkGraduationYear(request.graduationYear(), errors);
        if (!InputChecks.isPasswordValid(request.temporaryPassword())) {
            errors.put("temporaryPassword", InputChecks.PASSWORD_MESSAGE);
        }
        InputChecks.throwIfAny(errors);

        AlumniProfile profile = new AlumniProfile();
        profileService.applyFields(profile, request.toProfileFields());

        AccountService.CreatedAccount created =
                accountService.createAlumniAccount(request.email(), request.temporaryPassword(), profile);
        return AdminAlumniResponse.of(created.profile(), created.user().getEmail());
    }

    public AdminAlumniResponse update(String id, AdminAlumniRequest request) {
        AlumniProfile profile = findProfile(id);
        User user = userRepository.findById(profile.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Account for this alumnus was not found."));

        // Validate and apply in memory first, so nothing is saved if a rule fails.
        profileService.applyFields(profile, request.toProfileFields());

        if (!request.email().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.email())) {
                throw new DuplicateEmailException();
            }
            user.setEmail(request.email());
            user.setUpdatedAt(Instant.now());
            try {
                user = userRepository.save(user);
            } catch (DuplicateKeyException e) {
                throw new DuplicateEmailException();
            }
        }

        profile = profileRepository.save(profile);
        return AdminAlumniResponse.of(profile, user.getEmail());
    }

    public void delete(String id) {
        AlumniProfile profile = findProfile(id);
        String userId = profile.getUserId();

        User user = userRepository.findById(userId).orElse(null);
        if (user != null && user.getRole() == Role.ADMIN) {
            throw new BadRequestException("Administrator accounts cannot be deleted here.");
        }

        contributionRepository.deleteByUserId(userId);
        profileRepository.deleteById(profile.getId());
        if (user != null) {
            userRepository.deleteById(userId);
        }
    }

    private AlumniProfile findProfile(String id) {
        return profileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alumni record not found."));
    }

    private String emailOf(String userId) {
        return userRepository.findById(userId).map(User::getEmail).orElse("");
    }
}