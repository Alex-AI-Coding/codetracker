package com.io.kira.infrastructure.auth.filter;

import com.io.kira.adapter.auth.out.security.CustomUserDetailsService;
import com.io.kira.adapter.auth.out.service.JwtService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@Component
public class JwtFilter extends OncePerRequestFilter {

    public static final String AUTH_FAILURE_HEADER = "X-CodeTracker-Auth-Failure";

    private static final Logger LOGGER = LoggerFactory.getLogger(JwtFilter.class);

    private final JwtService jwtService;
    private final CustomUserDetailsService customUserDetailsService;

    public JwtFilter (JwtService jwtService, CustomUserDetailsService customUserDetailsService) {
        this.jwtService = jwtService;
        this.customUserDetailsService = customUserDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String token = extractToken(request);

        if (token == null || token.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String authId = jwtService.extractAuthId(token);

            if (authId == null || authId.isBlank()) {
                rejectUnauthorized(request, response, "token-invalid");
                return;
            }

            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = customUserDetailsService.loadUserByUsername(authId);

                if (!jwtService.isTokenValid(token, userDetails)) {
                    rejectUnauthorized(request, response, "token-rejected");
                    return;
                }

                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authenticationToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            }
        }
        catch (ExpiredJwtException e) {
            rejectUnauthorized(request, response, "token-expired");
            return;
        }
        catch (JwtException e) {
            rejectUnauthorized(request, response, "token-invalid");
            return;
        }
        catch (UsernameNotFoundException e) {
            rejectUnauthorized(request, response, "account-not-found");
            return;
        }
        catch (Exception e) {
            LOGGER.error(
                    "Unexpected JWT authentication failure: type={} method={} path={}",
                    e.getClass().getSimpleName(),
                    request.getMethod(),
                    request.getRequestURI()
            );
            response.setHeader(AUTH_FAILURE_HEADER, "internal-error");
            response.setHeader("Cache-Control", "no-store");
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        filterChain.doFilter(request,response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getServletPath().equals("/api/auth/refresh");
    }

    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String bearerToken = authHeader.substring(7).trim();
            if (!bearerToken.isEmpty()) {
                return bearerToken;
            }
        }

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("jwt".equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                    return cookie.getValue();
                }
            }
        }

        // Some servlet containers/proxies may leave getCookies() empty even
        // though the original Cookie header is present. JWTs contain no
        // semicolons, so parsing only the named segment is safe here.
        String rawCookieHeader = request.getHeader("Cookie");
        if (rawCookieHeader == null || rawCookieHeader.isBlank()) {
            return null;
        }

        for (String segment : rawCookieHeader.split(";")) {
            int separator = segment.indexOf('=');
            if (separator <= 0) {
                continue;
            }

            String name = segment.substring(0, separator).trim();
            String value = segment.substring(separator + 1).trim();
            if ("jwt".equals(name) && !value.isBlank()) {
                return value;
            }
        }

        return null;
    }

    private void rejectUnauthorized(
            HttpServletRequest request,
            HttpServletResponse response,
            String reason
    ) {
        // Never log the JWT or cookie value. The category and request path are
        // sufficient to diagnose production failures without exposing secrets.
        LOGGER.warn(
                "JWT authentication rejected: reason={} method={} path={}",
                reason,
                request.getMethod(),
                request.getRequestURI()
        );
        response.setHeader(AUTH_FAILURE_HEADER, reason);
        response.setHeader("Cache-Control", "no-store");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

}
