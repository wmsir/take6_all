package com.example.take6server.security.jwt;

import com.example.take6server.security.services.UserDetailsServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public class AuthTokenFilter extends OncePerRequestFilter {
    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    private static final Logger logger = LoggerFactory.getLogger(AuthTokenFilter.class);

    private static final String[] AUTH_WHITELIST = {
            "/api/auth/**",
            "/test/**"
    };

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            // 白名单路径直接放行
            if (isWhiteList(request)) {
                logger.info("请求 {} 在白名单中，直接放行", request.getServletPath());
                filterChain.doFilter(request, response);
                return;
            }

            String jwt = parseJwt(request);
            if (jwt != null && jwtUtils.validateJwtToken(jwt)) {
                String username = jwtUtils.getUserNameFromJwtToken(jwt);

                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            logger.error("无法设置用户认证: {}", e);
        }

        filterChain.doFilter(request, response);
    }

    private boolean isWhiteList(HttpServletRequest request) {
        // 特殊处理：验证接口需要鉴权，不走白名单
        if ("/api/auth/validate".equals(request.getServletPath())) {
            return false;
        }
        for (String path : AUTH_WHITELIST) {
            AntPathMatcher matcher = new AntPathMatcher();
            if (matcher.match(path, request.getServletPath())) {
                return true;
            }
        }
        return false;
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");

        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }

        // 尝试获取自定义 header "auth"
        String customAuth = request.getHeader("auth");
        if (StringUtils.hasText(customAuth)) {
            // 如果 customAuth 以 Bearer 开头，也去掉它，虽然小程序日志显示似乎没有 Bearer
            if (customAuth.startsWith("Bearer ")) {
                return customAuth.substring(7);
            }
            return customAuth;
        }

        return null;
    }
}
