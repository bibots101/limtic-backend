package tn.limtic.limtic_backend.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;

import jakarta.servlet.http.HttpServletResponse;
import tn.limtic.limtic_backend.filter.RateLimitFilter;

@EnableWebSecurity
@EnableMethodSecurity
@Configuration
public class SecurityConfig {

    private final RateLimitFilter rateLimitFilter;

    @Value("${app.rate-limit.enabled:true}")
    private boolean rateLimitEnabled;

    public SecurityConfig(ObjectProvider<RateLimitFilter> rateLimitFilterProvider) {
        this.rateLimitFilter = rateLimitFilterProvider.getIfAvailable();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                .ignoringRequestMatchers("/api/auth/**")
                .ignoringRequestMatchers("/api/contact")
                .ignoringRequestMatchers("/api/evenements/*/photos")
                .ignoringRequestMatchers("/api/admin/chercheurs/import-csv")
                .ignoringRequestMatchers("/api/publications/*/upload-pdf")
                .ignoringRequestMatchers("/api/admin/parametres/lot")
                .ignoringRequestMatchers("/api/admin/parametres/logo")
                .ignoringRequestMatchers("/api/admin/parametres")
                .ignoringRequestMatchers("/api/admin/parametres/*")
                .ignoringRequestMatchers("/api/doctorants/*/photo")
                .ignoringRequestMatchers("/api/masteriens/*/photo")
                .ignoringRequestMatchers("/api/chercheurs/*/photo")
                .ignoringRequestMatchers("/api/directeur/photo")
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
                    .hasRole("ADMIN")

                // ── Paramètres publics ───────────────────────────────────────
                .requestMatchers(HttpMethod.GET, "/api/admin/parametres/public").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/admin/parametres", "/api/admin/parametres/*").hasAnyRole("ADMIN", "SUPER_ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/admin/parametres/lot").hasAnyRole("ADMIN", "SUPER_ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/admin/parametres/logo").hasAnyRole("ADMIN", "SUPER_ADMIN")

                // ── Routes publiques en lecture ──────────────────────────────
                .requestMatchers(HttpMethod.GET,
                    "/api/masteriens", "/api/masteriens/**",
                    "/api/chercheurs", "/api/chercheurs/**",
                    "/api/publications", "/api/publications/**",
                    "/api/evenements", "/api/evenements/**",
                    "/api/outils", "/api/outils/**",
                    "/api/axes", "/api/axes/**",
                    "/api/doctorants", "/api/doctorants/**"
                ).permitAll()

                // Allow the error page to be accessed without authentication so
                // internal exceptions forwarded there don't trigger a 401.
                .requestMatchers("/error", "/error/**").permitAll()

                // ── Fichiers uploadés ────────────────────────────────────────
                .requestMatchers("/uploads/**").permitAll()

                // ── Authentification ─────────────────────────────────────────
                .requestMatchers("/api/auth/**").permitAll()

                // ── Contact (captcha) ────────────────────────────────────────
                .requestMatchers(HttpMethod.POST, "/api/contact").permitAll()

                // ── Droits exclusifs SUPER_ADMIN ─────────────────────────────
                .requestMatchers("/api/admin/parametres/**", "/api/users/**").hasRole("SUPER_ADMIN")
                // ── Journal d'audit : ADMIN, SUPER_ADMIN et CHERCHEUR ────────
                .requestMatchers(HttpMethod.GET, "/api/admin/audit/**").hasAnyRole("ADMIN", "SUPER_ADMIN", "CHERCHEUR")
                .requestMatchers(HttpMethod.POST, "/api/axes/**").hasRole("SUPER_ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/axes/**").hasRole("SUPER_ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/axes/**").hasRole("SUPER_ADMIN")

                // ── Administration ───────────────────────────────────────────
                .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")

                // ── Tout le reste → authentifié ──────────────────────────────
                .anyRequest().authenticated()
            );

        if (rateLimitEnabled && rateLimitFilter != null) {
            http.addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class);
        }

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
            "http://localhost:4200", 
            "https://localhost:4200",
            "https://limtic-frontend.vercel.app"
        ));
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

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return web -> web.ignoring().requestMatchers(HttpMethod.GET,
            "/api/masteriens", "/api/masteriens/**",
            "/api/chercheurs", "/api/chercheurs/**",
            "/api/publications", "/api/publications/**",
            "/api/evenements", "/api/evenements/**",
            "/api/outils", "/api/outils/**",
            "/api/axes", "/api/axes/**",
            "/api/doctorants", "/api/doctorants/**"
        );
    }
}
