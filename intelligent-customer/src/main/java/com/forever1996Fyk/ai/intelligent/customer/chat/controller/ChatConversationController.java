package com.forever1996Fyk.ai.intelligent.customer.chat.controller;

import com.forever1996Fyk.ai.intelligent.customer.auth.service.AuthService;
import com.forever1996Fyk.ai.intelligent.customer.chat.repository.bean.ChatConversationEntity;
import com.forever1996Fyk.ai.intelligent.customer.chat.service.ChatConversationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/6/5 23:41
 **/
@RestController
@RequestMapping("/chat/conversation")
public class ChatConversationController {
    @Autowired
    private AuthService authService;
    @Autowired
    private ChatConversationService chatConversationService;

    /**
     * 获取用户的会话列表
     *
     * @return 会话列表
     */
    @GetMapping("/list")
    public Map<String, Object> getConversationList() {
        String userId = authService.getCurrentUserId();
        List<ChatConversationEntity> conversations = chatConversationService.getConversationsByUserId(userId);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", conversations);
        result.put("total", conversations.size());
        return result;
    }


    /**
     * 根据会话ID获取会话详情
     *
     * @param conversationId 会话ID
     * @return 会话信息
     */
    @GetMapping("/detail")
    public Map<String, Object> getConversationDetail(@RequestParam String conversationId) {
        ChatConversationEntity conversation = chatConversationService.getByConversationId(conversationId);

        Map<String, Object> result = new HashMap<>();
        if (conversation != null) {
            result.put("success", true);
            result.put("data", conversation);
        } else {
            result.put("success", false);
            result.put("message", "会话不存在");
        }
        return result;
    }

    /**
     * 创建新会话
     *
     * @param userId 用户ID
     * @param title  会话标题（可选）
     * @return 会话ID
     */
    @PostMapping("/create")
    public Map<String, Object> createConversation(@RequestParam(required = false) String title) {
        String userId = authService.getCurrentUserId();
        String conversationId = chatConversationService.createConversation(userId, title);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("conversationId", conversationId);
        result.put("message", "会话创建成功");
        return result;
    }

    /**
     * 归档会话
     *
     * @param conversationId 会话ID
     * @return 操作结果
     */
    @PutMapping("/archive")
    public Map<String, Object> archiveConversation(@RequestParam String conversationId) {
        boolean success = chatConversationService.archiveConversation(conversationId);

        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("message", success ? "会话已归档" : "归档失败");
        return result;
    }

    /**
     * 删除会话
     *
     * @param conversationId 会话ID
     * @return 操作结果
     */
    @DeleteMapping("/delete")
    public Map<String, Object> deleteConversation(@RequestParam String conversationId) {
        boolean success = chatConversationService.deleteConversation(conversationId);

        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("message", success ? "会话已删除" : "删除失败");
        return result;
    }
}
