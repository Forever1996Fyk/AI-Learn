package com.forever1996Fyk.ai.agent.domain;

import com.forever1996Fyk.ai.agent.domain.record.SearchResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/6/27 22:56
 **/
@Data
@Builder
@AllArgsConstructor
public class SimpleReactResult {
    /**
     * 最终答案（纯文本）
     */
    private String answer;

    /**
     * 搜索结果列表
     */
    private List<SearchResult> searchResults;
}
