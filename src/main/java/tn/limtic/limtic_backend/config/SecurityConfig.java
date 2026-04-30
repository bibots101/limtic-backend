package tn.limtic.limtic_backend.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.http.HttpServletResponse;
import tn.limtic.limtic_backend.filter.RateLimitFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final RateLimitFilter rateLimitFilter;

    public SecurityConfig(RateLimitFilter rateLimitFilter) {
        this.rateLimitFilter = rateLimitFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                .ignoringRequestMatchers("/api/auth/login")
                .ignoringRequestMatchers("/api/auth/signup")
                .ignoringRequestMatchers("/api/auth/forgot-password")
                .ignoringRequestMatchers("/api/auth/reset-password")
                .ignoringRequestMatchers("/api/contact")
                .ignoringRequestMatchers("/api/evenements/*/photos")
                .ignoringRequestMatchers("/api/admin/chercheurs/import-csv")
                .ignoringRequestMatchers("/api/publications/*/upload-pdf")
                .ignoringRequestMatchers("/api/admin/parametres/lot")
                .ignoringRequestMatchers("/api/admin/parametres/logo")
                .ignoringRequestMatchers("/api/admin/parametres")
                .ignoringRequestMatchers("/api/admin/parametres/*")
            )

            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .sessionFixation().migrateSession()
                .maximumSessions(1)
            )

            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, e) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"Non authentifié\"}");
                })
                .accessDeniedHandler((request, response, e) -> {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"Accès refusé\"}");
                })
            )

            .authorizeHttpRequests(auth -> auth

                // ── Swagger protégé ADMIN ────────────────────────────────────
                .requestMatchers("/swagger-ui/**", "/api-docs/**", "/swagger-ui.html")
                    .hasAnyRole("ADMIN", "SUPER_ADMIN")

                // ── Paramètres publics ───────────────────────────────────────
                .requestMatchers(HttpMethod.GET, "/api/admin/parametres/public").permitAll()

                // ── Routes publiques en lecture ──────────────────────────────
                .requestMatchers(HttpMethod.GET, "/api/masteriens/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/chercheurs/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/publications/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/evenements/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/outils/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/axes/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/doctorants/**").permitAll()

                // ── Fichiers uploadés ────────────────────────────────────────
                .requestMatchers("/uploads/**").permitAll()

                // ── Authentification ─────────────────────────────────────────
                .requestMatchers("/api/auth/**").permitAll()

                // ── Contact (captcha) ────────────────────────────────────────
                .requestMatchers(HttpMethod.POST, "/api/contact").permitAll()

                // ── Droits exclusifs SUPER_ADMIN ─────────────────────────────
                .requestMatchers("/api/admin/parametres/**", "/api/admin/audit/**", "/api/users/**").hasRole("SUPER_ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/axes/**").hasRole("SUPER_ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/axes/**").hasRole("SUPER_ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/axes/**").hasRole("SUPER_ADMIN")

                // ── Administration ───────────────────────────────────────────
                .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")

                // ── Tout le reste → authentifié ──────────────────────────────
                .anyRequest().authenticated()
            )

            .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:4200", "https://localhost:4200"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of(
            "Content-Type",
            "Accept",
            "X-Requested-With",
            "X-XSRF-TOKEN"
        ));
        config.setExposedHeaders(List.of("X-XSRF-TOKEN", "Content-Disposition"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}