package com.forever1996Fyk.ai.agentx.core.stage;

import com.forever1996Fyk.ai.agentx.core.model.RunnableParams;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/9/9 16:58
 **/
public class AgentRuntimeContext {

    private final String query;
    private final RunnableParams params;

    public AgentRuntimeContext(String query, RunnableParams params) {
        this.query = query;
        this.params = params;
    }

    public String getQuery() {
        return query;
    }

    public RunnableParams getParams() {
        return params;
    }
}
