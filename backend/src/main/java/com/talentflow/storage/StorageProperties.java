package com.talentflow.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(
        String endpoint,
        String bucket,
        String region,
        String accessKey,
        String secretKey,
        boolean pathStyle) {
}
