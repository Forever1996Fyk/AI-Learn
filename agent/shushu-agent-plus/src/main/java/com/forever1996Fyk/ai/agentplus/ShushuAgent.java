package com.forever1996Fyk.ai.agentplus;

import com.forever1996Fyk.ai.agentplus.domain.entity.AgentxFile;
import com.forever1996Fyk.ai.agentplus.prompt.SystemPrompt;
import com.forever1996Fyk.ai.agentplus.service.FileManageService;
import com.forever1996Fyk.ai.agentplus.service.UserContextBuilder;
import com.forever1996Fyk.ai.agentplus.skill.SkillManager;
import com.forever1996Fyk.ai.agentplus.tools.AnalyzeFileTool;
import com.forever1996Fyk.ai.agentplus.tools.SkillsTool;
import com.forever1996Fyk.ai.agentplus.util.ToolMergeUtil;
import com.forever1996Fyk.ai.agentx.core.agent.ReactAgent;
import com.forever1996Fyk.ai.agentx.core.model.AgentStreamEvent;
import com.forever1996Fyk.ai.agentx.core.model.RunnableParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/9/21 15:22
 **/
@Slf4j
@Service
@RequiredArgsConstructor
public class ShushuAgent {

    /**
     * Tools
     */
    private final AnalyzeFileTool analyzeFileTool;


    private final SkillManager skillManager;
    private final FileManageService fileManageService;
    private final UserContextBuilder userContextBuilder;

    private ToolCallback[] alwaysOnBaseTools;

    /**
     * 流式调用。根据 userId + 会话文件构造 instructions，根据 online 注入 Tavily，
     * 会话有任意文件（本次或历史）时注入 analyzeFile。
     * 文件清单不再拼到 query 末尾，改由 buildInstructions 写进 system prompt。
     */
    public Flux<AgentStreamEvent> streamForResult(String query, RunnableParams params,
                                                  boolean online, List<String> fileIds) {
        String convId = params.getConversationId();
        if (fileIds != null && !fileIds.isEmpty()) {
            fileManageService.linkToConversation(fileIds, convId);
        }
        // 会话所有文件（含本次新传），决定是否注入 analyzeFile
        List<AgentxFile> files = fileManageService.listByConversationId(convId);
        boolean hasAnyFile = !files.isEmpty();

        String instructions = buildInstructions(params.getUserId(), convId, files);
        ReactAgent agent = buildReactAgent(instructions, online, hasAnyFile);
    }

    /**
     * 按用户身份 + 会话文件构造 instructions，文件按时间统一编号。
     */
    private String buildInstructions(String userIdStr, String convId, List<AgentxFile> files) {
        Long userId = parseUserId(userIdStr);
        String base = (userId == null) ? SystemPrompt.build("") : SystemPrompt.build(userContextBuilder.build(userId));
        return appendSessionFiles(base, convId, files);
    }

    private ReactAgent buildReactAgent(String instructions, boolean online, boolean injectFileTool) {
        ToolCallback[] skillsTools = buildSkillsTools();
        ToolCallback[] alwaysOn = ToolMergeUtil.mergeTools(alwaysOnBaseTools, skillsTools);
    }

    /**
     * 从 DB 查 enabled skills，构建 SkillsTool。
     * 无启用 skill 时返回空数组（不注册 SkillsTool）。
     */
    private ToolCallback[] buildSkillsTools() {
        List<String> enabledDirs = skillManager.getEnabledSkillDirs();
        if (enabledDirs.isEmpty()) {
            return new ToolCallback[0];
        }
        SkillsTool.Builder builder = SkillsTool.builder();
        for (String dir : enabledDirs) {
            builder.addSkillsDirectory(dir);
        }
        return new ToolCallback[]{builder.build()};
    }

    /**
     * 把会话文件分两组展示：本次新上传（一组）+ 历史文件（按 session_id 分多组，同一轮上传的聚在一起）。
     */
    private String appendSessionFiles(String instructions, String conversationId,
                                      List<AgentxFile> files) {
        if (StringUtils.isBlank(conversationId)) {
            return instructions;
        }
        if (files.isEmpty()) {
            return instructions;
        }
        Set<String> currentSet = files.stream().map(AgentxFile::getFileId).collect(Collectors.toSet());
        List<AgentxFile> currentFiles = new ArrayList<>();
        Map<Long, List<AgentxFile>> historyBySession = new LinkedHashMap<>();
        for (AgentxFile file : files) {
            if (currentSet.contains(file.getFileId())) {
                currentFiles.add(file);
            } else {
                historyBySession.computeIfAbsent(file.getSessionId(), k -> new ArrayList<>()).add(file);
            }
        }
        int historyCount = historyBySession.values().stream().mapToInt(List::size).sum();

        List<String> parts = new ArrayList<>();
        parts.add(instructions);
        parts.add("## 当前会话文件（重要）");
        parts.add("");

        if (currentFiles.isEmpty()) {
            parts.add("（本次无新文件上传，仅可追问以下历史文件）");
            parts.add("");
        } else {
            parts.add("### 本次新上传（" + currentFiles.size() + " 个，是一个整体）");
            parts.add("");
            parts.add(renderGroup(currentFiles));
            parts.add("");
        }

        if (historyCount > 0) {
            parts.add("### 历史文件（" + historyCount + " 个，按上传轮次分组，从旧到新）");
            parts.add("");
            int sessionIdx = 0;
            int sessionTotal = historyBySession.size();
            for (Map.Entry<Long, List<AgentxFile>> entry : historyBySession.entrySet()) {
                sessionIdx++;
                List<AgentxFile> groupFiles = entry.getValue();
                String tag = entry.getKey() == null
                        ? "未知轮次"
                        : "第 " + sessionIdx + " 轮";
                if (entry.getKey() != null && sessionTotal > 1) {
                    if (sessionIdx == 1) {
                        tag += "（最早）";
                    } else if (sessionIdx == sessionTotal) {
                        tag += "（最新）";
                    }
                }
                parts.add("【" + tag + "，" + groupFiles.size() + "个】");
                parts.add(renderGroup(groupFiles));
                parts.add("");
            }
        }

        parts.add("【触发规则】");
        parts.add("- 用户说\"这个/这些/它们/总结一下/讲了什么/分析下\"等指代词或泛指时，默认指【本次新上传】整组，必须挨个调 analyzeFile(fileId, question) 加载");
        parts.add("- 用户说\"上一篇/上一个/刚才的文档\"时，默认指【历史文件】里最新一轮的全部");
        parts.add("- 用户明说某个文件名/序号时，按指定的加载");
        parts.add("- 用户问题没明示文件但语义上可能指文件时，默认尝试调 analyzeFile");
        parts.add("- 普通闲聊（天气、写代码、打招呼）不要调");
        return String.join("\n", parts);
    }


    /**
     * 渲染一组文件列表，组内编号从 1 开始。
     */
    private String renderGroup(List<AgentxFile> groupFiles) {
        return IntStream.range(0, groupFiles.size())
                .mapToObj(i -> {
                    AgentxFile f = groupFiles.get(i);
                    return (i + 1) + ". fileId: " + f.getFileId()
                            + "，文件名: " + f.getFileName()
                            + "，类型: " + f.getFileType();
                })
                .collect(Collectors.joining("\n"));
    }

    private static Long parseUserId(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
