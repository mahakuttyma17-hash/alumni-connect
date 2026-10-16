package com.alumni.alumni_management_system.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import com.alumni.alumni_management_system.dto.DirectoryAlumniResponse;
import com.alumni.alumni_management_system.dto.FiltersResponse;
import com.alumni.alumni_management_system.dto.PageResponse;
import com.alumni.alumni_management_system.dto.TextUtil;
import com.alumni.alumni_management_system.exception.ResourceNotFoundException;
import com.alumni.alumni_management_system.model.AlumniProfile;
import com.alumni.alumni_management_system.repository.AlumniProfileRepository;

@Service
public class AlumniDirectoryService {

    private static final Sort BY_NAME = Sort.by(Sort.Order.asc("fullName"), Sort.Order.asc("id"));

    private final MongoTemplate mongoTemplate;
    private final AlumniProfileRepository profileRepository;

    public AlumniDirectoryService(MongoTemplate mongoTemplate, AlumniProfileRepository profileRepository) {
        this.mongoTemplate = mongoTemplate;
        this.profileRepository = profileRepository;
    }

    public PageResponse<DirectoryAlumniResponse> search(String q, String department, Integer graduationYear,
                                                        int page, int size) {
        List<Criteria> filters = new ArrayList<>();
        filters.add(Criteria.where("showInDirectory").is(true));

        String term = TextUtil.clean(q);
        if (term != null) {
            Pattern pattern = InputChecks.containsIgnoreCase(term);
            List<Criteria> anyOf = new ArrayList<>();
            anyOf.add(Criteria.where("fullName").regex(pattern));
            anyOf.add(Criteria.where("department").regex(pattern));
            anyOf.add(Criteria.where("company").regex(pattern));
            anyOf.add(Criteria.where("jobTitle").regex(pattern));
            if (term.matches("\\d{4}")) {
                anyOf.add(Criteria.where("graduationYear").is(Integer.parseInt(term)));
            }
            filters.add(new Criteria().orOperator(anyOf.toArray(new Criteria[0])));
        }

        String dept = TextUtil.clean(department);
        if (dept != null) {
            filters.add(Criteria.where("department").is(dept));
        }
        if (graduationYear != null) {
            filters.add(Criteria.where("graduationYear").is(graduationYear));
        }

        Query query = new Query(new Criteria().andOperator(filters.toArray(new Criteria[0])));
        Pageable pageable = InputChecks.pageable(page, size, BY_NAME);

        long total = mongoTemplate.count(query, AlumniProfile.class);
        query.with(pageable);
        List<DirectoryAlumniResponse> content = mongoTemplate.find(query, AlumniProfile.class).stream()
                .map(DirectoryAlumniResponse::from)
                .toList();

        return PageResponse.of(content, pageable.getPageNumber(), pageable.getPageSize(), total);
    }

    public FiltersResponse filters() {
        List<String> departments = new ArrayList<>(mongoTemplate.findDistinct(
                visibleQuery(), "department", AlumniProfile.class, String.class));
        List<Integer> years = new ArrayList<>(mongoTemplate.findDistinct(
                visibleQuery(), "graduationYear", AlumniProfile.class, Integer.class));
        departments.removeIf(d -> d == null || d.isBlank());
        years.removeIf(y -> y == null);
        Collections.sort(departments, String.CASE_INSENSITIVE_ORDER);
        years.sort(Collections.reverseOrder());
        return new FiltersResponse(departments, years);
    }

    public DirectoryAlumniResponse getById(String id) {
        return profileRepository.findById(id)
                .filter(AlumniProfile::isShowInDirectory)
                .map(DirectoryAlumniResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Alumni profile not found."));
    }

    private Query visibleQuery() {
        return new Query(Criteria.where("showInDirectory").is(true));
    }
}