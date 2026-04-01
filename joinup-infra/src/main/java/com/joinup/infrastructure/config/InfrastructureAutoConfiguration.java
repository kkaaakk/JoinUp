package com.joinup.infrastructure.config;

import com.joinup.infrastructure.security.JwtProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(JwtProperties.class)
public class InfrastructureAutoConfiguration {
}
