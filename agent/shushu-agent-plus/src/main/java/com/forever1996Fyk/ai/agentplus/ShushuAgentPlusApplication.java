package com.forever1996Fyk.ai.agentplus;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/9/20 17:35
 **/
@SpringBootApplication
@MapperScan({
        "com.forever1996Fyk.ai.agentplus.mapper",
        "com.forever1996Fyk.ai.agentplus.sys.mapper"
})
@EnableScheduling
public class ShushuAgentPlusApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShushuAgentPlusApplication.class, args);
    }
}
