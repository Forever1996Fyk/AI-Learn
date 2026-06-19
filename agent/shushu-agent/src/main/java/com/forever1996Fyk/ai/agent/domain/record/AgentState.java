package com.forever1996Fyk.ai.agent.domain.record;

import com.google.common.collect.Lists;

import java.util.List;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/6/19 23:51
 **/
public class AgentState {

    private List<SearchResult> searchResults = Lists.newArrayList();

    public List<SearchResult> getSearchResults() {
        return searchResults;
    }

    public void setSearchResults(List<SearchResult> searchResults) {
        this.searchResults = searchResults;
    }
}
