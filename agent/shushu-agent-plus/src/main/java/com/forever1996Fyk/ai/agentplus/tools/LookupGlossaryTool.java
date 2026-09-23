package com.forever1996Fyk.ai.agentplus.tools;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * @program: AI-Learn
 * @description:
 * 业务术语口径查询工具，编程式注册。不走 @Tool 注解（description 必须编译时常量），
 * 改用 FunctionToolCallback 构建，把 YAML 术语列表拼进 description，让 LLM 直接看到所有术语名。
 * 用精确匹配而非向量检索：口径必须一术语一标准答案，模糊检索会出错。
 * @author: YuKai Fan
 * @create: 2026/9/22 17:08
 **/
public final class LookupGlossaryTool {

    private LookupGlossaryTool() {
    }

    /**
     * 工具描述模板，%s 为术语列表注入位置。
     */
    private static final String TOOL_DESCRIPTION_TEMPLATE = """
            查询业务术语的标准口径（含定义 + 可复用 SQL 片段 + 同义词）。
            
            何时使用本工具：
            - 用户问题涉及"活跃客户""VIP""热门影片""高消费客户""租金"等业务指标术语时
            - 用户问题涉及"近 N 个月""最近""本月""上月""今年"等相对时间时
              （sakila 是历史样本库，所有相对时间必须基于 MAX(rental_date) 而非 NOW()）
            
            <可用术语列表>
            %s
            </可用术语列表>
            
            传入上述术语名或其同义词即可命中。未命中会返回全部术语供你重试。
            """;

    /**
     * 工具入参。
     */
    public record GlossaryInput(
            @ToolParam(description = "要查的术语名或同义词，必传，如 \"活跃客户\"、\"VIP\"、\"近3个月\"、\"最近\"" ,required = true)
            String term) {
    }

    public static class GlossaryFunction implements Function<GlossaryInput, String> {
        @Override
        public String apply(GlossaryInput glossaryInput) {
            return "";
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        /**
         * 构建 ToolCallback，把术语列表拼进 description。
         */
        public ToolCallback build() {
            return FunctionToolCallback.builder("lookupGlossary", new GlossaryFunction())
                    .inputType(GlossaryInput.class)
                    .build();
        }
    }
}
