package com.loanpathfinder.acceptance;

import java.time.Instant;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Replaces the injected clock and email gateway (contracts.md: "Injected"). */
@TestConfiguration
public class TestDoubles {

    public static final Instant NOW = Instant.parse("2026-11-20T15:00:00Z");

    @Bean
    @Primary
    public MutableClock testClock() {
        return new MutableClock(NOW);
    }

    @Bean
    @Primary
    public RecordingEmailGateway recordingEmailGateway() {
        return new RecordingEmailGateway();
    }
}
