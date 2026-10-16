package com.alumni.alumni_management_system.service;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.alumni.alumni_management_system.dto.AuthUserResponse;
import com.alumni.alumni_management_system.dto.RegisterRequest;
import com.alumni.alumni_management_system.exception.ResourceNotFoundException;
import com.alumni.alumni_management_system.model.AlumniProfile;
import com.alumni.alumni_management_system.model.Role;
import com.alumni.alumni_management_system.model.User;
import com.alumni.alumni_management_system.repository.AlumniProfileRepository;
import com.alumni.alumni_management_system.repository.UserRepository;

@Service
public class AuthService {

    private final AccountService accountService;
    private final UserRepository userRepository;
    private final AlumniProfileRepository profileRepository;

    public AuthService(AccountService accountService,
                       UserRepository userRepository,
                       AlumniProfileRepository profileRepository) {
        this.accountService = accountService;
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
    }

    public AuthUserResponse register(RegisterRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (!request.password().equals(request.confirmPassword())) {
            errors.put("confirmPassword", "Passwords do not match.");
        }
        if (!InputChecks.isPasswordValid(request.password())) {
            errors.put("password", InputChecks.PASSWORD_MESSAGE);
        }
        InputChecks.checkGraduationYear(request.graduationYear(), errors);
        InputChecks.throwIfAny(errors);

        AlumniProfile profile = new AlumniProfile();
        profile.setFullName(request.fullName());
        profile.setPhone(request.phone());
        profile.setDepartment(request.department());
        profile.setDegree(request.degree());
        profile.setGraduationYear(request.graduationYear());
        profile.setShowInDirectory(true);

        AccountService.CreatedAccount created =
                accountService.createAlumniAccount(request.email(), request.password(), profile);
        return describe(created.user());
    }

    public AuthUserResponse describe(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found."));
        return describe(user);
    }

    public AuthUserResponse describe(User user) {
        String displayName;
        if (user.getRole() == Role.ADMIN) {
            displayName = "Administrator";
        } else {
            displayName = profileRepository.findByUserId(user.getId())
                    .map(AlumniProfile::getFullName)
                    .orElse(user.getEmail());
        }
        return new AuthUserResponse(
                user.getId(), user.getEmail(), displayName, user.getRole().name(), redirectFor(user.getRole()));
    }

    public static String redirectFor(Role role) {
        return role == Role.ADMIN ? "admin.html" : "dashboard.html";
    }
}