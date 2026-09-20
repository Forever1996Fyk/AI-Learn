package com.forever1996Fyk.ai.agentx.core.context.compress.strategy;

import com.forever1996Fyk.ai.agentx.core.context.ContextPolicy;
import com.forever1996Fyk.ai.agentx.core.context.TokenEstimator;
import com.forever1996Fyk.ai.agentx.core.context.compress.CompressionContext;
import com.forever1996Fyk.ai.agentx.core.context.compress.CompressionStrategy;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @program: AI-Learn
 * @description:
 * L1 历史工具调用列表（不调 LLM）。
 * 在历史轮次区域内，若存在连续 N 条（assistant+tool_calls 或 tool_response）≥ minConsecutiveToolMessages
 * 且总 token ≥ minCompressionTokens 的工具段，将该段替换为单条字符串模板消息。
 * 原文按段整体 offload，供 context_reload 工具回溯。
 * @author: YuKai Fan
 * @create: 2026/9/18 15:20
 **/
public class HistoricalToolListStrategy implements CompressionStrategy {

    private static final Logger log = LoggerFactory.getLogger(HistoricalToolListStrategy.class);

    @Override
    public boolean tryCompress(CompressionContext ctx) {
        ContextPolicy policy = ctx.policy();
        int scanEnd = ctx.historicalScanEnd(policy.lastKeep());
        if (scanEnd <= 0) {
            return false;
        }
        List<Message> messages = ctx.messages();
        List<int[]> ranges = findConsecutiveToolRuns(messages, 0, scanEnd, policy);
        if (ranges.isEmpty()) {
            return false;
        }
        int compressedSegments = 0;
        for (int i = ranges.size() - 1; i >= 0; i--) {
            int[] range = ranges.get(i);
            int start = range[0];
            int end = range[1];
            List<Message> segment = new ArrayList<>(messages.subList(start, end));

            String uuid = offloadSegment(ctx, segment);
            String summary = buildToolListSummary(segment, uuid, policy);

            for (int j = end - 1; j >= start; j--) {
                messages.remove(j);
            }
            messages.add(start, new AssistantMessage(summary));
            compressedSegments++;
        }

        log.info("[L1] Compressed historical tool segments: segments={}, scanEnd={}",
                compressedSegments, scanEnd);
        return true;
    }

    /**
     * 在 [from, to) 范围内扫描消息列表，找出所有满足压缩阈值的连续工具消息段。
     * <p>
     * 连续工具消息是指相邻的 ToolResponseMessage 或带 toolCalls 的 AssistantMessage，
     * 当连续条数 ≥ minConsecutiveToolMessages 且总 token ≥ minCompressionTokens 时，
     * 该段被视为可压缩区间，返回 [start, end) 形式的索引对。
     *
     * @param messages 消息列表
     * @param from     扫描起始索引（含）
     * @param to       扫描结束索引（不含）
     * @param policy   上下文策略，提供压缩阈值参数
     * @return 可压缩的连续工具消息段列表，每段为 int[]{start, end}
     */
    private List<int[]> findConsecutiveToolRuns(List<Message> messages, int from, int to, ContextPolicy policy) {
        List<int[]> ranges = new ArrayList<>();
        // 当前连续工具段的起始索引，-1 表示未在追踪中
        int runStart = -1;
        // 当前连续工具段的消息条数
        int runLen = 0;
        // 当前连续工具段的估算 token 总数
        int runTokens = 0;

        for (int i = from; i < to; i++) {
            Message msg = messages.get(i);
            if (isToolMessage(msg)) {
                // 遇到工具消息：记录起始位置并累加统计
                if (runStart < 0) {
                    runStart = i;
                }
                runLen++;
                runTokens += TokenEstimator.estimateTokens(msg);
            } else {
                // 遇到非工具消息：当前连续段断裂，检查是否满足压缩阈值
                if (matchesThreshold(runLen, runTokens, policy)) {
                    ranges.add(new int[]{runStart, i});
                }
                // 重置追踪状态
                runStart = -1;
                runLen = 0;
                runTokens = 0;
            }
        }
        // 处理循环结束后仍未闭合的尾部工具段
        if (matchesThreshold(runLen, runTokens, policy)) {
            ranges.add(new int[]{runStart, to});
        }
        return ranges;
    }

    private boolean isToolMessage(Message msg) {
        if (msg instanceof ToolResponseMessage) {
            return true;
        }
        if (msg instanceof AssistantMessage am) {
            return !am.getToolCalls().isEmpty();
        }
        return false;
    }

    private String offloadSegment(CompressionContext ctx, List<Message> segment) {
        return ctx.hasOffloadStore()
                ? ctx.offloadStore().offload(ctx.conversationId(), ctx.sessionId(), segment)
                : null;
    }

    private String buildToolListSummary(List<Message> segment, String uuid, ContextPolicy policy) {
        List<String> entries = collectToolEntries(segment, policy);

        StringBuilder sb = new StringBuilder();
        sb.append("[历史工具调用记录]\n");
        sb.append("本轮对话之前，已执行过以下工具调用：\n");
        for (String entry : entries) {
            sb.append("- ").append(entry).append("\n");
        }
        if (uuid != null) {
            sb.append("（该段完整参数与结果已 offload，uuid=")
                    .append(uuid)
                    .append("；如需原文可调用 context_reload(uuid) 工具取回）");
        } else {
            sb.append("（当前未启用 offload 存储，仅保留工具调用摘要）");
        }
        return sb.toString();
    }

    private List<String> collectToolEntries(List<Message> segment, ContextPolicy policy) {
        List<String> entries = new ArrayList<>();
        for (Message msg : segment) {
            if (msg instanceof AssistantMessage am) {
                for (AssistantMessage.ToolCall tc : am.getToolCalls()) {
                    entries.add(formatToolCall(tc, policy));
                }
            } else if (msg instanceof ToolResponseMessage trm) {
                for (ToolResponseMessage.ToolResponse resp : trm.getResponses()) {
                    entries.add(formatResponseOnly(resp));
                }
            }
        }
        return deduplicatePreservingOrder(entries);
    }

    private String formatToolCall(AssistantMessage.ToolCall tc, ContextPolicy policy) {
        String name = tc.name();
        String args = truncate(tc.arguments(), policy.toolArgPreviewChars());
        return name + "(" + args + ")";
    }

    private String formatResponseOnly(ToolResponseMessage.ToolResponse resp) {
        String name = StringUtils.defaultIfBlank(resp.name(), "unknown");
        return name + "(response only)";
    }

    private String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        if (s.length() <= max) {
            return s;
        }
        return s.substring(0, max) + "...";
    }

    private List<String> deduplicatePreservingOrder(List<String> entries) {
        List<String> deduped = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String entry : entries) {
            if (seen.add(entry)) {
                deduped.add(entry);
            }
        }
        return deduped;
    }

    private boolean matchesThreshold(int len, int tokens, ContextPolicy policy) {
        return len >= policy.minConsecutiveToolMessages()
                && tokens >= policy.minCompressionTokens();
    }

}
