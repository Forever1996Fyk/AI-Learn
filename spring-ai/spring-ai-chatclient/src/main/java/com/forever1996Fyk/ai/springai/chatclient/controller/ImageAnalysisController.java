package com.forever1996Fyk.ai.springai.chatclient.controller;

import com.forever1996Fyk.ai.springai.chatclient.pojo.IdCard;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.MimeType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.function.Consumer;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/4/24 08:39
 **/
@RestController
@RequestMapping("/ai")
public class ImageAnalysisController {

    @Qualifier("zhipuaiClient")
    @Autowired
    private ChatClient zhipuaiClient;

    @GetMapping("/imageAnalyze")
    public String imageAnalyze() {
        ClassPathResource resource = new ClassPathResource("pubg.jpeg");
        String result = zhipuaiClient.prompt("你是一个图像识别专家，可以对用户关于图片识别问题进行精准回复")
                .user(new Consumer<ChatClient.PromptUserSpec>() {
                    @Override
                    public void accept(ChatClient.PromptUserSpec promptUserSpec) {
                        promptUserSpec.text("请对图片进行详细的描述")
                                .media(MimeType.valueOf("image/jpeg"), resource);
                    }
                })
                .call()
                .content();
        return result;
    }

    @GetMapping("/idCardAnalyze")
    public IdCard idCardAnalyze() {
        ClassPathResource resource = new ClassPathResource("idCard.png");
        IdCard idCard = zhipuaiClient.prompt("""
                        你是一个图像识别专家，可以对用户关于图片识别问题进行精准回复, 以JSON格式输出识别到的身份证中姓名(name)、性别(sex)、民族(nation)、出生日期(birth)、地址(address)、身份证号(idNo)
                        """)
                .user(new Consumer<ChatClient.PromptUserSpec>() {
                    @Override
                    public void accept(ChatClient.PromptUserSpec promptUserSpec) {
                        promptUserSpec.text("请对图片进行详细的描述")
                                .media(MimeType.valueOf("image/png"), resource);
                    }
                })
                .call()
                .entity(IdCard.class);
        return idCard;
    }
}
