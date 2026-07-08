package com.forever1996Fyk.ai.intelligent.customer.rag.modules.retriever;

import com.forever1996Fyk.ai.intelligent.customer.rag.util.ContentUtil;
import dev.langchain4j.experimental.rag.content.retriever.sql.SqlDatabaseContentRetriever;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/7/9 00:19
 **/
@Slf4j
public class IntelligentCustomerSqlDatabaseContentRetriever implements ContentRetriever {
    private final SqlDatabaseContentRetriever sqlDatabaseContentRetriever;
    private final ContentRetriever fallbackRetriever;

    public IntelligentCustomerSqlDatabaseContentRetriever(SqlDatabaseContentRetriever sqlDatabaseContentRetriever, ContentRetriever fallbackRetriever) {
        this.sqlDatabaseContentRetriever = sqlDatabaseContentRetriever;
        this.fallbackRetriever = fallbackRetriever;
    }

    @Override
    public List<Content> retrieve(Query query) {
        List<Content> results;
        try {
            String newQuery = "我的问题是：" + query.text() + ", 我的用户Id是: 123321" + ", 现在是：" + LocalDateTime.now();
            query = new Query(newQuery, query.metadata());
            results = sqlDatabaseContentRetriever.retrieve(query);
        } catch (Exception e) {
            log.warn("SQL 检索异常，降级使用知识库检索, query: {}", query.text(), e);
            return fallbackRetriever.retrieve(query);
        }

        if (results == null || results.isEmpty() || isSqlResultEmpty(results)) {
            log.info("SQL 检索结果为空，降级使用知识库检索, query: {}", query.text());
            return fallbackRetriever.retrieve(query);
        }

        // SQL 结构化查询结果直接透传，不参与后续重排序/融合
        return results.stream()
                .map(ContentUtil::markAsSkipRerank)
                .collect(Collectors.toList());
    }

    /**
     * 判断 SQL 查询结果是否实际为空
     * <p>
     * SqlDatabaseContentRetriever 在查询无数据时不会返回空 list，
     * 而是返回形如：
     * <pre>
     * Result of executing '...SQL...':
     * column1,column2
     * </pre>
     * 即只包含列名头部，没有实际数据行。
     * <p>
     * 通过定位最后一个 "':\n" 标记（SQL 语句描述结束位置），
     * 判断列名行之后是否存在实际数据行，避免 SQL 语句本身含换行符导致误判。
     */
    private boolean isSqlResultEmpty(List<Content> results) {
        if (results.size() != 1) {
            return false;
        }
        String text = results.getFirst().textSegment().text();
        if (!text.startsWith("Result of executing '")) {
            return false;
        }
        // ":\n" 标记列名开始，列名后的第一个 "\n" 标记数据开始
        int columnStartIndex = text.indexOf(":\n");
        if (columnStartIndex == -1) {
            return false;
        }
        // ":\n" 之后是列名行，找列名行结束的 "\n"（即数据开始位置）
        int dataStartIndex = text.indexOf('\n', columnStartIndex + 2);
        // 列名后没有换行符，或换行符后无实际内容，则表示无数据
        return dataStartIndex == -1 || text.substring(dataStartIndex + 1).trim().isEmpty();
    }


    /**
     * 获取内部的 SqlDatabaseContentRetriever 实例
     */
    public SqlDatabaseContentRetriever getSqlDatabaseContentRetriever() {
        return sqlDatabaseContentRetriever;
    }

    /**
     * 获取兜底的知识库检索器
     */
    public ContentRetriever getFallbackRetriever() {
        return fallbackRetriever;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private DataSource dataSource;
        private PromptTemplate promptTemplate;
        private String databaseStructure;
        private ChatModel chatModel;
        private ContentRetriever fallbackRetriever;

        public Builder dataSource(DataSource dataSource) {
            this.dataSource = dataSource;
            return this;
        }

        public Builder promptTemplate(PromptTemplate promptTemplate) {
            this.promptTemplate = promptTemplate;
            return this;
        }

        public Builder databaseStructure(String databaseStructure) {
            this.databaseStructure = databaseStructure;
            return this;
        }

        public Builder chatModel(ChatModel chatModel) {
            this.chatModel = chatModel;
            return this;
        }

        public Builder fallbackRetriever(ContentRetriever fallbackRetriever) {
            this.fallbackRetriever = fallbackRetriever;
            return this;
        }

        public IntelligentCustomerSqlDatabaseContentRetriever build() {
            SqlDatabaseContentRetriever sqlRetriever = SqlDatabaseContentRetriever.builder()
                    .dataSource(dataSource)
                    .promptTemplate(promptTemplate)
                    .databaseStructure(databaseStructure)
                    .chatModel(chatModel)
                    .build();
            return new IntelligentCustomerSqlDatabaseContentRetriever(sqlRetriever, fallbackRetriever);
        }
    }
}
