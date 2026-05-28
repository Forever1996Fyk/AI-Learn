package com.forever1996Fyk.ai.springai.rag.rewriter;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.fastjson2.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/5/28 22:15
 **/
@Service
public class QuestionRewriteService {
    private static final Logger log = LoggerFactory.getLogger(QuestionRewriteService.class);
    @Autowired
    private DashScopeChatModel chatModel;

    /**
     * 问题分解提示词
     */
    private static final String DECOMPOSE_PROMPT = """
            # 角色
            你是一名专业的查询逻辑分析专家。
            
            # 任务
            将给定的“用户原始问题”分解为一系列**相互独立、逻辑清晰**，且可单独用于检索的子查询列表。
            你的输出必须是一个标准的JSON数组格式。
            
            # 用户原始问题
            {QUESTION}
            
            # 输出格式要求 (JSON Array)
            [
              "子查询1",
              "子查询2",
              "子查询3",
              "..."
            ]
            （不强制要求数组元素个数，可根据真实情况输出，至少保留1个）
            
            # 输出
            请直接输出JSON数组，不要包含解释或多余的文字。
            """;

    /**
     * 富化提示词
     */
    private final static String ENRICHMENT_PROMPT = """
            # 角色
            你是一个专业的问题重写优化器。
            
            # 任务
            根据提供的“对话历史”和“用户原始问题”，重写为一个独立、完整、且包含所有必要背景信息的新查询，用于RAG检索。
            
            ## 对话历史
            {CHAT_HISTORY}
            
            ## 原始问题：
            {QUESTION}
            
            # 输出
            输出富化过后的新问题，不要包含多余的解释性内容
            """;

    /**
     * 多样化提示词
     */
    private final static String DIVERSIFY_PROMPT = """
            # 角色
            你是一名专业的语义扩展专家。
            
            # 任务
            为给定的“原始问题”生成**3个**语义相同但**措辞完全不同、且利于检索**的查询变体，以提高检索的召回率。
            你的输出必须是一个标准的JSON数组格式。
            
            # 原始问题
            {QUESTION}
            
            # 输出格式要求 (JSON Array)
            [
              "变体1",
              "变体2",
              "变体3"
            ]
            
            # 输出
            输出富化过后的新问题，不要包含多余的解释性内容
            """;

    /**
     * 回溯提示
     */
    private final static String STEP_BACK = """
            # 角色
            你是一个擅长抽象思维和原理推理的专家。
            
            # 任务
            请根据用户提出的具体问题，先“后退一步”，将其转化为一个更通用、更本质的问题，聚焦于背后的原理、规律、概念或一般性知识，而不是具体细节。
            
            # 原始问题
            {QUESTION}
            
            # 输出
            请只输出改写后的“后退问题”，不要解释，不要包含原始问题，也不要回答它。
            """;

    private final static String QUESTION = "QUESTION";
    private final static String CHAT_HISTORY = "CHAT_HISTORY";
    public List<String> decompose(String question) {
        PromptTemplate promptTemplate = new PromptTemplate(DECOMPOSE_PROMPT);
        promptTemplate.add(QUESTION, question);

        String result = chatModel.call(promptTemplate.create()).getResult().getOutput().getText();
        return JSON.parseArray(result, String.class);
    }

    public String enrichment(String chatHistory, String question) {
        PromptTemplate promptTemplate = new PromptTemplate(ENRICHMENT_PROMPT);
        promptTemplate.add(CHAT_HISTORY, chatHistory);
        promptTemplate.add(QUESTION, question);

        return chatModel.call(promptTemplate.create()).getResult().getOutput().getText();
    }

    public List<String> diversify(String question) {
        PromptTemplate promptTemplate = new PromptTemplate(DIVERSIFY_PROMPT);
        promptTemplate.add(QUESTION, question);

        String result = chatModel.call(promptTemplate.create()).getResult().getOutput().getText();
        return JSON.parseArray(result, String.class);
    }

    public String stepBack(String question) {
        PromptTemplate promptTemplate = new PromptTemplate(STEP_BACK);
        promptTemplate.add("QUESTION", question);

        return chatModel.call(promptTemplate.create()).getResult().getOutput().getText();
    }

    // 组合方法
    public List<String> rewriteQuery(String query) {
        log.info("===========进入问题重写组合策略流程===========");
        log.info("原始问题: {}", query);

        //回退
        String stepBackQuery = this.stepBack(query);

        // 分解
        List<String> decomposedQueries = this.decompose(stepBackQuery);

        // 多样化
        List<String> finalQueries = new ArrayList<>();
        for (String subQuery : decomposedQueries) {
            List<String> variations = this.diversify(subQuery);
            finalQueries.addAll(variations);
        }

        if (finalQueries.isEmpty()) {
            finalQueries.add(query);
        }

        log.info("===========组合重写完成，最终查询列表: {} ===========", finalQueries);
        return finalQueries;
    }
}
