package com.alumni.alumni_management_system.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.alumni.alumni_management_system.model.AlumniProfile;

public interface AlumniProfileRepository extends MongoRepository<AlumniProfile, String> {

    Optional<AlumniProfile> findByUserId(String userId);

    boolean existsByUserId(String userId);

    List<AlumniProfile> findByUserIdIn(Collection<String> userIds);

    long countByShowInDirectoryTrue();
}