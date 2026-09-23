package com.forever1996Fyk.ai.agentplus.controller;

import com.forever1996Fyk.ai.agentplus.ShushuAgent;
import com.forever1996Fyk.ai.agentplus.auth.service.AuthService;
import com.forever1996Fyk.ai.agentplus.domain.dto.ChatRequest;
import com.forever1996Fyk.ai.agentplus.service.ConversationCoordinator;
import com.forever1996Fyk.ai.agentplus.service.LiveStreamRegistry;
import com.forever1996Fyk.ai.agentx.core.model.AgentStreamEvent;
import com.forever1996Fyk.ai.agentx.core.model.RunnableParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.UUID;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/9/21 11:10
 **/
@Slf4j
@RestController
@RequestMapping("/agent")
@RequiredArgsConstructor
public class AgentController {

    private final ShushuAgent shushuAgent;
    private final AuthService authService;

    private final LiveStreamRegistry liveStreams;

    /**
     * 多实例协调器（单实例模式不存在，getIfAvailable 返回 null）
     */
    private final ObjectProvider<ConversationCoordinator> coordinators;


    /**
     * 节点标识（端口），多实例部署时日志里可看出请求落在哪个实例
     */
    @Value("${server.port:0}")
    private String port;

    /**
     * 流式问答。
     * <p>
     * 执行与连接解耦：Agent 流注册进 LiveStreamRegistry 后台驱动，前端断开（关页/切页/断网）
     * 不影响执行；重连时带 recovery + startSeq 从断点续传（事件序号在 SSE 的 id 字段里）。
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<AgentStreamEvent>> stream(@RequestBody ChatRequest request) {
        String convId = StringUtils.hasText(request.conversationId())
                ? request.conversationId()
                : "shushu_conv_" + UUID.randomUUID();

        // 拦截器已校验登录，走到这里 userId 必非空；为空说明 SaTokenContext 异常，直接 fail-fast
        Long userId = authService.getCurrentUserIdOrNull();
        if (userId == null) {
            log.error("[dodo-agentx] 拦截器已通过但 userId 为空，SaTokenContext 异常 | convId={}", convId);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户身份获取失败，请重新登录");
        }

        String userIdStr = userId.toString();

        // 恢复请求：不新起执行，接回该会话进行中的流
        if (Boolean.TRUE.equals(request.recovery())) {
            requireActiveOwned(convId, userId);
            long startSeq = request.startSeq() == null ? 0 : request.startSeq();
            log.info("[dodo-agentx] 恢复流式: convId={}, userId={}, startSeq={}, node=:{}", convId, userId, startSeq, port);
            return liveStreams.attach(convId, startSeq);
        }

        // 执行中来了新消息（对齐豆包交互）：打断旧回答、丢弃断点，随后走全新执行
        if (liveStreams.isActive(convId)) {
            takeOverActiveStream(convId);
        }

        // 多实例模式：抢会话执行锁，防止请求路由到其他实例导致同一会话并行执行
        ConversationCoordinator coordinator = coordinators.getIfAvailable();
        if (coordinator != null && coordinator.tryLock(convId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "当前会话回答在其他节点进行中，请等待完成或先停止");
        }
        boolean online = Boolean.TRUE.equals(request.online());

        Flux<AgentStreamEvent> source;
        // 存在中断快照 → 恢复执行（本次 query 作为 resumeQuery 传入）
        if (shushuAgent.hasInterruptedState(convId)) {
            log.info("[dodo-agentx] 恢复中断会话: convId={}, userId={}, online={}, files={}, resumeQuery={}, node=:{}",
                    convId, userId, online,
                    request.fileIds() == null ? 0 : request.fileIds().size(),
                    request.query(), port);
            source = null;
        } else {
            // userId 进入 RunnableParams：用于记忆维度，也通过 toolParams 在工具执行前注入。
            RunnableParams params = RunnableParams.builder()
                    .conversationId(convId)
                    .userId(userIdStr)
                    .addToolParam("userId", userIdStr)
                    .build();
            log.info("[dodo-agentx] 收到查询: convId={}, userId={}, online={}, files={}, query={}, node=:{}",
                    convId, userId, online,
                    request.fileIds() == null ? 0 : request.fileIds().size(),
                    request.query(), port);
            source = shushuAgent.streamForResult(request.query(), params, online, request.fileIds());
        }

        // 多实例模式：锁挂在执行流终止上（HTTP 断开不影响），执行结束（含中断暂停）才释放
        if (coordinator != null) {
            source = source.doFinally(signal -> coordinator.unlock(convId));
        }

        return liveStreams.register(convId, userId, source);
    }

    /**
     * 恢复请求的前置校验：流存在、且当前用户是属主。
     */
    private void requireActiveOwned(String convId, Long userId) {
        if (!liveStreams.isActive(convId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "当前会话没有进行中的回答");
        }
        Long owner = liveStreams.ownerOf(convId);
        if (owner != null && !owner.equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权访问该会话的流");
        }
    }

    /**
     * 新消息顶掉进行中的回答（对齐豆包交互）：打断旧执行（本地打断失败说明执行在其他节点，
     * 广播过去），等旧流真正停下后删除打断产生的快照（断点已被新请求取代，历史里保留半截回答），
     * 随后走全新执行。极端情况旧执行没在超时内停下，回退为拒绝，防止同会话并行执行。
     */
    private void takeOverActiveStream(String convId) {
        boolean interrupted = shushuAgent.interrupt(convId);
        if (!interrupted) {
            ConversationCoordinator coordinator = coordinators.getIfAvailable();
            if (coordinator != null) {
                coordinator.broadcastInterrupt(convId);
            }
        }
        // 等待旧流真正停止（快照落库、active 摘除），最多 3 秒
        for (int i = 0; i < 30 && liveStreams.isActive(convId); i++) {
            try {
                Thread.sleep(Duration.ofMillis(100));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        if (liveStreams.isActive(convId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "当前会话回答尚未停止，请稍后重试");
        }

        shushuAgent.discardInterruptedState(convId);
        log.info("[dodo-agentx] 新消息顶掉进行中的回答: convId={}, node=:{}", convId, port);
    }
}
