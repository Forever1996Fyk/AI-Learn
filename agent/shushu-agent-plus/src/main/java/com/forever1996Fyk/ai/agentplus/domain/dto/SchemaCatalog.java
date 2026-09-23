package com.forever1996Fyk.ai.agentplus.domain.dto;

import java.util.List;

/**
 * @program: AI-Learn
 * @description: schema 字典的内存模型（由 shushu_agent_plus.yml 加载）。record 嵌套聚合在一个文件，与 YAML 结构一一对应。
 * @author: YuKai Fan
 * @create: 2026/9/22 17:12
 **/
public final class SchemaCatalog {

    /**
     * 业务术语标准口径。
     */
    public record GlossaryDesc(String term, String description, String sqlFragment,
                               List<String> synonyms) {
        public List<String> synonyms() {
            return synonyms == null ? List.of() : synonyms;
        }
    }
}
