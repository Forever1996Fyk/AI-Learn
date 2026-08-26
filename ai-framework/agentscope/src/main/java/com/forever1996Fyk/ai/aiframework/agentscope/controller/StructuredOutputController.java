package com.forever1996Fyk.ai.aiframework.agentscope.controller;

import com.alibaba.fastjson2.JSON;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.DashScopeChatModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/8/26 22:45
 **/
@RestController
@RequestMapping("/structured")
public class StructuredOutputController {

    private final static String api_key = System.getenv("dashscope.api-key");

    @GetMapping("/chat")
    public String chat() {
        ReActAgent agent = ReActAgent.builder()
                .name("AnalysisAgent")
                .sysPrompt("You are an intelligent analysis assistant. Analyze user requests and provide structured responses.")
                .model(
                        DashScopeChatModel.builder()
                                .apiKey(api_key)
                                .modelName("qwen-max")
                                .build()
                )
                .build();

        // 提取联系人信息
        ContactInfo contactInfo = extractContactInfo(agent);
        System.out.println("Name: " + contactInfo.name);
        System.out.println("Email: " + contactInfo.email);
        System.out.println("Phone: " + contactInfo.phone);
        System.out.println("Company: " + contactInfo.company);
        return JSON.toJSONString(contactInfo);
    }

    private static ContactInfo extractContactInfo(ReActAgent agent) {
        Msg userMsg = Msg.builder().role(MsgRole.USER).content(TextBlock.builder().text("Extract contact info: Please contact YK at michaelkai@aliyun.com, " + "phone +1-555-1234, company SuperMichael.").build()).build();

        Msg result = agent.call(userMsg, ContactInfo.class).block();
        return result.getStructuredData(ContactInfo.class);
    }

    /**
     * 联系人信息
     */
    public static class ContactInfo {
        public String name;
        public String email;
        public String phone;
        public String company;
    }
}
