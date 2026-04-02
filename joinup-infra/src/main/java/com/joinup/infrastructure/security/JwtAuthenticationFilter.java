package com.joinup.infrastructure.security;

import com.joinup.common.constants.SecurityConstants;
import com.joinup.common.context.LoginUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT 鉴权过滤器。
 * 从请求头提取 Bearer Token，校验后把 LoginUser 放入 SecurityContext。
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader(SecurityConstants.AUTHORIZATION_HEADER);
        if (SecurityContextHolder.getContext().getAuthentication() == null
                && authorization != null
                && authorization.startsWith(SecurityConstants.TOKEN_PREFIX)) {
            // 只有在当前上下文还未认证时才尝试解析，避免重复覆盖已认证用户。
            String token = authorization.substring(SecurityConstants.TOKEN_PREFIX.length());
            if (jwtTokenProvider.validateToken(token)) {
                LoginUser loginUser = jwtTokenProvider.parseLoginUser(token);
                UsernamePasswordAuthenticationToken authenticationToken =
                        new UsernamePasswordAuthenticationToken(
                                loginUser,
                                null,
                                Collections.singletonList(new SimpleGrantedAuthority(SecurityConstants.ROLE_USER)));
                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            }
        }
        filterChain.doFilter(request, response);
    }
}
