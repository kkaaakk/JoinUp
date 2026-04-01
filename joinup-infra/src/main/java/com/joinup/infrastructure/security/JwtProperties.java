package com.joinup.infrastructure.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "joinup.security.jwt")
public class JwtProperties {

    private String secret = "CHANGE_ME_TO_A_LONG_RANDOM_SECRET_WITH_AT_LEAST_32_BYTES";
    private long expireMinutes = 120;
    private String issuer = "joinup";
}
