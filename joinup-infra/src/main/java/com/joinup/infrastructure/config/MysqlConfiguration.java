package com.joinup.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * MySQL / 事务能力基础配置。
 */
@Configuration
@EnableTransactionManagement
public class MysqlConfiguration {
}
