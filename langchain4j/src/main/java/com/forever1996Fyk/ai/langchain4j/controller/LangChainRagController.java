package com.forever1996Fyk.ai.langchain4j.controller;

import com.forever1996Fyk.ai.langchain4j.service.LangChainAiService;
import com.forever1996Fyk.ai.langchain4j.service.LangChainMemoryAiService;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.injector.ContentInjector;
import dev.langchain4j.rag.content.injector.DefaultContentInjector;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.IngestionResult;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/6/1 22:15
 **/
@RestController
@RequestMapping("/langchain4j/rag")
public class LangChainRagController {
    @Autowired
    private OpenAiChatModel chatModel;

    private EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

    @GetMapping("/init")
    public String init(String filePath) {
        //1.加载文档
        Document document = FileSystemDocumentLoader.loadDocument(filePath, new ApacheTikaDocumentParser());

        // 2. 分割文档
        DocumentByParagraphSplitter splitter = new DocumentByParagraphSplitter(
                300,
                50
        );

        List<TextSegment> segmentList = splitter.split(document);

        OpenAiEmbeddingModel embeddingModel = OpenAiEmbeddingModel.builder()
                .modelName("text-embedding-v3")
                .dimensions(768)
                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1")
                .maxSegmentsPerBatch(9)
                .apiKey(System.getenv("langchain4j.dashscope.api-key"))
                .build();

        // 3. 生成embedding
        List<Embedding> embeddings = embeddingModel.embedAll(segmentList).content();

        // 4. 向量存储
        embeddingStore.addAll(embeddings, segmentList);
        return "success";
    }


    @GetMapping("/simplifyInit")
    public String simplifyInit(String filePath) {

        OpenAiEmbeddingModel embeddingModel = OpenAiEmbeddingModel.builder()
                .modelName("text-embedding-v3")
                .dimensions(768)
                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1")
                .maxSegmentsPerBatch(9)
                .apiKey(System.getenv("langchain4j.dashscope.api-key"))
                .build();

        // 直接一行代码实现文档读取，文档分割，向量化，向量存储
        IngestionResult result = EmbeddingStoreIngestor.builder()
                // 分割文档
                .documentSplitter(DocumentSplitters.recursive(300, 50))
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build()
                // 读取文档
                .ingest(FileSystemDocumentLoader.loadDocument(filePath, new ApacheTikaDocumentParser()));
        return result.toString();
    }

    @GetMapping("/retriever")
    public String retriever(String query, HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");
        OpenAiEmbeddingModel embeddingModel = OpenAiEmbeddingModel.builder()
                .modelName("text-embedding-v3")
                .dimensions(768)
                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1")
                .maxSegmentsPerBatch(9)
                .apiKey(System.getenv("langchain4j.dashscope.api-key"))
                .build();

        // 5. 创建向量检索器
        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .maxResults(5)
                .minScore(0.7)
                .build();

        ContentInjector contentInjector = DefaultContentInjector.builder()
                .promptTemplate(
                        new PromptTemplate("""
                                 ## 角色定位
                                 你是一位专业的RAG问答助手。请根据提供的上下文信息，详细、准确地回答用户的问题。如果参考文档没有内容，请务必不要胡编乱造，请直接说明"没有找到相关信息"。
                                 
                                 ## 任务要求：
                                 1. 请基于以下提供的参考文档内容，回答用户的问题。
                                 2. 如果参考文档中没有相关信息，请直接说明"没有找到相关信息"，不要编造内容。
                                 3. 如果有了参考文档内容，请务必尽量回答问题。有可能用户的输入比较随意，你可以先尝试回答用户的问题，猜测他的实际需求，先给出回复，你需要尽量去贴合用户的问题需求。
                                 
                                 ## 格式要求：
                                 1. 你的所有回答必须使用Markdown格式进行排版。
                                 2. 上下文信息中包含了图片描述标签，格式为：`<image src="URL" description="多模态描述"></image>`。
                                 3. 如果图片与用户提问高度相关，请将此标签转换为标准的Markdown图片格式 `![图片](URL)`。
                                 4. 仅在必要时包含图片，请注意千万不要输出重复的内容和图片，图片确保最终生成的URL不要重复。
                                 
                                 ## 参考文档:
                                 {{contents}}
                                 
                                 ## 用户问题:
                                 {{userMessage}}
                                 
                                 注意：如果参考文档下面的内容为空，请直接回答“没有找到相关信息”。
                                """)
                ).build();

        RetrievalAugmentor retrievalAugmentor = DefaultRetrievalAugmentor.builder()
                .contentRetriever(contentRetriever)
                .contentInjector(contentInjector)
                .build();

        // 7. 构建最终的AI服务
        LangChainAiService langChainAiService = AiServices.builder(LangChainAiService.class)
                .chatModel(chatModel)
                .retrievalAugmentor(retrievalAugmentor)
                .build();
        // 8.调用AI服务
        return langChainAiService.chat(query);
    }
}
