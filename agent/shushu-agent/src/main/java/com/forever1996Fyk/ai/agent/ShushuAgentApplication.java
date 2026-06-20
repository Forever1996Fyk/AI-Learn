package com.forever1996Fyk.ai.agent;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/6/19 21:09
 **/
@SpringBootApplication
@MapperScan(basePackages = "com.forever1996Fyk.ai.agent.repository.mapper")
public class ShushuAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShushuAgentApplication.class, args);
    }
}
