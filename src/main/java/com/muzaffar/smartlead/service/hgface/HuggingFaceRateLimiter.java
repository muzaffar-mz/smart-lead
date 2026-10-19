package com.muzaffar.smartlead.service.hgface;

import com.muzaffar.smartlead.exceptions.RateLimitInfo;

import java.time.LocalDateTime;

public interface HuggingFaceRateLimiter {

    boolean canSendRequests();

    LocalDateTime getBlockedUntil();

    void registerFailure(RateLimitInfo rateLimit);
}
