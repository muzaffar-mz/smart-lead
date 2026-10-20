package com.muzaffar.smartlead.service.hgface.impl;

import com.muzaffar.smartlead.exceptions.RateLimitInfo;
import com.muzaffar.smartlead.service.hgface.HuggingFaceRateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
@Service
public class HuggingFaceRateLimiterImpl implements HuggingFaceRateLimiter {

    private LocalDateTime blockedUntil;

    @Override
    public synchronized boolean canSendRequests() {
        if (blockedUntil == null) {
            return true;
        }

        return !LocalDateTime.now().isBefore(blockedUntil);
    }

    @Override
    public synchronized LocalDateTime getBlockedUntil() {
        return blockedUntil;
    }

    @Override
    public synchronized void registerFailure(RateLimitInfo rateLimit) {
        //we only care about daily reate limit exhaustion
        if (rateLimit == null || rateLimit.remainingRequestsPerDay() == null || rateLimit.remainingRequestsPerDay() > 0) {
            return;
        }

        Long waitMillis = rateLimit.retryAfterMillis() != null
                ? rateLimit.retryAfterMillis()
                : rateLimit.resetRequestsPerDayMillis();
        if (waitMillis == null) {
            log.warn("Daily request limit exhausted but no retry-after or reset header was sent");
            return;
        }
        blockedUntil = LocalDateTime.now().plus(Duration.ofMillis(waitMillis));
        log.warn("Daily request limit exhausted, no requests until {} (in {})",
                blockedUntil, Duration.ofMillis(waitMillis));
    }
}
