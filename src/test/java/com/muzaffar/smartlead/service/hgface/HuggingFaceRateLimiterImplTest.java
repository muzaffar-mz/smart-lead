package com.muzaffar.smartlead.service.hgface;

import com.muzaffar.smartlead.exceptions.RateLimitInfo;
import com.muzaffar.smartlead.service.hgface.impl.HuggingFaceRateLimiterImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class HuggingFaceRateLimiterImplTest {

    private HuggingFaceRateLimiterImpl underTest;

    @BeforeEach
    void setUp() {
        underTest = new HuggingFaceRateLimiterImpl();
    }


    @Test
    void canSendRequestsIsTrueInitially() {
        //initially no blocks

        //given
        //when
        //then
        assertThat(underTest.getBlockedUntil()).isNull();
        assertThat(underTest.canSendRequests()).isTrue();
    }

    @Test
    void registerFailureBlocksUntilRetryAfterWhenDailyLimitIsExhausted() {

        //daily rate limit is exhausted.
        //next request are blocked until block is released

        //given
        var before = LocalDateTime.now();
        var retryAfter = 60_000L;


        //when
        var rateLimitInfo = new RateLimitInfo(
                retryAfter,
                1_000L,
                8_000L,
                0L,
                null,
                5_000L,
                null);

        underTest.registerFailure(rateLimitInfo);

        //then
        assertThat(underTest.getBlockedUntil()).isNotNull();
        assertThat(underTest.canSendRequests()).isFalse();
        assertThat(underTest.getBlockedUntil())
                .isAfterOrEqualTo(before.plus(Duration.ofMillis(retryAfter)))
                .isBeforeOrEqualTo(LocalDateTime.now().plus(Duration.ofMillis(retryAfter)));
    }

    @Test
    void registerFailureFallsBackToDailyResetWhenThereIsNoRetryAfter() {

        //rate limit per minute exhausted

        //given
        var before = LocalDateTime.now();
        var resetRequestsPerDayMillis = 120_000L;


        //when
        var rateLimitInfo = new RateLimitInfo(
                null,
                1_000L,
                null,
                0L,
                null,
                resetRequestsPerDayMillis,
                null);
        underTest.registerFailure(rateLimitInfo);

        //then
        assertThat(underTest.canSendRequests()).isFalse();
        assertThat(underTest.getBlockedUntil()).isNotNull();
        assertThat(underTest.getBlockedUntil())
                .isAfterOrEqualTo(before.plus(Duration.ofMillis(resetRequestsPerDayMillis)))
                .isBeforeOrEqualTo(LocalDateTime.now().plus(Duration.ofMillis(resetRequestsPerDayMillis)));
    }

    @Test
    void registerFailureIgnoresPerMinuteLimitsWhileDailyRequestsRemain() {
        //given
        var retryAfter = 60_000L;


        //when
        var rateLimitInfo = new RateLimitInfo(
                retryAfter,
                null,
                null,
                10L,
                null,
                5_000L,
                null);
        underTest.registerFailure(rateLimitInfo);

        //then
        assertThat(underTest.canSendRequests()).isTrue();
        assertThat(underTest.getBlockedUntil()).isNull();
    }

    @Test
    void registerFailure_ignoresMissingInformation() {

        //given
        var rateLimitOne = new RateLimitInfo(
                60_000L,
                null,
                null,
                null,
                null,
                null,
                null);
        var rateLimitTwo = new RateLimitInfo(
                null,
                null,
                null,
                0L,
                null,
                null,
                null);

        //when
        underTest.registerFailure(null);
        underTest.registerFailure(rateLimitOne);
        underTest.registerFailure(rateLimitTwo);

        //then
        assertThat(underTest.canSendRequests()).isTrue();
        assertThat(underTest.getBlockedUntil()).isNull();
    }

    @Test
    void canSendRequestsIsTrueAgainOnceTheBlockHasPassed() throws InterruptedException {

        //given
        var retryAfterMillis = 100L;
        var rateLimitOne = new RateLimitInfo(
                retryAfterMillis,
                null,
                null,
                0L,
                null,
                null,
                null);

        //when
        underTest.registerFailure(rateLimitOne);

        //then
        //blocked right after the failure
        assertThat(underTest.canSendRequests()).isFalse();
        assertThat(underTest.getBlockedUntil()).isNotNull();

        //the block is released once the retry-after time has passed
        Thread.sleep(retryAfterMillis);
        assertThat(underTest.canSendRequests()).isTrue();
    }
}
