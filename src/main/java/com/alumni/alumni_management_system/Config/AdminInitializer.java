package com.alumni.alumni_management_system.Config;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.alumni.alumni_management_system.dto.ValidationPatterns;
import com.alumni.alumni_management_system.model.Role;
import com.alumni.alumni_management_system.model.User;
import com.alumni.alumni_management_system.repository.UserRepository;

@Component
public class AdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);

    private static final Pattern EMAIL = Pattern.compile(ValidationPatterns.EMAIL);
    private static final Pattern PASSWORD_RULE = Pattern.compile(ValidationPatterns.PASSWORD_RULE);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MongoTemplate mongoTemplate;
    private final String adminEmail;
    private final String adminPassword;

    public AdminInitializer(UserRepository userRepository,
                            PasswordEncoder passwordEncoder,
                            MongoTemplate mongoTemplate,
                            @Value("${app.admin.email:}") String adminEmail,
                            @Value("${app.admin.password:}") String adminPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.mongoTemplate = mongoTemplate;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            mongoTemplate.executeCommand("{ ping: 1 }");
            log.info("MongoDB connection OK. Using database: {}", mongoTemplate.getDb().getName());
        } catch (RuntimeException e) {
            log.error("COULD NOT CONNECT TO MONGODB. Start MongoDB (or fix MONGODB_URI) and restart the app. "
                    + "Reason: {}", e.getMessage());
            return;
        }

        try {
            seedAdmin();
        } catch (RuntimeException e) {
            log.error("Admin account setup failed: {}", e.getMessage());
        }
    }

    private void seedAdmin() {
        String email = adminEmail == null ? "" : adminEmail.trim().toLowerCase(Locale.ROOT);
        boolean noCredentials = email.isEmpty() || adminPassword == null || adminPassword.isEmpty();

        if (noCredentials) {
            boolean anyAdmin = mongoTemplate.exists(Query.query(Criteria.where("role").is(Role.ADMIN)), User.class);
            if (!anyAdmin) {
                log.warn("No administrator exists yet. Set ADMIN_EMAIL and ADMIN_PASSWORD "
                        + "(environment variables or local.properties) and restart to create one.");
            }
            return;
        }

        if (!EMAIL.matcher(email).matches()) {
            log.error("ADMIN_EMAIL is not a valid email address. Administrator was not created.");
            return;
        }
        if (!passwordIsAcceptable(adminPassword)) {
            log.error("ADMIN_PASSWORD must be 8-64 characters with at least one letter and one number. "
                    + "Administrator was not created.");
            return;
        }

        Optional<User> existing = userRepository.findByEmail(email);
        if (existing.isPresent()) {
            if (existing.get().getRole() == Role.ADMIN) {
                log.info("Administrator account {} already exists.", email);
            } else {
                log.warn("{} is already registered as an alumni account, so it was NOT turned into an "
                        + "administrator. Use a different ADMIN_EMAIL.", email);
            }
            return;
        }

        Instant now = Instant.now();
        User admin = new User();
        admin.setEmail(email);
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setRole(Role.ADMIN);
        admin.setEnabled(true);
        admin.setCreatedAt(now);
        admin.setUpdatedAt(now);
        try {
            userRepository.save(admin);
            log.info("Administrator account created for {}.", email);
        } catch (DuplicateKeyException e) {
            log.info("Administrator account {} already exists.", email);
        }
    }

    private boolean passwordIsAcceptable(String password) {
        return password.length() >= 8
                && password.length() <= 64
                && password.getBytes(StandardCharsets.UTF_8).length <= 72
                && PASSWORD_RULE.matcher(password).matches();
    }
}