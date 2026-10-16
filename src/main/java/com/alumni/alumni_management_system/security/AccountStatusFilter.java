package com.alumni.alumni_management_system.security;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.alumni.alumni_management_system.model.User;
import com.alumni.alumni_management_system.repository.UserRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/** Not a Spring bean on purpose: SecurityConfig adds it to the security chain only. */
public class AccountStatusFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;

    public AccountStatusFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AppUserDetails principal) {
            boolean stillValid;
            try {
                stillValid = userRepository.findById(principal.getId())
                        .filter(User::isEnabled)
                        .filter(u -> u.getRole() == principal.getRole())
                        .isPresent();
            } catch (RuntimeException e) {
                stillValid = true; // database unreachable: let the request fail visibly further on
            }
            if (!stillValid) {
                SecurityContextHolder.clearContext();
                HttpSession session = request.getSession(false);
                if (session != null) {
                    session.invalidate();
                }
            }
        }
        chain.doFilter(request, response);
    }
}