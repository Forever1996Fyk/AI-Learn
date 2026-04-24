package com.forever1996Fyk.ai.springai.chatclient.controller;

import com.forever1996Fyk.ai.springai.chatclient.pojo.Student;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/4/23 17:23
 **/
//@RestController
//@RequestMapping("/ai")
public class ChatClientController {

    private final ChatClient chatClient;

    public ChatClientController(ChatClient.Builder builder) {
        this.chatClient = builder.defaultSystem("你是聊天助手，名字叫做小智").build();
    }

    /**
     * 聊天
     */
    @GetMapping("/chat")
    public String chat(@RequestParam String message) {
        return chatClient.prompt("如果用户让你讲故事，智能讲解神话故事，不能讲解其他类型的故事")
                .user(message)
                .call()
                .content();
    }

    /**
     * 聊天流
     */
    @GetMapping("/chatStream")
    public Flux<String> chatStream(@RequestParam String message, HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");
        return chatClient.prompt("严格按照用户要求返回数据")
                .user(message)
                .stream()
                .content();
    }

    /**
     * 获取一个学生对象
     */
    @GetMapping("/getOneStudent")
    public Student getOneStudent() {
        return chatClient.prompt("严格按照用户要求返回数据")
                .user("生成一个 Stduent 对象，输出单个 JSON 格式对象：字段有 id(Long), name(String), age(Integer)")
                .call()
                .entity(Student.class);
    }


    /**
     * 获取多个学生对象
     */
    @GetMapping("/getStudentList")
    public List<Student> getStudentList() {
        return chatClient.prompt("严格按照用户要求返回数据")
                .user("生成3个 Stduent 对象，输出单个 JSON 格式对象：字段有 id(Long), name(String), age(Integer)")
                .call()
                .entity(new ParameterizedTypeReference<>() {});
    }
}
