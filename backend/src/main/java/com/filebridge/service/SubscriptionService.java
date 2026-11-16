package com.filebridge.service;

import com.filebridge.exception.FileNotFoundException;
import com.filebridge.model.SubscriptionPlan;
import com.filebridge.model.SubscriptionStatus;
import com.filebridge.model.User;
import com.filebridge.repository.FileMetadataRepository;
import com.filebridge.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class SubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionService.class);

    private final UserRepository userRepository;
    private final FileMetadataRepository fileMetadataRepository;
    private final long freeStorageBytes;
    private final long premiumStorageBytes;
    private final int premiumPriceInr;

    public SubscriptionService(
            UserRepository userRepository,
            FileMetadataRepository fileMetadataRepository,
            @Value("${filebridge.subscription.free-storage-bytes:104857600}") long freeStorageBytes,
            @Value("${filebridge.subscription.premium-storage-bytes:524288000}") long premiumStorageBytes,
            @Value("${filebridge.subscription.premium-price-inr:199}") int premiumPriceInr) {
        this.userRepository = userRepository;
        this.fileMetadataRepository = fileMetadataRepository;
        this.freeStorageBytes = freeStorageBytes;
        this.premiumStorageBytes = premiumStorageBytes;
        this.premiumPriceInr = premiumPriceInr;
    }

    public long getStorageLimitForUser(User user) {
        if (user == null) {
            return freeStorageBytes;
        }

        // Check if plan has expired
        if (user.getSubscriptionPlan() == SubscriptionPlan.PREMIUM) {
            if (user.getSubscriptionEndDate() != null && user.getSubscriptionEndDate().isBefore(Instant.now())) {
                log.info("Subscription expired for user: {}", user.getEmail());
                // Mark status expired if not already
                if (user.getSubscriptionStatus() == SubscriptionStatus.ACTIVE) {
                    user.setSubscriptionStatus(SubscriptionStatus.EXPIRED);
                    userRepository.save(user);
                }
                return freeStorageBytes;
            }
            if (user.getSubscriptionStatus() == SubscriptionStatus.ACTIVE) {
                return premiumStorageBytes;
            }
        }
        return freeStorageBytes;
    }

    public long getStorageLimitByUserId(String userId) {
        User user = userRepository.findById(userId).orElse(null);
        return getStorageLimitForUser(user);
    }

    public User upgradeToPremium(String userId, int durationDays) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new FileNotFoundException("User not found with id: " + userId));

        Instant now = Instant.now();
        Instant endDate;
        if (user.isPremium() && user.getSubscriptionEndDate() != null && user.getSubscriptionEndDate().isAfter(now)) {
            // Extend existing subscription
            endDate = user.getSubscriptionEndDate().plus(durationDays, ChronoUnit.DAYS);
        } else {
            endDate = now.plus(durationDays, ChronoUnit.DAYS);
        }

        user.setSubscriptionPlan(SubscriptionPlan.PREMIUM);
        user.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
        user.setSubscriptionStartDate(now);
        user.setSubscriptionEndDate(endDate);

        User saved = userRepository.save(user);
        log.info("User '{}' upgraded to PREMIUM until {}", user.getEmail(), endDate);
        return saved;
    }

    public long getFreeStorageBytes() { return freeStorageBytes; }
    public long getPremiumStorageBytes() { return premiumStorageBytes; }
    public int getPremiumPriceInr() { return premiumPriceInr; }
}
