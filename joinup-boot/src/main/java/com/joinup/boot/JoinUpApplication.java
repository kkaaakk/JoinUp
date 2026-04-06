package com.joinup.boot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.joinup")
public class JoinUpApplication {

    public static void main(String[] args) {
        // 启动模块只负责装配和引导，不承载具体业务逻辑。
        SpringApplication.run(JoinUpApplication.class, args);
    }
}
