package com.alumni.alumni_management_system.service;

import java.time.Instant;
import java.util.Locale;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.alumni.alumni_management_system.exception.DuplicateEmailException;
import com.alumni.alumni_management_system.model.AlumniProfile;
import com.alumni.alumni_management_system.model.Role;
import com.alumni.alumni_management_system.model.User;
import com.alumni.alumni_management_system.repository.AlumniProfileRepository;
import com.alumni.alumni_management_system.repository.UserRepository;

@Service
public class AccountService {

    public record CreatedAccount(User user, AlumniProfile profile) {
    }

    private final UserRepository userRepository;
    private final AlumniProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountService(UserRepository userRepository,
                          AlumniProfileRepository profileRepository,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Creates a user (role ALUMNI) and links the given profile to it.
     * The profile must already hold its fields; this method sets userId and timestamps.
     */
    public CreatedAccount createAlumniAccount(String email, String rawPassword, AlumniProfile profile) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateEmailException();
        }

        Instant now = Instant.now();
        User user = new User();
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setRole(Role.ALUMNI);
        user.setEnabled(true);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        try {
            user = userRepository.save(user);
        } catch (DuplicateKeyException e) {
            // Two requests raced: the unique index on email caught the second one.
            throw new DuplicateEmailException();
        }

        try {
            profile.setUserId(user.getId());
            profile.setCreatedAt(now);
            profile.setUpdatedAt(now);
            profile = profileRepository.save(profile);
        } catch (RuntimeException e) {
            // No multi-document transactions on a standalone MongoDB, so undo the user manually.
            userRepository.deleteById(user.getId());
            throw e;
        }

        return new CreatedAccount(user, profile);
    }
}