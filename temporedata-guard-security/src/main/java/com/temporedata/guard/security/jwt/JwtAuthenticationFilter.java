package com.temporedata.guard.security.jwt;

import com.temporedata.guard.security.LoginUser;
import com.temporedata.guard.security.context.TenantContext;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static com.temporedata.guard.api.base.constants.HttpConstants.AUTH_HEADER;
import static com.temporedata.guard.api.base.constants.HttpConstants.TOKEN_PREFIX;

/**
 * Reads the JWT from the Authorization header, validates it and sets the
 * SecurityContext + tenant context for the current request.
 * V2 优化：直接从 claims 构建认证对象，每请求免查库。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String token = resolveToken(request);
        if (StringUtils.hasText(token) && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                Claims claims = tokenProvider.parseToken(token);
                String userId = claims.getSubject();
                String username = claims.get("username", String.class);
                String tenantId = claims.get("tenantId", String.class);
                String density = claims.get("density", String.class);
                Set<String> roles = parseRoles(claims.get("roles", String.class));

                LoginUser loginUser = new LoginUser(userId, username, null, tenantId, density, roles, true);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
                TenantContext.setTenantId(tenantId);
            } catch (Exception e) {
                // invalid / expired token -> leave unauthenticated
                log.warn("JWT authentication failed: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }
        try {
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private Set<String> parseRoles(String roles) {
        if (!StringUtils.hasText(roles)) return Set.of();
        return Arrays.stream(roles.split(","))
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader(AUTH_HEADER);
        if (StringUtils.hasText(bearer) && bearer.startsWith(TOKEN_PREFIX)) {
            return bearer.substring(TOKEN_PREFIX.length());
        }
        return null;
    }
}