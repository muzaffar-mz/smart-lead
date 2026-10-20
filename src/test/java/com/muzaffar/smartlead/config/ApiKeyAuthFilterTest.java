package com.muzaffar.smartlead.config;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ApiKeyAuthFilterTest {

    private final ApiKeyAuthFilter filter = new ApiKeyAuthFilter("secret");
    private final FilterChain chain = mock(FilterChain.class);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void run(String headerValue) throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/v1/messages");
        if (headerValue != null) {
            request.addHeader(ApiKeyAuthFilter.HEADER, headerValue);
        }
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
    }

    @Test
    void authenticatesWhenTheKeyMatches() throws Exception {
        run("secret");

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.isAuthenticated()).isTrue();
    }

    @Test
    void staysAnonymousWhenTheKeyIsWrong() throws Exception {
        run("not-the-secret");

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void staysAnonymousWhenTheHeaderIsMissing() throws Exception {
        run(null);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doesNotAcceptAPrefixOfTheKey() throws Exception {
        run("secre");

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
