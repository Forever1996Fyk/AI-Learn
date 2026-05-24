package com.forever1996Fyk.ai.springai.example.entity;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/5/23 17:16
 **/
public record OrderChat(@JsonPropertyDescription("订单号") String orderId
        , @JsonPropertyDescription("用户Id") String userId
        , @JsonPropertyDescription("对话Id") String chatId
        , @JsonPropertyDescription("对话状态") ChatStatus status) {

}
