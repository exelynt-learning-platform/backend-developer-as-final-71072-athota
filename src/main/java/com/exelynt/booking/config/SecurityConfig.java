package com.exelynt.booking.config;

import com.exelynt.booking.security.JwtAuthenticationFilter;
import com.exelynt.booking.security.SecurityErrorResponseWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final SecurityErrorResponseWriter securityErrorResponseWriter;
    private final boolean h2ConsoleEnabled;
    private final List<String> allowedOrigins;

    @Autowired
    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          SecurityErrorResponseWriter securityErrorResponseWriter,
                          @Value("${app.h2-console.enabled:false}") boolean h2ConsoleEnabled,
                          @Value("${app.cors.allowed-origins:http://localhost:3000}") List<String> allowedOrigins) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.securityErrorResponseWriter = securityErrorResponseWriter;
        this.h2ConsoleEnabled = h2ConsoleEnabled;
        this.allowedOrigins = List.copyOf(allowedOrigins);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                // ── CSRF DESIGN DECISION ─────────────────────────────────────────────────
                // CSRF attacks exploit the browser's automatic inclusion of session cookies
                // on cross-origin requests. This API issues NO cookies of any kind:
                //   • Authentication is performed solely via Authorization: Bearer <JWT>
                //   • Sessions are stateless (SessionCreationPolicy.STATELESS)
                //   • No Set-Cookie headers are ever returned
                //
                // Because there is no browser-managed credential for an attacker to replay,
                // CSRF protection is architecturally inapplicable here. Disabling it also
                // allows standard REST clients (curl, Postman, fetch with Authorization header)
                // to interact without needing to obtain and forward a CSRF token.
                //
                // Compensating controls in place:
                //   • All state-changing endpoints require a valid, short-lived JWT.
                //   • CORS is restricted to configured origins (CORS_ALLOWED_ORIGINS env var).
                //   • If cookie-based auth is ever introduced, CSRF MUST be re-enabled.
                // ─────────────────────────────────────────────────────────────────────────
                .csrf(AbstractHttpConfigurer::disable)
                .headers(headers -> {
                    if (h2ConsoleEnabled) {
                        headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin);
                    }
                })
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) ->
                                securityErrorResponseWriter.write(
                                        request, response, HttpStatus.UNAUTHORIZED, "Authentication is required"))
                        .accessDeniedHandler((request, response, exception) ->
                                securityErrorResponseWriter.write(
                                        request, response, HttpStatus.FORBIDDEN, "Access denied")))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                            .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html", "/api-docs/**").permitAll();
                    if (h2ConsoleEnabled) {
                        auth.requestMatchers("/h2-console/**").permitAll();
                    }
                    auth.requestMatchers(HttpMethod.GET, "/resources/**").authenticated()
                            .requestMatchers(HttpMethod.POST, "/resources/**").hasAuthority("ROLE_ADMIN")
                            .requestMatchers(HttpMethod.PUT, "/resources/**").hasAuthority("ROLE_ADMIN")
                            .requestMatchers(HttpMethod.DELETE, "/resources/**").hasAuthority("ROLE_ADMIN")
                            .requestMatchers("/reservations/**").authenticated()
                            .anyRequest().authenticated();
                });

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Auth-Token", "Accept"));
        configuration.setExposedHeaders(List.of("Authorization", "X-Auth-Token"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
