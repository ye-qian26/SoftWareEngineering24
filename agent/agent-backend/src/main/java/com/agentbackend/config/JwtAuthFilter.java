package com.agentbackend.config;

import com.agentbackend.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthFilter.class);
    
    private static final List<String> PUBLIC_PATHS = List.of(
        "/api/user/register",
        "/api/user/login",
        "/error"
    );
    
    @Autowired
    private JwtUtil jwtUtil;
    
    private boolean isPublicPath(String uri) {
        return PUBLIC_PATHS.stream().anyMatch(uri::startsWith);
    }
    
    private boolean isProtectedPath(String uri) {
        return uri.startsWith("/api/") && !isPublicPath(uri);
    }
    
    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        
        Map<String, Object> body = new HashMap<>();
        body.put("code", 401);
        body.put("message", message);
        body.put("data", null);
        
        new ObjectMapper().writeValue(response.getOutputStream(), body);
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String requestURI = request.getRequestURI();
        String authHeader = request.getHeader("Authorization");
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            
            try {
                if (jwtUtil.validateToken(token)) {
                    Long userId = jwtUtil.getUserIdFromToken(token);
                    String username = jwtUtil.getUsernameFromToken(token);
                    
                    request.setAttribute("userId", userId);
                    request.setAttribute("username", username);
                    
                    UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(
                            userId, 
                            null, 
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                        );
                    
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    logger.debug("JWT认证成功: userId={}, username={}, uri={}", userId, username, requestURI);
                } else {
                    logger.warn("JWT token验证失败: uri={}", requestURI);
                    if (isProtectedPath(requestURI)) {
                        sendUnauthorizedResponse(response, "登录已过期，请重新登录");
                        return;
                    }
                }
            } catch (Exception e) {
                logger.error("JWT token解析异常: uri={}, error={}", requestURI, e.getMessage());
                SecurityContextHolder.clearContext();
                if (isProtectedPath(requestURI)) {
                    sendUnauthorizedResponse(response, "认证失败，请重新登录");
                    return;
                }
            }
        } else {
            if (isProtectedPath(requestURI)) {
                logger.warn("访问受保护资源未携带token: uri={}", requestURI);
                sendUnauthorizedResponse(response, "未登录，请先登录");
                return;
            }
            logger.debug("请求未携带JWT token: uri={}", requestURI);
        }
        
        filterChain.doFilter(request, response);
    }
}
