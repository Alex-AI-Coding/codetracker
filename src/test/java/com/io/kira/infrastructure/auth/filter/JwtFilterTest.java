package com.io.kira.infrastructure.auth.filter;

import com.io.kira.adapter.auth.out.security.CustomUserDetailsService;
import com.io.kira.adapter.auth.out.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtFilterTest {

    private static final String SIGNING_SECRET =
            "dGVzdC1qd3Qtc2VjcmV0LXRlc3Qtand0LXNlY3JldC10ZXN0LWp3dC1zZWNyZXQ=";
    private static final String DIFFERENT_SIGNING_SECRET =
            "YW5vdGhlci10ZXN0LWp3dC1zZWNyZXQtYW5vdGhlci10ZXN0LWp3dC1zZWNyZXQ=";

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validJwtCookieAuthenticatesAndContinuesTheRequest() throws Exception {
        UUID authId = UUID.randomUUID();
        JwtService jwtService = new JwtService(SIGNING_SECRET, 300_000);
        CustomUserDetailsService userDetailsService = mock(CustomUserDetailsService.class);
        UserDetails userDetails = User.withUsername(authId.toString())
                .password("unused")
                .authorities("ROLE_PROFESSOR")
                .build();
        when(userDetailsService.loadUserByUsername(authId.toString())).thenReturn(userDetails);

        JwtFilter filter = new JwtFilter(jwtService, userDetailsService);
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletRequest request = protectedRequest(jwtService.generateToken(authId));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(response.getHeader(JwtFilter.AUTH_FAILURE_HEADER)).isNull();
    }

    @Test
    void expiredJwtCookieReturnsSafeDiagnosticWithoutCallingTheApplication() throws Exception {
        UUID authId = UUID.randomUUID();
        JwtService expiredTokenService = new JwtService(SIGNING_SECRET, -1_000);
        CustomUserDetailsService userDetailsService = mock(CustomUserDetailsService.class);
        JwtFilter filter = new JwtFilter(expiredTokenService, userDetailsService);
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletRequest request = protectedRequest(expiredTokenService.generateToken(authId));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getHeader(JwtFilter.AUTH_FAILURE_HEADER)).isEqualTo("token-expired");
        assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store");
        verify(chain, never()).doFilter(request, response);
        verify(userDetailsService, never()).loadUserByUsername(authId.toString());
    }

    @Test
    void incorrectlySignedJwtReturnsSafeDiagnosticWithoutCallingTheApplication() throws Exception {
        UUID authId = UUID.randomUUID();
        JwtService issuer = new JwtService(DIFFERENT_SIGNING_SECRET, 300_000);
        JwtService validator = new JwtService(SIGNING_SECRET, 300_000);
        CustomUserDetailsService userDetailsService = mock(CustomUserDetailsService.class);
        JwtFilter filter = new JwtFilter(validator, userDetailsService);
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletRequest request = protectedRequest(issuer.generateToken(authId));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getHeader(JwtFilter.AUTH_FAILURE_HEADER)).isEqualTo("token-invalid");
        assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store");
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void refreshEndpointStillBypassesJwtValidation() throws Exception {
        JwtService jwtService = new JwtService(SIGNING_SECRET, 300_000);
        CustomUserDetailsService userDetailsService = mock(CustomUserDetailsService.class);
        JwtFilter filter = new JwtFilter(jwtService, userDetailsService);
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/refresh");
        request.setServletPath("/api/auth/refresh");
        request.setCookies(new Cookie("jwt", "expired-or-malformed"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(response.getHeader(JwtFilter.AUTH_FAILURE_HEADER)).isNull();
    }

    private MockHttpServletRequest protectedRequest(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/classrooms/test/activities/owner");
        request.setServletPath("/api/classrooms/test/activities/owner");
        request.setCookies(new Cookie("jwt", token));
        return request;
    }
}
