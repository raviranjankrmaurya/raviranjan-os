package com.raviranjan.os.controller;

import com.raviranjan.os.common.ApiResponse;
import com.raviranjan.os.dto.ai.AiChatRequest;
import com.raviranjan.os.dto.ai.AiChatResponse;
import com.raviranjan.os.service.AiAssistantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@Tag(name = "Ask AI Assistant", description = "Retrieval-Augmented Generation assistant answering visitor portfolio queries in English, Hindi, and Hinglish")
public class PublicAiController {

    private final AiAssistantService aiService;

    public PublicAiController(AiAssistantService aiService) {
        this.aiService = aiService;
    }

    @PostMapping({"/chat", "/ask"})
    @Operation(summary = "Ask a question about Raviranjan's portfolio, projects, architecture, and experience")
    public ResponseEntity<ApiResponse<AiChatResponse>> chat(
            @Valid @RequestBody AiChatRequest request,
            HttpServletRequest httpServletRequest
    ) {
        String clientIp = extractClientIp(httpServletRequest, request);
        String userAgent = httpServletRequest.getHeader("User-Agent");
        if (userAgent == null || userAgent.isBlank()) {
            userAgent = "Web Client";
        }

        if (request.getPageUrl() == null || request.getPageUrl().isBlank()) {
            String ref = httpServletRequest.getHeader("Referer");
            request.setPageUrl(ref != null && !ref.isBlank() ? ref : "/");
        }
        if (request.getOrigin() == null || request.getOrigin().isBlank()) {
            String originHdr = httpServletRequest.getHeader("Origin");
            if (originHdr == null || originHdr.isBlank()) {
                originHdr = httpServletRequest.getHeader("Referer");
            }
            request.setOrigin(originHdr != null && !originHdr.isBlank() ? originHdr : "https://raviranjan.dev");
        }
        if (request.getReferrer() == null || request.getReferrer().isBlank()) {
            String ref = httpServletRequest.getHeader("Referer");
            request.setReferrer(ref != null && !ref.isBlank() ? ref : request.getPageUrl());
        }
        if (request.getBrowserHash() == null || request.getBrowserHash().isBlank()) {
            String bHash = httpServletRequest.getHeader("X-Browser-Hash");
            if (bHash != null && !bHash.isBlank()) {
                request.setBrowserHash(bHash);
            }
        }
        if (request.getCountry() == null || request.getCountry().isBlank()) {
            String c = httpServletRequest.getHeader("X-Visitor-Country");
            if (c != null && !c.isBlank()) request.setCountry(c);
        }
        if (request.getRegion() == null || request.getRegion().isBlank()) {
            String r = httpServletRequest.getHeader("X-Visitor-Region");
            if (r != null && !r.isBlank()) request.setRegion(r);
        }
        if (request.getCity() == null || request.getCity().isBlank()) {
            String ci = httpServletRequest.getHeader("X-Visitor-City");
            if (ci != null && !ci.isBlank()) request.setCity(ci);
        }

        AiChatResponse response = aiService.ask(request, clientIp, userAgent);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private String extractClientIp(HttpServletRequest req, AiChatRequest chatRequest) {
        if (chatRequest != null && chatRequest.getClientIp() != null && !chatRequest.getClientIp().isBlank()) {
            String candidate = chatRequest.getClientIp().trim();
            if (!isPrivateOrLocal(candidate)) {
                return candidate;
            }
        }

        String visitorHdr = req.getHeader("X-Visitor-Public-Ip");
        if (visitorHdr != null && !visitorHdr.isBlank()) {
            String candidate = visitorHdr.trim();
            if (!isPrivateOrLocal(candidate)) {
                return candidate;
            }
        }

        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            String[] parts = xff.split(",");
            for (String p : parts) {
                String candidate = p.trim();
                if (!isPrivateOrLocal(candidate)) {
                    return candidate;
                }
            }
            return parts[0].trim();
        }

        String realIp = req.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            String candidate = realIp.trim();
            if (!isPrivateOrLocal(candidate)) {
                return candidate;
            }
            return candidate;
        }

        String cfIp = req.getHeader("CF-Connecting-IP");
        if (cfIp != null && !cfIp.isBlank()) {
            return cfIp.trim();
        }

        if (chatRequest != null && chatRequest.getClientIp() != null && !chatRequest.getClientIp().isBlank()) {
            return chatRequest.getClientIp().trim();
        }

        String remote = req.getRemoteAddr();
        return remote != null ? remote : "127.0.0.1";
    }

    private boolean isPrivateOrLocal(String ip) {
        if (ip == null || ip.isBlank()) return true;
        String clean = ip.replace("::ffff:", "").trim();
        return clean.equals("127.0.0.1") || clean.equals("::1") || clean.equalsIgnoreCase("localhost")
                || clean.startsWith("10.") || clean.startsWith("192.168.")
                || clean.startsWith("172.16.") || clean.startsWith("172.17.")
                || clean.startsWith("172.18.") || clean.startsWith("172.19.")
                || clean.startsWith("172.2") || clean.startsWith("172.3");
    }
}
