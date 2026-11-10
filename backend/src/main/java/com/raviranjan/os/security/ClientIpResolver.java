package com.raviranjan.os.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.util.*;

@Component
public class ClientIpResolver {

    private final boolean trustInternalProxies;
    private final Set<String> explicitTrustedProxies;

    public ClientIpResolver(
            @Value("${app.security.proxy.trust-internal-proxies:true}") boolean trustInternalProxies,
            @Value("${app.security.proxy.trusted-ips:}") String trustedIpsConfig
    ) {
        this.trustInternalProxies = trustInternalProxies;
        Set<String> set = new HashSet<>();
        if (trustedIpsConfig != null && !trustedIpsConfig.isBlank()) {
            for (String token : trustedIpsConfig.split(",")) {
                String trimmed = token.trim();
                if (!trimmed.isEmpty()) {
                    String norm = normalizeIp(trimmed);
                    if (norm != null) set.add(norm);
                }
            }
        }
        this.explicitTrustedProxies = Collections.unmodifiableSet(set);
    }

    public String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String remoteAddr = normalizeIp(request.getRemoteAddr());
        if (remoteAddr == null) {
            return null;
        }

        if (!isTrustedProxy(remoteAddr)) {
            return remoteAddr;
        }

        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            String[] hops = xff.split(",");
            List<String> validHops = new ArrayList<>();
            for (String rawHop : hops) {
                String cleanHop = normalizeIp(rawHop);
                if (cleanHop != null) {
                    validHops.add(cleanHop);
                }
            }

            for (int i = validHops.size() - 1; i >= 0; i--) {
                String candidate = validHops.get(i);
                if (!isTrustedProxy(candidate)) {
                    return candidate;
                }
            }

            if (!validHops.isEmpty()) {
                return validHops.get(0);
            }
        }

        String xRealIp = normalizeIp(request.getHeader("X-Real-IP"));
        if (xRealIp != null && !isDockerBridgeAddress(xRealIp)) {
            return xRealIp;
        }
        if (xRealIp != null) {
            return xRealIp;
        }

        return remoteAddr;
    }

    public boolean isTrustedProxy(String ip) {
        String normalized = normalizeIp(ip);
        if (normalized == null) {
            return false;
        }
        if (explicitTrustedProxies.contains(normalized)) {
            return true;
        }
        if (!trustInternalProxies) {
            return false;
        }
        return isLoopbackOrPrivateAddress(normalized);
    }

    public boolean isPrivateOrLoopback(String ip) {
        String normalized = normalizeIp(ip);
        if (normalized == null) {
            return true;
        }
        return isLoopbackOrPrivateAddress(normalized);
    }

    public String normalizeIp(String rawIp) {
        if (rawIp == null || rawIp.isBlank()) {
            return null;
        }
        String candidate = rawIp.trim();

        if (candidate.startsWith("\"") && candidate.endsWith("\"") && candidate.length() > 2) {
            candidate = candidate.substring(1, candidate.length() - 1).trim();
        }

        if (candidate.toLowerCase(Locale.ROOT).startsWith("::ffff:")) {
            String ipv4Part = candidate.substring(7);
            if (ipv4Part.contains(".")) {
                candidate = ipv4Part;
            }
        }

        if (candidate.startsWith("[")) {
            int closeBracket = candidate.indexOf(']');
            if (closeBracket > 1) {
                candidate = candidate.substring(1, closeBracket);
            } else {
                return null;
            }
        } else if (candidate.indexOf(':') != -1 && candidate.indexOf(':') == candidate.lastIndexOf(':') && candidate.contains(".")) {
            candidate = candidate.substring(0, candidate.indexOf(':'));
        }

        int percentIdx = candidate.indexOf('%');
        if (percentIdx > 0) {
            candidate = candidate.substring(0, percentIdx);
        }

        if (!looksLikeIpLiteral(candidate)) {
            return null;
        }

        try {
            InetAddress addr = InetAddress.getByName(candidate);
            if (addr.isLoopbackAddress()) {
                return candidate.contains(":") ? "::1" : "127.0.0.1";
            }
            return addr.getHostAddress();
        } catch (Exception e) {
            return null;
        }
    }

    private boolean looksLikeIpLiteral(String s) {
        if (s == null || s.isEmpty() || s.length() > 45) {
            return false;
        }
        boolean hasColon = s.indexOf(':') >= 0;
        boolean hasDot = s.indexOf('.') >= 0;
        if (!hasColon && !hasDot) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            boolean validChar = (c >= '0' && c <= '9')
                    || (c >= 'a' && c <= 'f')
                    || (c >= 'A' && c <= 'F')
                    || c == '.' || c == ':';
            if (!validChar) {
                return false;
            }
        }
        return true;
    }

    private boolean isLoopbackOrPrivateAddress(String normalizedIp) {
        try {
            InetAddress addr = InetAddress.getByName(normalizedIp);
            if (addr.isLoopbackAddress() || addr.isSiteLocalAddress() || addr.isLinkLocalAddress() || addr.isAnyLocalAddress()) {
                return true;
            }
            byte[] bytes = addr.getAddress();
            if (bytes.length == 16 && ((bytes[0] & 0xfe) == 0xfc)) {
                return true;
            }
            if (bytes.length == 4 && (bytes[0] & 0xFF) == 100 && ((bytes[1] & 0xC0) == 64)) {
                return true;
            }
            return false;
        } catch (Exception e) {
            return true;
        }
    }

    private boolean isDockerBridgeAddress(String normalizedIp) {
        return normalizedIp != null && (normalizedIp.startsWith("172.17.") || normalizedIp.startsWith("172.18.") || normalizedIp.startsWith("172.19."));
    }
}
