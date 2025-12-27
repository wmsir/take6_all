package com.example.take6server.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class VerificationCodeService {

    private static final Logger logger = LoggerFactory.getLogger(VerificationCodeService.class);
    private static final int CODE_LENGTH = 6;
    private static final int EXPIRY_MINUTES = 10; // Code expires in 10 minutes
    private static final int MAX_ATTEMPTS_BEFORE_LOCKOUT = 5; // Max verification attempts

    // Note: This in-memory map is not suitable for production in a distributed environment
    // or for applications requiring persistence. Consider using Redis or a database.
    private final Map<String, VerificationAttempt> attempts = new ConcurrentHashMap<>();

    private static class VerificationAttempt {
        String code;
        LocalDateTime expiryTime;
        int tryCount = 0;

        VerificationAttempt(String code, LocalDateTime expiryTime) {
            this.code = code;
            this.expiryTime = expiryTime;
        }
    }

    public String generateAndStoreCode(String email) {
        String code = generateRandomCode();
        attempts.put(email, new VerificationAttempt(code, LocalDateTime.now().plusMinutes(EXPIRY_MINUTES)));
        logger.info("Generated verification code for email: {}", email);
        return code;
    }

    public boolean verifyCode(String email, String code) {
        VerificationAttempt attempt = attempts.get(email);

        if (attempt == null) {
            logger.warn("Verification attempt for email {} failed: No code found (possibly expired or never requested).", email);
            return false; // No code requested, already used, or structure cleared.
        }

        if (LocalDateTime.now().isAfter(attempt.expiryTime)) {
            logger.warn("Verification attempt for email {} failed: Code expired.", email);
            attempts.remove(email); // Clean up expired code
            return false;
        }

        if (attempt.code.equals(code)) {
            logger.info("Verification successful for email: {}", email);
            attempts.remove(email); // Valid, remove after use
            return true;
        }

        attempt.tryCount++;
        logger.warn("Verification attempt {} for email {} failed: Invalid code.", attempt.tryCount, email);
        if (attempt.tryCount >= MAX_ATTEMPTS_BEFORE_LOCKOUT) {
            logger.warn("Max verification attempts reached for email {}. Removing code.", email);
            attempts.remove(email); // Lockout after too many failed attempts
        }
        return false;
    }

    private String generateRandomCode() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(random.nextInt(10)); // Generates a digit between 0-9
        }
        return sb.toString();
    }
}