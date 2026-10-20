package com.muzaffar.smartlead.config;

import com.muzaffar.smartlead.exceptions.enums.ErrorConstant;
import com.muzaffar.smartlead.model.BaseResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    public static final String TEST_KEY_HEADER = "X-TEST-KEY";

    private static final String[] SWAGGER_PATHS = {"/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**"};

    /** Swagger UI and the OpenAPI docs: HTTP Basic with the spring.security.user credentials. */
    @Bean
    @Order(1)
    SecurityFilterChain swaggerFilterChain(HttpSecurity http) {
        return http
                .securityMatcher(SWAGGER_PATHS)
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .build();
    }

    /** Everything else needs a valid X-API-KEY header, except the health probe. */
    @Bean
    @Order(2)
    SecurityFilterChain apiFilterChain(HttpSecurity http, JsonMapper jsonMapper,
                                       @Value("${security.api-key}") String apiKey,
                                       @Value("${security.test-key:}") String testKey) {
        return http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health/**").permitAll()
                        .requestMatchers("/api/v1/test/**").access((authentication, context) ->
                                new AuthorizationDecision(isApiClient(authentication.get())
                                        && matches(testKey, context.getRequest().getHeader(TEST_KEY_HEADER))))
                        .anyRequest().authenticated())
                .addFilterBefore(new ApiKeyAuthFilter(apiKey), AnonymousAuthenticationFilter.class)
                .exceptionHandling(ex -> ex.authenticationEntryPoint((request, response, e) -> {
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    jsonMapper.writeValue(response.getOutputStream(),
                            BaseResponse.error(ErrorConstant.UNAUTHORIZED.getCode(),
                                    "Missing or invalid " + ApiKeyAuthFilter.HEADER + " header"));
                }))
                .exceptionHandling(ex -> ex.accessDeniedHandler((request, response, e) -> {
                    response.setStatus(HttpStatus.FORBIDDEN.value());
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    jsonMapper.writeValue(response.getOutputStream(),
                            BaseResponse.error(ErrorConstant.UNAUTHORIZED.getCode(),
                                    "Missing or invalid " + TEST_KEY_HEADER + " header"));
                }))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .build();
    }

    private static boolean isApiClient(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    /** A blank configured key disables the endpoint: nothing matches it. */
    private static boolean matches(String expected, String provided) {
        return !expected.isBlank() && provided != null
                && MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), provided.getBytes(StandardCharsets.UTF_8));
    }
}
