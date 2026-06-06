package com.forever1996Fyk.ai.intelligent.customer.rag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/6/6 22:26
 **/
@ConfigurationProperties(prefix = Neo4jProperties.PREFIX)
public class Neo4jProperties {
    public static final String PREFIX = "neo4j";

    private String uri;

    private String username;

    private String password;


    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
