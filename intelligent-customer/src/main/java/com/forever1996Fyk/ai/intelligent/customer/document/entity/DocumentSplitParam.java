package com.forever1996Fyk.ai.intelligent.customer.document.entity;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/6/4 09:20
 **/
public record DocumentSplitParam(String splitType, Integer chunkSize, Integer overlap, Integer titleLevel, String separator, String regex) {
}
