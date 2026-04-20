package com.forever1996Fyk.ai.model.claude;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.Base64ImageSource;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.ImageBlockParam;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.MessageParam;
import com.anthropic.models.messages.Model;
import com.anthropic.models.messages.TextBlockParam;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Base64;
import java.util.List;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/4/20 11:55
 **/
public class ClaudeVision {

    public static void main(String[] args) throws Exception {
        // 获取图像 URL 和媒体类型
        String imageUrl = "https://example.com/image.png";
        String imageMediaType = "image/png";

        // 下载并编码图像为 Base64
        String imageData = downloadAndEncodeImage(imageUrl);
        System.out.println(imageData);

        // 创建Anthropic客户端，使用 API KEY 认证
        AnthropicClient client = AnthropicOkHttpClient.builder()
                .apiKey(Config.getApiKey())
                .build();
        // 构建 IamgeBlockParam构建图像，包含 Base64图像数据和媒体数据
        ContentBlockParam imageBlock = ContentBlockParam.ofImage(
                ImageBlockParam.builder()
                        .source(Base64ImageSource.builder()
                                .data(imageData)
                                .mediaType(Base64ImageSource.MediaType.IMAGE_PNG).build()
                        ).build()
        );

        // 使用TextBlockParam构建文本，提供给 Claude 的分析指令
        ContentBlockParam textBlock = ContentBlockParam.ofText(
                TextBlockParam.builder()
                        .text("描述这样图片。")
                        .build()
        );

        // 将图像块和文本块包装为MessageParam
        List<ContentBlockParam> contentBlocks = List.of(imageBlock, textBlock);

        MessageParam param = MessageParam.builder()
                .role(MessageParam.Role.USER)
                .contentOfBlockParams(contentBlocks)
                .build();

        // 依旧创建MessageCreateParams指定模型和最大 token 数
        MessageCreateParams params = MessageCreateParams.builder()
                .model(Model.CLAUDE_HAIKU_4_5)
                .maxTokens(1024L)
                .addMessage(param)
                .build();
        Message message = client.messages().create(params);
        System.out.println(message);
    }

    /**
     * Download an image from a URL and encode it as base64.
     *
     * @param imageUrl imageUrl
     * @return  base64 encoded image data
     * @throws Exception if an error occurs during download or encoding
     */
    private static String downloadAndEncodeImage(String imageUrl) throws Exception {
        URL url = new URL(imageUrl);
        try (InputStream is = url.openStream(); ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            byte[] imageBytes = os.toByteArray();
            return Base64.getEncoder().encodeToString(imageBytes);
        }
    }
}
