package com.forever1996Fyk.ai.springai.rag.controller;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.forever1996Fyk.ai.springai.rag.embedding.EmbeddingService;
import com.forever1996Fyk.ai.springai.rag.processor.PdfMultiModalProcessor;
import com.forever1996Fyk.ai.springai.rag.service.DocumentCleaner;
import com.forever1996Fyk.ai.springai.rag.splitter.ModalTextSplitter;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.content.Media;
import org.springframework.ai.document.Document;
import org.springframework.ai.model.SimpleApiKey;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.generation.augmentation.QueryAugmenter;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/6/1 22:59
 **/
@RestController
@RequestMapping("/rag/image")
public class RagImageController {

    @Autowired
    private DashScopeChatModel chatModel;

    @Autowired
    private PdfMultiModalProcessor processor;
    @Autowired
    private EmbeddingService embeddingService;
    @Autowired
    private VectorStore vectorStore;


    @RequestMapping("/callWithOpenAI")
    public String callWithOpenAI() throws URISyntaxException, MalformedURLException {
        OpenAiChatOptions options = OpenAiChatOptions.builder().temperature(0.2d).model("qwen3-vl-plus").build();
        OpenAiChatModel multimodalChatModel = OpenAiChatModel.builder()
                .openAiApi(
                        OpenAiApi.builder()
                                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/")
                                .apiKey(new SimpleApiKey(System.getenv("dashscope.api-key")))
                                .build())
                .defaultOptions(options)
                .build();

        var imageData = new ClassPathResource("/RAG材料/image/demo.jpeg");

        List<Media> mediaList = List.of(
                new Media(MimeTypeUtils.IMAGE_JPEG,
                        imageData
                )
        );

        var userMessage = UserMessage.builder().text("请非常简要的描述一下你看到的这个图片?").media(mediaList).build();
        var response = multimodalChatModel.call(new Prompt(List.of(userMessage)));

        return response.getResult().getOutput().getText();
    }
    @RequestMapping("/callWithSpringAiAlibaba")
    public String callWithSpringAiAlibaba() throws URISyntaxException, MalformedURLException {
        var imageData = new ClassPathResource("/RAG材料/image/img.png");
        List<Media> mediaList = List.of(new Media(MimeTypeUtils.IMAGE_PNG, imageData));
        var userMessage = UserMessage.builder().text("请详细的描述一下你看到的这个图片?").media(mediaList).build();
        return chatModel.call(
                new Prompt(userMessage,
                        DashScopeChatOptions.builder()
                                .model("qwen3-vl-plus")
                                .multiModel(true)
                                .build()
                )).getResult().getOutput().getText();
    }
    @GetMapping("/process")
    public String process(String filePath) {
        try {
            return processor.processPdf(new File(filePath));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping("/processDoc")
    public List<Document> processDoc(String filePath) throws Exception {
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("文件不存在或不是有效文件: " + filePath);
        }
        if (!file.getName().toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("文件不是pdf格式: " + filePath);
        }
        // 1. 加载并处理多模态文档
        String fileStr = processor.processPdf(file);

        Document document = new Document(fileStr);

        List<Document> documents = List.of(document);

        // 2. 文本清洗 这里要注意防止把 image 标签清洗掉
//        documents = DocumentCleaner.cleanDocuments(documents);
        // 3. 文本切分
        ModalTextSplitter textSplitter = new ModalTextSplitter(300, 30);

        List<Document> apply = textSplitter.apply(documents);
        // 4. 向量化
        embeddingService.embedAndStore(apply);

        return apply;
    }

    @GetMapping("/chat")
    public String chat(String question) {
        VectorStoreDocumentRetriever documentRetriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .topK(5)
                .similarityThreshold(0.3)
                .build();

        QueryAugmenter queryAugmenter = ContextualQueryAugmenter.builder()
                .allowEmptyContext(true)
                .promptTemplate(new PromptTemplate("""
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
                {context}
                
                ## 用户问题:
                {query}
                
                注意：如果参考文档下面的内容为空，请直接回答“没有找到相关信息”。
                
                """))
                .build();
        RetrievalAugmentationAdvisor advisor = RetrievalAugmentationAdvisor.builder()
                .documentRetriever(documentRetriever)
                .queryAugmenter(queryAugmenter)
                .build();
        ChatClient chatClient = ChatClient.builder(chatModel).defaultAdvisors(advisor)
                .build();
        return chatClient.prompt(new Prompt(question)).call().content();
    }
}
