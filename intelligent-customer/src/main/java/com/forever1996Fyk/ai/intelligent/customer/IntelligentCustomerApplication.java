package com.forever1996Fyk.ai.intelligent.customer;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/6/2 22:19
 **/
@SpringBootApplication
@MapperScan(basePackages = "com.forever1996Fyk.ai.intelligent.customer.document.repository.mapper")
public class IntelligentCustomerApplication {

    public static void main(String[] args) {
        SpringApplication.run(IntelligentCustomerApplication.class, args);
    }
}
