package com.joinup.infrastructure.config;

import com.joinup.infrastructure.security.JwtProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 基础设施统一开关。
 * 目前集中开启调度和 JWT 配置绑定，后续新增 infra 级别能力也优先收敛在这里。
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties(JwtProperties.class)
public class InfrastructureAutoConfiguration {
}
