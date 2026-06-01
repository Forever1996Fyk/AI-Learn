package com.forever1996Fyk.ai.springai.rag.controller;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.forever1996Fyk.ai.springai.rag.embedding.EmbeddingService;
import com.forever1996Fyk.ai.springai.rag.processor.PdfMultiModalProcessor;
import com.forever1996Fyk.ai.springai.rag.service.DocumentCleaner;
import com.forever1996Fyk.ai.springai.rag.splitter.ModalTextSplitter;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.document.Document;
import org.springframework.ai.model.SimpleApiKey;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
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
}
