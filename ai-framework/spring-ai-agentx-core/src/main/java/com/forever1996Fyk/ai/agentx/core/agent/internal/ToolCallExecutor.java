package com.forever1996Fyk.ai.agentx.core.agent.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;

import java.util.Map;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/9/9 18:23
 **/
public class ToolCallExecutor {

    private static final Logger log = LoggerFactory.getLogger(ToolCallExecutor.class);

    private final Map<String, ToolCallback> toolMap;
    private final ObjectMapper objectMapper;
    private final String askUserToolName;

    public ToolCallExecutor(Map<String, ToolCallback> toolMap, ObjectMapper objectMapper,
                            String askUserToolName) {
        this.toolMap = toolMap;
        this.objectMapper = objectMapper;
        this.askUserToolName = askUserToolName;
    }
}
