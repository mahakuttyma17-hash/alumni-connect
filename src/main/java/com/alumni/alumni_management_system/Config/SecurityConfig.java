package com.alumni.alumni_management_system.Config;

import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

import com.alumni.alumni_management_system.repository.UserRepository;
import com.alumni.alumni_management_system.security.AccountStatusFilter;
import com.alumni.alumni_management_system.security.AppUserDetails;
import com.alumni.alumni_management_system.security.JsonResponses;
import com.alumni.alumni_management_system.service.AuthService;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   AuthService authService,
                                                   UserRepository userRepository) throws Exception {
        http
            // CSRF stays ON. The token is kept in the XSRF-TOKEN cookie, which script.js
            // reads and sends back in the X-XSRF-TOKEN header on every write request.
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))

            .authorizeHttpRequests(auth -> auth
                // Static pages and assets (they contain no data; the APIs are protected below)
                .requestMatchers("/", "/index.html", "/directory.html", "/login.html", "/register.html",
                        "/dashboard.html", "/profile.html", "/contributions.html", "/admin.html",
                        "/style.css", "/script.js", "/icons.svg", "/favicon.ico", "/error").permitAll()
                // Public API
                .requestMatchers("/api/auth/csrf", "/api/auth/register",
                        "/api/auth/login", "/api/auth/logout").permitAll()
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/alumni", "/api/alumni/**").permitAll()
                // Any logged-in user
                .requestMatchers("/api/auth/me").authenticated()
                // Role-restricted API
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/profile/**", "/api/dashboard/**", "/api/contributions/**").hasRole("ALUMNI")
                // Any other /api path is refused
                .requestMatchers("/api/**").denyAll()
                .anyRequest().permitAll())

            .formLogin(form -> form
                .loginPage("/login.html")
                .loginProcessingUrl("/api/auth/login")
                .usernameParameter("email")
                .passwordParameter("password")
                .successHandler((request, response, authentication) -> {
                    AppUserDetails principal = (AppUserDetails) authentication.getPrincipal();
                    JsonResponses.writeUser(response, authService.describe(principal.getId()));
                })
                .failureHandler((request, response, exception) ->
                    JsonResponses.writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized",
                            "Incorrect email or password.", Map.of()))
                .permitAll())

            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .logoutSuccessHandler((request, response, authentication) ->
                    response.setStatus(HttpServletResponse.SC_NO_CONTENT))
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID", "XSRF-TOKEN"))

            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) ->
                    JsonResponses.writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized",
                            "Please sign in to continue.", Map.of()))
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    String message = accessDeniedException instanceof CsrfException
                            ? "Your security token expired. Please try again."
                            : "You do not have permission to do that.";
                    JsonResponses.writeError(response, HttpServletResponse.SC_FORBIDDEN, "Forbidden",
                            message, Map.of());
                }))

            .addFilterBefore(new AccountStatusFilter(userRepository), AnonymousAuthenticationFilter.class);

        return http.build();
    }
}