package com.forever1996Fyk.ai.agent.controller;

import com.forever1996Fyk.ai.agent.agent.deepresearch.PlanExecuteAgent;
import com.forever1996Fyk.ai.agent.agent.file.FileReactAgent;
import com.forever1996Fyk.ai.agent.agent.pptx.PPTBuilderAgent;
import com.forever1996Fyk.ai.agent.agent.skills.SkillsReactAgent;
import com.forever1996Fyk.ai.agent.agent.skills.manual.SkillManager;
import com.forever1996Fyk.ai.agent.agent.skills.manual.config.SkillConfig;
import com.forever1996Fyk.ai.agent.agent.skills.manual.tool.ReadSkillTool;
import com.forever1996Fyk.ai.agent.agent.websearch.WebSearchReactAgent;
import com.forever1996Fyk.ai.agent.manager.AgentTaskManager;
import com.forever1996Fyk.ai.agent.service.AiPptInstService;
import com.forever1996Fyk.ai.agent.service.AiPptTemplateService;
import com.forever1996Fyk.ai.agent.service.AiSessionService;
import com.forever1996Fyk.ai.agent.service.ImageGenerationService;
import com.forever1996Fyk.ai.agent.service.MinioService;
import com.forever1996Fyk.ai.agent.service.PptIntentRecognizerService;
import com.forever1996Fyk.ai.agent.service.PptRenderService;
import com.forever1996Fyk.ai.agent.tool.BashTool;
import com.forever1996Fyk.ai.agent.tool.FileContentTool;
import com.forever1996Fyk.ai.agent.tool.FileSystemTool;
import com.forever1996Fyk.ai.agent.tool.GrepTool;
import com.forever1996Fyk.ai.agent.tool.ToolMergeUtils;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/6/19 23:10
 **/
@Slf4j
@RestController
@RequestMapping("/agent")
public class AgentController implements InitializingBean {
    @Autowired
    private ChatModel chatModel;
    @Autowired
    private AgentTaskManager taskManager;
    @Autowired
    private AiSessionService sessionService;
    @Autowired
    private MinioService minioService;
    @Autowired
    private AiPptInstService pptInstService;
    @Autowired
    private AiPptTemplateService pptTemplateService;
    @Autowired
    private ImageGenerationService imageGenerationService;
    @Autowired
    private PptRenderService pptRenderService;

    @Autowired
    private FileContentTool fileContentTool;
    /**
     * 网页搜索工具回调
     */
    private ToolCallback[] webSearchToolCallbacks;

    @Value("${tavily.api-key}")
    private String tavilyApiKey;
    /**
     * Tavily MCP URL
     */
    @Value("${tavily.mcp-url}")
    private String tavilyMcpUrl;

    /**
     * Skills 目录路径
     */
    @Value("${skills.directory:}")
    private String skillsDirectory;

    /**
     * 接收用户查询并返回流式响应， 使用联网搜索获取信息
     *
     * @param query          用户查询
     * @param conversationId 会话ID
     * @return 流式响应
     */
    @GetMapping(value = "/chat/stream", produces = "text/event-stream;charset=UTF-8")
    @Operation(summary = "智能问答", description = "接收用户查询并返回流式响应， 使用联网搜索获取信息")
    public Flux<String> webSearchStream(
            @RequestParam String query,
            @RequestParam String conversationId
    ) {
        log.info("收到网页搜索请求：query={}, conversationId={}", query, conversationId);

        if (StringUtils.isBlank(query) || StringUtils.isBlank(StringUtils.trim(query))) {
            log.warn("查询参数为空或无效");
            return Flux.error(new IllegalArgumentException("查询参数不能为空"));
        }

        try {
            // 初始化WebSearchAgent
            WebSearchReactAgent agent = initWebSearchAgent();
            // 创建持久化记忆
            ChatMemory chatMemory = agent.createPersistentChatMemory(conversationId, 30);
            agent.setChatMemory(chatMemory);
            // 执行流式处理
            return agent.execute(conversationId, query);
        } catch (Exception e) {
            log.error("处理网页搜索请求时发生错误: ", e);
            return Flux.error(e);
        }
    }

    @GetMapping("/stop")
    @Operation(summary = "停止Agent执行", description = "停止指定会话的Agent执行，中断底层调用")
    public Map<String, Object> stopAgent(@RequestParam String conversationId) {
        log.info("收到停止请求: conversationId={}", conversationId);

        boolean success = taskManager.stopTask(conversationId);
        Map<String, Object> result = new HashMap<>();
        if (success) {
            result.put("success", true);
            result.put("message", "已停止执行");
        } else {
            result.put("success", false);
            result.put("message", "没有找到正在执行的任务或已停止");
        }
        return result;
    }


    @GetMapping(value = "/file/stream", produces = "text/event-stream;charset=UTF-8")
    @Operation(summary = "文件问答", description = "接收用户查询并返回流式响应，基于上传的文件内容进行问答")
    public Flux<String> fileStream(@RequestParam(required = true) String query,
                                   @RequestParam(required = true) String conversationId,
                                   @RequestParam(required = true) String fileId) {
        log.info("收到文件问答请求: query={}, conversationId={}, fileId={}", query, conversationId, fileId);

        if (query == null || query.trim().isEmpty()) {
            log.warn("查询参数为空或无效");
            return Flux.error(new IllegalArgumentException("查询参数不能为空"));
        }

        if (fileId == null || fileId.trim().isEmpty()) {
            log.warn("文件ID参数为空");
            return Flux.error(new IllegalArgumentException("文件ID不能为空"));
        }

        try {
            FileReactAgent fileReactAgent = initFileReactAgent();
            // 使用持久化记忆加载历史记录
            ChatMemory persistentMemory = fileReactAgent.createPersistentChatMemory(conversationId, 30);
            fileReactAgent.setChatMemory(persistentMemory);
            return fileReactAgent.stream(conversationId, query, fileId);
        } catch (Exception e) {
            log.error("处理文件问答请求时发生错误: ", e);
            return Flux.error(e);
        }
    }

    @GetMapping(value = "/pptx/stream", produces = "text/event-stream;charset=UTF-8")
    @Operation(summary = "PPT 生成", description = "接收用户需求并返回流式响应，基于模板驱动生成PPT")
    public Flux<String> pptxStream(@RequestParam(required = true) String query,
                                   @RequestParam(required = true) String conversationId) {
        log.info("收到PPT Builder请求: query={}, conversationId={}", query, conversationId);

        if (query == null || query.trim().isEmpty()) {
            log.warn("查询参数为空或无效");
            return Flux.error(new IllegalArgumentException("查询参数不能为空"));
        }

        try {
            PPTBuilderAgent pptBuilderAgent = initPPTBuilderAgent();
            // 使用持久化记忆加载历史记录
            ChatMemory persistentMemory = pptBuilderAgent.createPersistentChatMemory(conversationId, 30);
            pptBuilderAgent.setChatMemory(persistentMemory);
            return pptBuilderAgent.execute(conversationId, query);
        } catch (Exception e) {
            log.error("处理PPT Builder请求时发生错误: ", e);
            return Flux.error(e);
        }
    }

    @GetMapping(value = "/deep/stream", produces = "text/event-stream;charset=UTF-8")
    @Operation(summary = "深度研究", description = "接收用户查询并返回流式响应，使用计划-执行模式进行深度研究")
    public Flux<String> deepStream(@RequestParam(required = true) String query,
                                   @RequestParam(required = true) String conversationId) {
        log.info("收到深度研究请求: query={}, conversationId={}", query, conversationId);

        if (query == null || query.trim().isEmpty()) {
            log.warn("查询参数为空或无效");
            return Flux.error(new IllegalArgumentException("查询参数不能为空"));
        }

        try {
            PlanExecuteAgent planExecuteAgent = initPlanExecuteAgent();
            // 使用持久化记忆加载历史记录
            ChatMemory persistentMemory = planExecuteAgent.createPersistentChatMemory(conversationId, 30);
            planExecuteAgent.setChatMemory(persistentMemory);
            return planExecuteAgent.stream(conversationId, query);
        } catch (Exception e) {
            log.error("处理深度研究请求时发生错误: ", e);
            return Flux.error(e);
        }
    }

    @GetMapping(value = "/skills/stream", produces = "text/event-stream;charset=UTF-8")
    @Operation(summary = "Skills智能问答", description = "接收用户查询并返回流式响应，自动判断使用技能/搜索/文件等能力")
    public Flux<String> skillsStream(@RequestParam(required = true) String query,
                                     @RequestParam(required = true) String conversationId,
                                     @RequestParam(required = false) String fileId) {
        log.info("收到Skills请求: query={}, conversationId={}, fileId={}", query, conversationId, fileId);

        if (query == null || query.trim().isEmpty()) {
            log.warn("查询参数为空或无效");
            return Flux.error(new IllegalArgumentException("查询参数不能为空"));
        }

        try {
            // springai原生的方式
//            SkillsReactAgent skillsReactAgent = initSkillsReactAgent();
            // 手动构建的skills
            SkillsReactAgent skillsReactAgent = initManualSkillsReactAgent();
            ChatMemory persistentMemory = skillsReactAgent.createPersistentChatMemory(conversationId, 30);
            skillsReactAgent.setChatMemory(persistentMemory);
            return skillsReactAgent.stream(conversationId, query, fileId);
        } catch (Exception e) {
            log.error("处理Skills请求时发生错误: ", e);
            return Flux.error(e);
        }
    }

    /**
     * 初始化 Skills React Agent（手动开发方式）
     */
    private SkillsReactAgent initManualSkillsReactAgent() {
        log.info("初始化 Skills React Agent (manual)...");

        // 1. 通过 SkillConfig 配置技能目录，构建 SkillManager
        SkillConfig skillConfig = SkillConfig.builder()
                .addDirectory(skillsDirectory)
                .build();
        SkillManager skillManager = SkillManager.create(skillConfig);

        // 2. 将技能列表格式化为系统提示词的一部分
        String skillsPrompt = skillManager.formatPrompt();
        log.info("手动 Skills 模式：加载了 {} 个技能", skillManager.getSkillCount());

        // 3. 创建 ReadSkillTool 作为独立的工具回调
        ToolCallback readSkillTool = ReadSkillTool.create(skillManager.getRegistry());

        // 4. 合并工具：搜索 + 文件 + read_skill + 文件系统 + 搜索 + Bash
        ToolCallback[] allTools = ToolMergeUtils.mergeTools(
                webSearchToolCallbacks,
                ToolCallbacks.from(fileContentTool),
                new ToolCallback[]{readSkillTool},
                FileSystemTool.create(),
                GrepTool.create(),
                BashTool.create()
        );
        return SkillsReactAgent.builder()
                .name("manual-skills")
                .chatModel(chatModel)
                .tools(allTools)
                .systemPrompt(skillsPrompt)
                .sessionService(sessionService)
                .taskManager(taskManager)
                .maxRounds(10)
                .build();
    }


    private WebSearchReactAgent initWebSearchAgent() {
        log.info("初始化WebSearchReactAgent...");
        return WebSearchReactAgent.builder()
                .name("web react")
                .chatModel(chatModel)
                .tools(webSearchToolCallbacks)
                .sessionService(sessionService)
                .taskManager(taskManager)
                .maxRounds(5)
                .build();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        log.info("开始初始化工具ToolCallback");

        // 初始化网页搜索工具回调
        initWebSearchToolCallbacks();

        log.info("工具ToolCallback初始化完成");
    }

    private void initWebSearchToolCallbacks() {
        log.info("初始化网页搜索工具回调...");
        // tavily 搜索引擎
        String authorizationHeader = "Bearer " + tavilyApiKey;

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .header("Authorization", authorizationHeader);

        // 下面是MCP Client配置，可以用yaml配置替代
        HttpClientStreamableHttpTransport tavTransport = HttpClientStreamableHttpTransport.builder(tavilyMcpUrl)
                .requestBuilder(builder).build();
        McpSyncClient tavilyMcp = McpClient.sync(tavTransport)
                .requestTimeout(Duration.ofSeconds(300))
                .build();
        tavilyMcp.initialize();

        List<McpSyncClient> mcpSyncClients = List.of(tavilyMcp);
        SyncMcpToolCallbackProvider provider = SyncMcpToolCallbackProvider.builder()
                .mcpClients(mcpSyncClients)
                .build();

       this.webSearchToolCallbacks =  provider.getToolCallbacks();
        log.info("网页搜索工具回调初始化完成，工具数量: {}", webSearchToolCallbacks.length);
    }


    /**
     * 初始化文件问答 Agent
     */
    private FileReactAgent initFileReactAgent() {
        log.info("初始化文件问答 Agent...");

        List<ToolCallback> allTools = Arrays.asList(ToolCallbacks.from(fileContentTool));

        return FileReactAgent.builder()
                .name("file react")
                .chatModel(chatModel)
                .tools(allTools)
                .sessionService(sessionService)
                .taskManager(taskManager)
                .build();
    }

    /**
     * 初始化PPT Builder Agent
     */
    private PPTBuilderAgent initPPTBuilderAgent() {
        log.info("初始化PPT Builder Agent...");

        return new PPTBuilderAgent(
                chatModel,
                Arrays.asList(webSearchToolCallbacks),
                sessionService,
                pptInstService,
                pptTemplateService,
                taskManager,
                pptRenderService,
                imageGenerationService,
                minioService);
    }

    /**
     * 初始化 PlanExecute Agent
     */
    private PlanExecuteAgent initPlanExecuteAgent() {
        log.info("初始化 PlanExecute Agent...");

        return PlanExecuteAgent.builder()
                .chatModel(chatModel)
                .tools(webSearchToolCallbacks)
                .sessionService(sessionService)
                .taskManager(taskManager)
                .maxRounds(3)
                .build();
    }
}
