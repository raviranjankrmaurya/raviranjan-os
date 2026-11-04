package com.raviranjan.os.service;

import com.raviranjan.os.dto.auth.AuthResponse;
import com.raviranjan.os.dto.auth.LoginRequest;
import com.raviranjan.os.dto.auth.PasswordChangeRequest;

import java.util.Map;

public interface AuthService {
    AuthResponse login(LoginRequest request);
    AuthResponse refreshToken(String refreshToken);
    void changePassword(String username, PasswordChangeRequest request);
    Map<String, Object> requestCredentialOtp(String email, String mobileNumber, String purpose);
    Map<String, Object> verifyOtpAndUpdateCredentials(String email, String mobileNumber, String otpCode, String newUsername, String newPassword);
    Map<String, Object> getCredentialMetadata();
    AuthResponse issueAdminSessionToken();
}
