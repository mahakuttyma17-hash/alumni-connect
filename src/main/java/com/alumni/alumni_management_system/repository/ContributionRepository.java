package com.alumni.alumni_management_system.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.alumni.alumni_management_system.model.Contribution;
import com.alumni.alumni_management_system.model.ContributionStatus;
import com.alumni.alumni_management_system.model.ContributionType;

public interface ContributionRepository extends MongoRepository<Contribution, String> {

    List<Contribution> findByUserId(String userId, Sort sort);

    List<Contribution> findByUserId(String userId, Pageable pageable);

    long countByUserId(String userId);

    long countByType(ContributionType type);

    long countByStatus(ContributionStatus status);

    List<Contribution> findByTypeAndStatus(ContributionType type, ContributionStatus status);

    void deleteByUserId(String userId);
}