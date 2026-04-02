package com.joinup.infrastructure.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 配置项绑定。
 */
@Data
@ConfigurationProperties(prefix = "joinup.security.jwt")
public class JwtProperties {

    /** HS256 密钥，生产环境必须替换。 */
    private String secret = "CHANGE_ME_TO_A_LONG_RANDOM_SECRET_WITH_AT_LEAST_32_BYTES";
    /** Token 有效期，单位分钟。 */
    private long expireMinutes = 120;
    /** Token 发行方。 */
    private String issuer = "joinup";
}
