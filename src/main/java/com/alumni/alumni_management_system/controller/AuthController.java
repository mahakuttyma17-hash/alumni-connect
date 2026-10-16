package com.alumni.alumni_management_system.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alumni.alumni_management_system.dto.AuthUserResponse;
import com.alumni.alumni_management_system.dto.RegisterRequest;
import com.alumni.alumni_management_system.security.AppUserDetails;
import com.alumni.alumni_management_system.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** Reading the token makes Spring set the XSRF-TOKEN cookie if the browser does not have one. */
    @GetMapping("/csrf")
    public ResponseEntity<Void> csrf(HttpServletRequest request) {
        Object token = request.getAttribute(CsrfToken.class.getName());
        if (token instanceof CsrfToken csrfToken) {
            csrfToken.getToken();
        }
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/register")
    public ResponseEntity<AuthUserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @GetMapping("/me")
    public AuthUserResponse me(@AuthenticationPrincipal AppUserDetails user) {
        return authService.describe(user.getId());
    }
}