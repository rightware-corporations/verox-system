package com.rightware.verox.common.config;

import com.rightware.verox.authentication.web.ApiKeyAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.nio.charset.StandardCharsets;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        ApiKeyAuthenticationFilter apiKeyAuthenticationFilter
    ) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, exception) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.getWriter().write(
                        "{\"error\":{\"code\":\"AUTHENTICATION_REQUIRED\",\"message\":\"A valid VEROX API key is required.\"}}"
                    );
                })
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/public/**").permitAll()
                .requestMatchers("/v1/**").authenticated()
                .anyRequest().denyAll()
            )
            .addFilterBefore(apiKeyAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
