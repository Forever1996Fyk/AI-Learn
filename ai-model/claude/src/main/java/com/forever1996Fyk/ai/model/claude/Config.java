package com.forever1996Fyk.ai.model.claude;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/4/20 11:33
 **/
public class Config {

    private final static String API_KEY = System.getProperty("claude.apikey");

    public static String getApiKey() {
        return API_KEY;
    }
}
