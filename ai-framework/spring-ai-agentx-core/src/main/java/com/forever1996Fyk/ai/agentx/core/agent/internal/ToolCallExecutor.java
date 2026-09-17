package com.forever1996Fyk.ai.agentx.core.agent.internal;

import com.alibaba.fastjson2.JSON;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forever1996Fyk.ai.agentx.core.model.AgentStreamEvent;
import com.forever1996Fyk.ai.agentx.core.model.PendingToolCall;
import com.forever1996Fyk.ai.agentx.core.model.RunnableParams;
import com.forever1996Fyk.ai.agentx.core.stage.AgentRuntimeContext;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

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

    /**
     * 校验并修复工具调用参数。
     * 如果某个 tool call 的 arguments 不是合法 JSON，替换为 {@code {}}，避免后续 API 调用 400。
     * 全部合法时原样返回传入列表。
     */
    public List<AssistantMessage.ToolCall> sanitizeToolCalls(List<AssistantMessage.ToolCall> toolCalls) {
        boolean needsFix = false;
        for (AssistantMessage.ToolCall tc : toolCalls) {
            String args = tc.arguments();
            if (StringUtils.isNotBlank(args) && !isValidJson(args)) {
                needsFix = true;
                break;
            }
        }
        if (!needsFix) {
            return toolCalls;
        }
        List<AssistantMessage.ToolCall> fixed = new ArrayList<>(toolCalls.size());
        for (AssistantMessage.ToolCall tc : toolCalls) {
            String args = tc.arguments();
            if (StringUtils.isBlank(args) || isValidJson(args)) {
                fixed.add(tc);
            } else {
                log.warn("工具 '{}' 的 arguments 不是合法 JSON，已替换为空对象: {}", tc.name(), args);
                fixed.add(new AssistantMessage.ToolCall(tc.id(), tc.type(), tc.name(), "{}"));
            }
        }
        return fixed;
    }

    private boolean isValidJson(String json) {
        try {
            JSON.parse(json);
            return true;
        } catch (Exception e) {
            return false;
        }
    }


    public void addToolCallMessages(AssistantMessage.ToolCall toolCall,
                                    ToolExecutionResult result,
                                    List<Message> messages) {
        addNormalToolMessage(toolCall, result.rawResult(), messages);
    }

    public void addNormalToolMessage(AssistantMessage.ToolCall toolCall,
                                     String rawResult, List<Message> messages) {
        ToolResponseMessage.ToolResponse tr = new ToolResponseMessage.ToolResponse(
                toolCall.id(), toolCall.name(), rawResult);
        messages.add(ToolResponseMessage.builder()
                .responses(List.of(tr))
                .build());
    }

    /**
     * 执行非拦截的工具调用（拦截的由 resume 处理）。
     */
    public void executeNonPendingTools(List<AssistantMessage.ToolCall> allToolCalls,
                                       List<PendingToolCall> pending,
                                       List<Message> messages,
                                       RunnableParams params,
                                       AgentRuntimeContext runtimeCtx) {
        Set<String> pendingIds = new HashSet<>();
        for (PendingToolCall ptc : pending) {
            pendingIds.add(ptc.id());
        }

        for (AssistantMessage.ToolCall tc : allToolCalls) {
            if (!pendingIds.contains(tc.id())) {
                ToolExecutionResult result = executeSingleTool(tc, params, null, runtimeCtx);
                List<Message> collected = collectToolCallMessages(tc, result);
                messages.addAll(collected);
                if (runtimeCtx != null) {
                    runtimeCtx.appendOriginalMessages(collected);
                }
            }
        }
    }

    /**
     * 异步执行工具调用（保证顺序）。
     * 多个工具并发执行，但结果按原始 toolCalls 顺序添加到 messages。
     */
    public void executeToolCallsAsync(Sinks.Many<AgentStreamEvent> sink,
                                      List<AssistantMessage.ToolCall> toolCalls,
                                      List<Message> messages,
                                      RunnableParams params,
                                      AgentRuntimeContext runtimeCtx,
                                      Runnable onComplete) {
        int total = toolCalls.size();
        AtomicInteger completedCount = new AtomicInteger(0);
        List<List<Message>> results = new ArrayList<>(total);
        for (int i = 0; i < total; i++) {
            results.add(null);
        }
        List<ToolExecDetail> execDetails = new ArrayList<>(total);
        for (int i = 0; i < total; i++) {
            execDetails.add(null);
        }

        for (int i = 0; i < toolCalls.size(); i++) {
            final int index = i;
            AssistantMessage.ToolCall toolCall = toolCalls.get(i);
            Schedulers.boundedElastic().schedule(() -> {
                try {
                    ToolExecutionResult toolResult = executeSingleTool(toolCall, params, sink, runtimeCtx);
                    results.set(index, collectToolCallMessages(toolCall, toolResult));
                    execDetails.set(index, new ToolExecDetail(toolCall, toolResult.rawResult(), null));
                } catch (Exception ex) {
                    log.error("Unexpected error in tool execution: {} - {}", toolCall.name(), ex.getMessage());
                    results.set(index, collectToolCallErrorMessages(toolCall, ex));
                    execDetails.set(index, new ToolExecDetail(toolCall, null, ex));
                } finally {
                    int completed = completedCount.incrementAndGet();
                    if (completed >= total) {
                        appendResultsInOrder(results, messages);
                        if (runtimeCtx != null) {
                            for (List<Message> result : results) {
                                runtimeCtx.appendOriginalMessages(result);
                            }
                        }

                        for (ToolExecDetail detail : execDetails) {
                            if (detail.error == null) {
                                sink.tryEmitNext(new AgentStreamEvent.ToolEnd(
                                        detail.toolCall.name(), detail.toolCall.id(), detail.rawResult));
                            }
                        }

                        onComplete.run();
                    }
                }
            });
        }
    }

    private void appendResultsInOrder(List<List<Message>> results, List<Message> messages) {
        for (List<Message> result : results) {
            messages.addAll(result);
        }
    }


    /**
     * 执行单个工具调用（非流式路径，sink 和 runtimeCtx 为 null）。
     */
    public ToolExecutionResult executeSingleTool(AssistantMessage.ToolCall toolCall, RunnableParams params) {
        return executeSingleTool(toolCall, params, null, null);
    }

    /**
     * 执行单个工具调用。
     *
     * @param toolCall   工具调用信息
     * @param params     调用参数
     * @param sink       事件 Sink（流式路径不为 null，非流式路径为 null）
     * @param runtimeCtx 当前调用的运行时上下文（null 时不触发 Hook）
     */
    public ToolExecutionResult executeSingleTool(AssistantMessage.ToolCall toolCall, RunnableParams params,
                                                 Sinks.Many<AgentStreamEvent> sink,
                                                 AgentRuntimeContext runtimeCtx) {
        String toolName = toolCall.name();
        String argsJson = toolCall.arguments();

        // 无参数工具：LLM 可能返回 null 或空字符串，默认为空 JSON 对象
        if (StringUtils.isBlank(argsJson)) {
            argsJson = "{}";
        }

        ToolCallback callback = toolMap.get(toolName);

        if (callback == null) {
            return errorResult(toolName, "不存在名为 '" + toolName + "' 的工具");
        }

        // 先按工具的 inputSchema 过滤，只注入该工具真实声明的字段（避免 MCP 服务端严格校验报错）
        argsJson = replaceToolParams(callback, argsJson, params);

        log.debug("Executing tool: {} with args: {}", toolName, argsJson);

        Object result;
        try {
            ToolContext toolContext = buildToolContext(params, sink);
            String effectiveArgs = argsJson;

            // Hook 处理后、工具执行前发射 ToolStart（携带 post-hook 入参，与工具真实执行一致）
            if (sink != null) {
                sink.tryEmitNext(new AgentStreamEvent.ToolStart(toolName, toolCall.id(), effectiveArgs));
            }

            result = callback.call(effectiveArgs, toolContext);
        } catch (Exception e) {
            log.error("Tool '{}' execution failed: {}", toolName, e.getMessage(), e);
            String hint = buildErrorHint(toolName, e);
            return errorResult(toolName, hint);
        }

        String rawResult = result.toString();
        return new ToolExecutionResult(rawResult);
    }

    public List<Message> collectToolCallMessages(AssistantMessage.ToolCall toolCall,
                                                 ToolExecutionResult result) {
        List<Message> collected = new ArrayList<>();
        addToolCallMessages(toolCall, result, collected);
        return collected;
    }

    public List<Message> collectToolCallErrorMessages(AssistantMessage.ToolCall toolCall,
                                                      Exception ex) {
        log.error("Unexpected error in tool execution: {} - {}", toolCall.name(), ex.getMessage());
        ToolExecutionResult errorResult = errorResult(toolCall.name(), "内部错误：" + ex.getMessage());
        List<Message> collected = new ArrayList<>();
        addNormalToolMessage(toolCall, errorResult.rawResult(), collected);
        return collected;
    }


    /**
     * 把 RunnableParams.toolParams 合并进 LLM 生成的工具调用 args，**仅注入工具 inputSchema 真实声明的字段**。
     * <p>
     * 三种情况都覆盖：
     * <ul>
     *   <li>LLM 漏掉 {@code userId} → 增量补上（避免 LLM 偶发漏字段导致工具收到 null）</li>
     *   <li>LLM 乱填 {@code "userId":"xxx"} → 覆盖为真值（框架参数不该信 LLM）</li>
     *   <li>LLM 传了旧版占位值 {@code "userId":"default"} → 同样覆盖为真值</li>
     * </ul>
     * 用 JSON merge 而非正则替换：能正确处理非 String 类型（Number/Boolean），
     * 且不依赖 LLM 是否在 args 里写了字段名。
     * <p>
     * <b>白名单过滤</b>：本地 {@code @Tool} 注解工具（MethodToolCallback）会忽略未知字段，
     * 但 MCP 工具走远程 JSON-RPC，服务端常严格校验（如 Tavily 用 Pydantic，多字段即报
     * {@code unexpected_keyword_argument}）。因此从 callback 的 inputSchema 读出合法参数名清单，
     * 只注入该工具声明的字段，未声明的跳过。
     */
    private String replaceToolParams(ToolCallback callback, String argsJson, RunnableParams params) {
        if (params == null || params.getToolParams() == null || params.getToolParams().isEmpty()) {
            return argsJson;
        }
        if (argsJson == null || argsJson.isBlank()) {
            return argsJson;
        }

        Set<String> accepted = getAcceptedParamNames(callback);
        if (accepted.isEmpty()) {
            // 拿不到 schema（异常或无 properties）→ 保守起见不注入，避免污染 MCP 严格校验工具
            log.debug("工具 {} 的 inputSchema 无 properties，跳过 toolParams 注入", callback.getToolDefinition().name());
            return argsJson;
        }

        try {
            Map<String, Object> args = objectMapper.readValue(argsJson,
                    new TypeReference<Map<String, Object>>() {
                    });
            for (Map.Entry<String, Object> entry : params.getToolParams().entrySet()) {
                if (accepted.contains(entry.getKey())) {
                    args.put(entry.getKey(), entry.getValue());
                } else {
                    log.debug("工具 {} 不接受参数 {}，跳过注入", callback.getToolDefinition().name(), entry.getKey());
                }
            }
            return objectMapper.writeValueAsString(args);
        } catch (Exception e) {
            log.error("替换工具参数失败（argsJson 不是合法 JSON，原样返回）: {}", argsJson, e);
            return argsJson;
        }
    }

    /**
     * 从 ToolCallback 的 inputSchema 读出合法参数名集合。
     * schema 形如 {"type":"object","properties":{"sql":{...},"userId":{...}}}。
     */
    private Set<String> getAcceptedParamNames(ToolCallback callback) {
        try {
            String schemaJson = callback.getToolDefinition().inputSchema();
            if (StringUtils.isBlank(schemaJson)) {
                return Set.of();
            }
            Map<String, Object> schema = objectMapper.readValue(schemaJson, new TypeReference<>() {});
            Object properties = schema.get("properties");
            if (!(properties instanceof Map<?, ?> map)) {
                return Set.of();
            }
            Set<String> names = new HashSet<>(map.size());
            for (Object k : map.keySet()) {
                if (k != null) {
                    names.add(k.toString());
                }
            }
            return names;
        } catch (Exception e) {
            log.warn("解析工具 {} inputSchema 失败，跳过 toolParams 注入: {}",
                    callback.getToolDefinition().name(), e.getMessage());
            return Set.of();
        }
    }

    private ToolContext buildToolContext(RunnableParams params, Sinks.Many<AgentStreamEvent> sink) {
        Map<String, Object> context = new HashMap<>();
        if (params != null) {
            if (params.getUserId() != null) {
                context.put("userId", params.getUserId());
            }
            if (params.getConversationId() != null) {
                context.put("conversationId", params.getConversationId());
            }
            context.put("runnableParams", params);
        }
        if (sink != null) {
            context.put("eventSink", sink);
        }
        return new ToolContext(context);
    }

    private String buildErrorHint(String toolName, Exception e) {
        if (e.getCause() instanceof JsonProcessingException
                || e instanceof JsonProcessingException) {
            return toolName + " 工具调用失败：参数 JSON 解析错误，可能是参数过长被截断。"
                    + "请尝试缩短参数或拆分为多步操作。";
        }
        return toolName + " 工具调用失败：" + e.getMessage();
    }

    private ToolExecutionResult errorResult(String toolName, String errorMessage) {
        try {
            Map<String, String> errorMap = new HashMap<>();
            errorMap.put("error", errorMessage);
            errorMap.put("tool", toolName);
            return new ToolExecutionResult(objectMapper.writeValueAsString(errorMap));
        } catch (JsonProcessingException ex) {
            return new ToolExecutionResult("{\"error\":\"" + errorMessage + "\"}");
        }
    }

    // ==================== 内部记录 ====================

    record ToolExecutionResult(String rawResult) {
    }

    record ToolExecDetail(AssistantMessage.ToolCall toolCall, String rawResult, Exception error) {
    }
}
