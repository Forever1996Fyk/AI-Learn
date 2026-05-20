package com.forever1996Fyk.ai.springai.chatclient.model;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.math.BigDecimal;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/5/20 21:49
 **/
public record Book(@JsonPropertyDescription("书名 已中文展示") String name, @JsonPropertyDescription("作者") String author,
                   @JsonPropertyDescription("出版社") String description, @JsonPropertyDescription("价格，人民币，以分为单位") String price) {
}
