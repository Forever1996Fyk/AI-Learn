package com.forever1996Fyk.ai.langchain4j.controller;

import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.print.attribute.standard.Media;
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

    @RequestMapping("/callWithOpenAI")
    public String callWithOpenAI() throws URISyntaxException, MalformedURLException {
        OpenAiChatOptions options = OpenAiChatOptions.builder().temperature(0.2d).model("qwen3-vl-plus").build();
        OpenAiChatModel multimodalChatModel = OpenAiChatModel.builder().openAiApi(OpenAiApi.builder().baseUrl("https://dashscope.aliyuncs.com/compatible-mode/").apiKey(new SimpleApiKey("sk-8ef405c4686e456e91f6698272253126")).build()).defaultOptions(options).build();

        List<Media> mediaList = List.of(new Media(MimeTypeUtils.IMAGE_PNG, new URI("https://cdn.nlark.com/yuque/0/2025/png/5378072/1762350625634-664f1db7-e1c9-4daa-ab8e-81b6b7da5a68.png").toURL().toURI()));

        var userMessage = UserMessage.builder().text("请非常简要的描述一下你看到的这个图片?").media(mediaList).build();
        var response = multimodalChatModel.call(new Prompt(List.of(userMessage)));

        return response.getResult().getOutput().getText();
    }
}
