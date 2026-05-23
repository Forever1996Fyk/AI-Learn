package com.forever1996Fyk.ai.langchain4j.entity;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/5/22 21:25
 **/
public record Book(@JsonPropertyDescription("书名 已中文展示") String name, @JsonPropertyDescription("作者") String author,
                   @JsonPropertyDescription("出版社") String description, @JsonPropertyDescription("价格，人民币，以分为单位") String price) {
}