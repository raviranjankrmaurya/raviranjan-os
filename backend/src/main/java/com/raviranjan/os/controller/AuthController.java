package com.raviranjan.os.controller;

import com.raviranjan.os.common.ApiResponse;
import com.raviranjan.os.dto.auth.AuthResponse;
import com.raviranjan.os.dto.auth.LoginRequest;
import com.raviranjan.os.dto.auth.PasswordChangeRequest;
import com.raviranjan.os.dto.auth.TokenRefreshRequest;
import com.raviranjan.os.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Admin authentication, database OTP verification, and credential lifecycle endpoints")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate admin with username and password")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Authentication successful", response));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh expired JWT access token using refresh token")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody TokenRefreshRequest request) {
        AuthResponse response = authService.refreshToken(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", response));
    }


    @GetMapping("/credential-metadata")
    @Operation(summary = "Get admin credential update timestamp and verification metadata from Neon DB")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCredentialMetadata() {
        return ResponseEntity.ok(ApiResponse.success(authService.getCredentialMetadata()));
    }

    @PostMapping("/request-otp")
    @Operation(summary = "Generate and store a 6-digit OTP in Neon DB after verifying admin email & mobile number")
    public ResponseEntity<ApiResponse<Map<String, Object>>> requestOtp(@RequestBody Map<String, String> body) {
        String email = body.getOrDefault("email", "");
        String mobileNumber = body.getOrDefault("mobileNumber", "");
        String purpose = body.getOrDefault("purpose", "UPDATE_CREDENTIALS");
        Map<String, Object> result = authService.requestCredentialOtp(email, mobileNumber, purpose);
        return ResponseEntity.ok(ApiResponse.success("OTP generated and saved in Neon DB", result));
    }

    @PostMapping("/reset-credentials")
    @Operation(summary = "Verify database OTP and update admin username and password in Neon DB")
    public ResponseEntity<ApiResponse<Map<String, Object>>> resetCredentialsWithOtp(@RequestBody Map<String, String> body) {
        String email = body.getOrDefault("email", "");
        String mobileNumber = body.getOrDefault("mobileNumber", "");
        String otpCode = body.getOrDefault("otpCode", "");
        String newUsername = body.getOrDefault("newUsername", "");
        String newPassword = body.getOrDefault("newPassword", "");
        Map<String, Object> result = authService.verifyOtpAndUpdateCredentials(email, mobileNumber, otpCode, newUsername, newPassword);
        return ResponseEntity.ok(ApiResponse.success("Admin username and password updated in Neon DB", result));
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change admin username & password (supports OTP verification)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> changePassword(
            @Valid @RequestBody PasswordChangeRequest request,
            Principal principal
    ) {
        if (request.getOtpCode() != null && !request.getOtpCode().isBlank()) {
            Map<String, Object> result = authService.verifyOtpAndUpdateCredentials(
                    request.getEmail(),
                    request.getMobileNumber(),
                    request.getOtpCode(),
                    request.getNewUsername(),
                    request.getNewPassword()
            );
            return ResponseEntity.ok(ApiResponse.success("Admin credentials updated in Neon DB", result));
        }
        String name = principal != null ? principal.getName() : "raviranjan";
        authService.changePassword(name, request);
        return ResponseEntity.ok(ApiResponse.success("Password updated successfully", authService.getCredentialMetadata()));
    }
}
