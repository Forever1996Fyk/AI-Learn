package com.forever1996Fyk.ai.intelligent.customer.rag.constant;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/6/4 09:59
 **/
public class MetadataKeyConstant {
    /**
     * 文件名称
     */
    public static final String FILE_NAME = "fileName";


    public static final String DOC_ID = "docId";

    public static final String CHUNK_ID = "chunkId";

    /**
     * 父块ID
     */
    public static final String PARENT_CHUNK_ID = "parentChunkId";

    /**
     * 同级块ID
     */
    public static final String BROTHER_CHUNK_ID = "brotherChunkId";

    /**
     * 头级别
     */
    public static final String HEADER_LEVEL = "headerLevel";


    /**
     * 跳过embedding标记，true表示不需要做embedding
     */
    public static final String SKIP_EMBEDDING = "skipEmbedding";

    /**
     * 文件地址
     */
    public static final String URL = "url";

    /**
     * 跳过重排序/融合标记，true表示该内容来自结构化查询（SQL/Cypher），不需要参与重排序和融合
     */
    public static final String SKIP_RERANK = "skipRerank";

    /**
     * 文件版本
     */
    public static final String VERSION = "version";

    /**
     * 访问权限
     */
    public static final String ACCESSIBLE_BY = "accessibleBy";

}
