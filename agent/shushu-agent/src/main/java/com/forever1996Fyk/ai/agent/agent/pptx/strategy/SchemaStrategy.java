package com.forever1996Fyk.ai.agent.agent.pptx.strategy;

import com.alibaba.fastjson2.JSON;
import com.forever1996Fyk.ai.agent.domain.FieldData;
import com.forever1996Fyk.ai.agent.domain.PptSchema;
import com.forever1996Fyk.ai.agent.domain.Slide;
import com.forever1996Fyk.ai.agent.enums.PptInstStatus;
import com.forever1996Fyk.ai.agent.prompts.PptBuilderPrompts;
import com.forever1996Fyk.ai.agent.repository.bean.AiPptInstEntity;
import com.forever1996Fyk.ai.agent.repository.bean.AiPptTemplateEntity;
import com.forever1996Fyk.ai.agent.util.ThinkTagParser;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.core.ParameterizedTypeReference;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * @program: AI-Learn
 * @description: Schema生成策略
 * @author: YuKai Fan
 * @create: 2026/6/24 23:50
 **/
@Slf4j
public class SchemaStrategy implements PptStateStrategy {

    /**
     * 目标状态: 渲染
     */
    private static final PptInstStatus TARGET_STATUS = PptInstStatus.RENDER;

    @Override
    public void execute(AiPptInstEntity inst, Sinks.Many<String> sink, String query, StringBuilder thinkingBuffer, PptStateStrategyContext context) {
        sink.tryEmitNext(context.createThinkingResponse("正在设计PPT详细内容...\n"));

        String templateCode = inst.getTemplateCode();
        AiPptTemplateEntity template = context.getPptTemplateService().getByCode(templateCode);
        String templateSchema = template.getTemplateSchema();
        String outline = inst.getOutline();

        String prompt = PptBuilderPrompts.getSchemaGenerationPrompt(templateSchema, outline);

        BeanOutputConverter<PptSchema> converter = new BeanOutputConverter<>(new ParameterizedTypeReference<>() {
        });

        // Mono异步任务，可以理解为线程池异步任务，只不过是Reactor式
        Disposable disposable = Mono.fromCallable(() -> {
                    String text = context.getChatModel().call(new Prompt(prompt)).getResult().getOutput().getText();
                    String json = ThinkTagParser.stripThinkTags(text);

                    PptSchema pptSchema = converter.convert(json);
                    String pptSchemaJson = JSON.toJSONString(pptSchema);

                    context.getPptInstService().updatePptSchema(inst.getId(), pptSchemaJson, TARGET_STATUS);


                    // 处理图片生成
                    processImageGeneration(pptSchema, sink, inst.getConversationId(), context);

                    // 更新包含图片URL的schema
                    context.getPptInstService().updatePptSchema(inst.getId(), JSON.toJSONString(pptSchema), TARGET_STATUS);
                    context.continueStateMachine(inst, sink, query, thinkingBuffer);
                    return null;
                })
                .doOnError(err -> {
                    log.error("Schema生成异常", err);
                    // 失败时不回退状态，只更新错误信息，转到 FAILED
                    context.getPptInstService().updateError(inst.getId(),
                            "Schema生成失败: " + err.getMessage(), PptInstStatus.SCHEMA);
                    // 转到 FAILED 策略
                    PptStateStrategyFactory.getInstance().executeFailedState(inst, sink, query, thinkingBuffer, context);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe();
        // 添加到任务管理器，用于停止任务
        context.setDisposable(inst.getConversationId(), disposable);
    }

    @Override
    public PptInstStatus getTargetStatus() {
        return TARGET_STATUS;
    }

    /**
     * 处理图片生成
     */
    private void processImageGeneration(PptSchema pptSchema, Sinks.Many<String> sink, String conversationId,
                                        PptStateStrategyContext context) {
        if (pptSchema.getSlides() == null) {
            return;
        }

        List<ImageGenerationTask> tasks = Lists.newArrayList();
        List<Slide> slides = pptSchema.getSlides();
        for (Slide slide : slides) {
            Map<String, FieldData> data = slide.getData();
            if (MapUtils.isEmpty(data)) {
                continue;
            }
            for (Map.Entry<String, FieldData> entry : data.entrySet()) {
                String key = entry.getKey();
                FieldData fieldData = entry.getValue();
                if (fieldData == null) {
                    continue;
                }
                String type = fieldData.getType();
                // 只处理image和background类型
                if (!"image".equalsIgnoreCase(type) && !"background".equalsIgnoreCase(type)) {
                    continue;
                }
                // 如果url已经有值，跳过
                if (StringUtils.isNotBlank(fieldData.getUrl())) {
                    continue;
                }
                // url为空，需要用content作为提示词生成图片
                String prompt = fieldData.getContent();
                if (StringUtils.isBlank(prompt)) {
                    continue;
                }
                tasks.add(new ImageGenerationTask(key, fieldData, prompt, slide));
            }
        }
        if (CollectionUtils.isEmpty(tasks)) {
            return;
        }

        int size = tasks.size();
        sink.tryEmitNext(context.createThinkingResponse("PPT内容设计完成，开始生成图片素材\n"));
        sink.tryEmitNext(context.createThinkingResponse("共需生成 " + size + " 张图片，开始生成...\n"));
        for (int i = 0; i < size; i++) {
            ImageGenerationTask task = tasks.get(i);
            int current = i + 1;
            sink.tryEmitNext(context.createThinkingResponse("正在生成图片 (" + current + "/" + size + ")... \n"));

            try {
                // 调用图片生成服务（文生图模型）
                String originalImageUrl = context.getImageGenerationService().generateImage(task.prompt);
                // 下载图片并上传到MinIO
                byte[] imageBytes = downloadImageFromUrl(originalImageUrl);

                if (imageBytes != null && imageBytes.length > 0) {
                    // 上传到MinIO
                    // 上传到MinIO
                    String objectName = "ppt/" + conversationId + "/images/" + System.currentTimeMillis() + "_" + (i + 1) + ".png";
                    String minioUrl = context.getMinioService().uploadFile(objectName, imageBytes, "image/png");

                    // 更新schema中的url为MinIO地址
                    task.fieldData.setUrl(minioUrl);

                    sink.tryEmitNext(context.createThinkingResponse("✅ 图片生成完成 (" + current + "/" + size + ")\n"));
                    log.info("图片已上传到MinIO: {} -> {}", task.key, minioUrl);
                } else {
                    throw new RuntimeException("图片下载失败");
                }
            } catch (Exception e) {
                log.error("图片生成或上传失败: {}", task.prompt, e);
                sink.tryEmitNext(context.createThinkingResponse("⚠ 图片生成失败 (" + current + "/" + size + "): \n" + task.key));
                // 使用空字符串
                task.fieldData.setUrl("");
            }
        }
        sink.tryEmitNext(context.createThinkingResponse("✅ 所有图片生成完成\n"));
        sink.tryEmitNext(context.createThinkingResponse("✅素材准备就绪，开始渲染PPT\n"));
    }

    /**
     * 从URL下载图片
     */
    private byte[] downloadImageFromUrl(String imageUrl) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(imageUrl))
                .GET()
                .build();

        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

        if (response.statusCode() == 200) {
            return response.body();
        } else {
            throw new RuntimeException("下载图片失败，状态码: " + response.statusCode());
        }
    }

    /**
     * 执行 Schema 策略，支持修改模式
     *
     * @param inst           PPT 实例
     * @param sink           输出 sink
     * @param question       用户查询
     * @param thinkingBuffer 思考缓冲
     * @param context        策略上下文
     * @param modifyPrompt   修改提示词，如果为 null 表示正常流程
     */
    public void executeWithModifyPrompt(AiPptInstEntity inst, Sinks.Many<String> sink, String question, StringBuilder thinkingBuffer, PptStateStrategyContext context, String modifyPrompt) {
        sink.tryEmitNext(context.createThinkingResponse("正在重新生成PPT详细内容...\n"));
        BeanOutputConverter<PptSchema> converter = new BeanOutputConverter<>(new ParameterizedTypeReference<>() {
        });

        Disposable disposable = Mono.fromCallable(() -> {
                    String json = ThinkTagParser.stripThinkTags(
                            context.getChatModel().call(new Prompt(modifyPrompt)).getResult().getOutput().getText());
                    PptSchema pptSchema = converter.convert(json);
                    String pptSchemaJson = JSON.toJSONString(pptSchema);

                    context.getPptInstService().updatePptSchema(inst.getId(), pptSchemaJson, TARGET_STATUS);

                    // 处理图片生成
                    processImageGeneration(pptSchema, sink, inst.getConversationId(), context);

                    // 更新包含图片URL的schema
                    context.getPptInstService().updatePptSchema(inst.getId(), JSON.toJSONString(pptSchema), TARGET_STATUS);
                    context.continueStateMachine(inst, sink, question, thinkingBuffer);
                    return null;
                })
                .doOnError(err -> {
                    log.error("Schema生成异常", err);
                    // 失败时不回退状态，只更新错误信息，转到 FAILED
                    context.getPptInstService().updateError(inst.getId(),
                            "Schema生成失败: " + err.getMessage(), PptInstStatus.SCHEMA);
                    // 转到 FAILED 策略
                    PptStateStrategyFactory.getInstance().executeFailedState(inst, sink, question, thinkingBuffer, context);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe();

        // 保存 disposable 到任务管理器，用于停止任务
        context.setDisposable(inst.getConversationId(), disposable);
    }


    /**
     * 图片生成任务
     */
    private static class ImageGenerationTask {
        String key;
        FieldData fieldData;
        String prompt;
        Slide slide;

        ImageGenerationTask(String key, FieldData fieldData, String prompt, Slide slide) {
            this.key = key;
            this.fieldData = fieldData;
            this.prompt = prompt;
            this.slide = slide;
        }
    }
}
