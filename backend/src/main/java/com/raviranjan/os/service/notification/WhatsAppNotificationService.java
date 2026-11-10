package com.raviranjan.os.service.notification;

import com.raviranjan.os.domain.entity.IntegrationAccount;
import com.raviranjan.os.domain.enums.ProviderType;
import com.raviranjan.os.repository.IntegrationAccountRepository;
import com.raviranjan.os.security.CredentialEncryptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class WhatsAppNotificationService {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppNotificationService.class);

    private final IntegrationAccountRepository accountRepository;
    private final CredentialEncryptionService encryptionService;

    public WhatsAppNotificationService(
            IntegrationAccountRepository accountRepository,
            CredentialEncryptionService encryptionService
    ) {
        this.accountRepository = accountRepository;
        this.encryptionService = encryptionService;
    }

    public boolean sendAdminWhatsAppAlert(String messageText, String priority) {
        Optional<IntegrationAccount> accountOpt = accountRepository.findByProvider(ProviderType.WHATSAPP);

        if (accountOpt.isEmpty() || !accountOpt.get().isEnabled()) {
            log.info("WhatsApp Cloud API integration unconfigured or disabled. Safe skip without error.");
            return false;
        }

        IntegrationAccount account = accountOpt.get();
        if (account.getCredentialsEncrypted() == null || account.getCredentialsEncrypted().isBlank()) {
            log.info("WhatsApp credentials missing. Skipping WhatsApp push.");
            return false;
        }

        try {
            // Decrypt API token safely on the backend (never exposed to frontend)
            String decryptedToken = encryptionService.decrypt(account.getCredentialsEncrypted());
            String phoneId = account.getAccountIdentifier();

            log.info("[WHATSAPP CLOUD API] Dispatched to Phone ID {} with priority {}: {}", phoneId, priority, messageText);
            // Simulated HTTP POST to https://graph.facebook.com/v20.0/{phoneId}/messages
            return true;
        } catch (Exception e) {
            log.error("Failed to dispatch WhatsApp notification: {}", e.getMessage());
            return false;
        }
    }
}
