package com.talentflow;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestEmailConfig {

    @Bean
    @Primary
    RecordingEmailService recordingEmailService() {
        return new RecordingEmailService();
    }
}
