package com.forever1996Fyk.ai.intelligent.customer.document.service.impl;

import com.alibaba.fastjson2.JSON;
import com.forever1996Fyk.ai.intelligent.customer.document.constant.ContentType;
import com.forever1996Fyk.ai.intelligent.customer.document.enums.DocumentStatus;
import com.forever1996Fyk.ai.intelligent.customer.document.repository.bean.KnowledgeDocumentEntity;
import com.forever1996Fyk.ai.intelligent.customer.document.service.FileProcessService;
import com.forever1996Fyk.ai.intelligent.customer.document.service.FileStorageService;
import com.forever1996Fyk.ai.intelligent.customer.document.service.KnowledgeDocumentService;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.Assert;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/6/3 11:25
 **/
@Slf4j
public abstract class MinerUFileProcessBaseServiceImpl implements FileProcessService {
    private static final String CONVERTED_FILE_DIR = "converted/";
    @Autowired
    private FileStorageService fileStorageService;
    @Autowired
    private KnowledgeDocumentService knowledgeDocumentService;

    @Value("${file.parse.api.url:http://localhost:8000}")
    private String fileParseApiUrl;

    @Value("${file.parse.api.connectTimeout:600000}")
    private int connectTimeout;

    @Value("${file.parse.api.responseTimeout:600000}")
    private int responseTimeout;

    /**
     * Process the document.
     * <p>
     * 1. 从 MinIO下载文件
     * 2. 调用文档解析接口获取 md/zip
     * 3. 转换后的文档保存在 MiniIO上
     * 4. 更新文档状态和转换后的 URL
     *
     * @param document    the document entity
     * @param inputStream the input stream of the document
     */
    @Override
    public String processDocument(KnowledgeDocumentEntity document, InputStream inputStream) {
        return processDocumentToMarkdownFromZip(document, inputStream);
    }

    /**
     * Process the document to Markdown.
     *
     * @param document    the document entity
     * @param inputStream the input stream of the document
     */
    public void processDocumentToMarkdown(KnowledgeDocumentEntity document, InputStream inputStream) {
        log.info("开始处理文档转换为 ZIP，documentId: {}", document.getDocTitle());
        // 更新状态为转换中
        document.setStatus(DocumentStatus.CONVERTING);
        boolean result = knowledgeDocumentService.updateById(document);
        Assert.isTrue(result, "更新文档状态CONVERTING失败");
        try {
            // 生成一串数字，避免文件名中文乱码
            String docTitle = document.getDocTitle() + document.getDocTitle().hashCode();

            // 调用文档解析获取 Markdown
            String parseResult = parseDocumentToMarkdown(docTitle, inputStream);

            String markdownContent = JSON.parseObject(parseResult)
                    .getJSONObject("results")
                    .getJSONObject(docTitle)
                    .getString("md_content");
            // 保存转换后的内容到 MinIO
            String convertedObjectName = CONVERTED_FILE_DIR + document.getDocTitle().substring(0, document.getDocTitle().lastIndexOf(".")) + ".md";
            String convertedUrl = fileStorageService.uploadFile(convertedObjectName, markdownContent.getBytes(), ContentType.TEXT_MARKDOWN);

            // 更新文档状态为已转换
            document.setStatus(DocumentStatus.CONVERTED);
            document.setConvertedDocUrl(convertedUrl);
            result = knowledgeDocumentService.updateById(document);
            Assert.isTrue(result, "更新文档状态CONVERTED失败");
            log.info("文档 Markdown 转换完成，documentId:{}, docTitle:{}", document.getDocId(), document.getDocTitle());
        } catch (Exception e) {
            log.error("文档 Markdown 转换失败，documentId:{}, docTitle:{}", document.getDocId(), document.getDocTitle(), e);
            document.setStatus(DocumentStatus.UPLOADED);
            result = knowledgeDocumentService.updateById(document);
            Assert.isTrue(result, "更新文档状态UPLOADED失败");
            throw new RuntimeException(String.format("文档 Markdown 转换失败: %s", e.getMessage()), e);
        } finally {
            closeQuietly(inputStream);
        }
    }

    private String parseDocumentToMarkdown(String fileName, InputStream inputStream) {
        String url = fileParseApiUrl + "/file_parse";
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofMicroseconds(connectTimeout))
                .setResponseTimeout(Timeout.ofMilliseconds(responseTimeout))
                .build();
        try (CloseableHttpClient httpClient = HttpClients.custom().setDefaultRequestConfig(requestConfig).build()) {
            HttpPost httpPost = new HttpPost(url);
            httpPost.setHeader("Accept", "application/json");

            try (HttpEntity httpEntity = MultipartEntityBuilder.create()
                    .addBinaryBody("files", inputStream, org.apache.hc.core5.http.ContentType.APPLICATION_OCTET_STREAM, fileName)
                    .addTextBody("response_format_zip", "false")
                    .addTextBody("return_images", "false")
                    .addTextBody("return_model_output", "false")
                    .addTextBody("return_model_json", "false")
                    .build()) {
                httpPost.setEntity(httpEntity);

                log.info("开始调用文件解析接口：{}", url);
                String result = httpClient.execute(httpPost, response -> {
                    int statusCode = response.getCode();
                    log.info("文件解析接口响应码：{}", statusCode);

                    HttpEntity responseEntity = response.getEntity();
                    String responseBody = responseEntity != null ? EntityUtils.toString(responseEntity, "UTF-8") : "";
                    if (statusCode == 200) {
                        log.info("文件解析接口调用成功，响应体长度：{}", responseBody.length());
                        return responseBody;
                    } else {
                        log.error("文件解析接口调用失败, 状态码：{}, 响应：{}", statusCode, responseBody);
                        throw new RuntimeException(String.format("文件解析接口调用失败, 状态码：%s, 响应：%s", statusCode, responseBody));
                    }
                });
                return result;
            }

        } catch (Exception e) {
            log.error("调用文件解析接口异常", e);
            throw new RuntimeException("调用文件解析接口失败: " + e.getMessage(), e);
        } finally {
            closeQuietly(inputStream);
        }
    }

    /**
     * 处理文档转换为 ZIP 格式
     * <p>
     * 1. 调用文档解析结果获取 ZIP（包含 Markdown 和图片）
     * 2. 保存 ZIP 到本地磁盘
     * 3. 解压ZIP 文件
     * 4. 上传解压后的 md 和图片到 MinIO
     * 5. 调用 LLM 生成图片描述并更新 md
     * 6. 保存 md 的 MinIO 地址到 convertedUrl
     * 8. 异步清理本地临时文件
     *
     * @param document    KnowledgeDocumentEntity
     * @param inputStream InputStream
     */
    public String processDocumentToMarkdownFromZip(KnowledgeDocumentEntity document, InputStream inputStream) {
        log.info("开始处理文档转换为 ZIP，documentId: {}, docTitle: {}", document.getDocId(), document.getDocTitle());

        // 更新状态为转换中
        document.setStatus(DocumentStatus.CONVERTING);
        boolean result = knowledgeDocumentService.updateById(document);
        Assert.isTrue(result, "更新文档状态CONVERTING失败");

        String zipFilePath = null;
        String extractDir = null;
        try {
            // 生成一串数字，避免文件名的中文乱码
            String docTitle = document.getDocTitle() + document.getDocTitle().hashCode();

            // 1. 调用文档解析获取 ZIP 格式响应
            byte[] zipBytes = parseDocumentToZip(docTitle, inputStream);

            // 2. 保存 ZIP 到本地临时目录
            String tempDir = System.getProperty("java.io.tmpdir");
            String uniqueId = UUID.randomUUID().toString();
            zipFilePath = tempDir + File.separator + uniqueId + ".zip";
            extractDir = tempDir + File.separator + uniqueId + "_extracted";

            Files.write(Paths.get(zipFilePath), zipBytes);

            // 3. 解压 ZIP 文件
            extractZip(zipFilePath, extractDir);
            log.info("ZIP 文件已解压到：{}", extractDir);

            // 4. 上传解压后的 md 和图片到 MinIO，并处理 md 内容
            String mdMinIOUrl = processExtractedFiles(document, extractDir);

            // 5. 更新文档状态为已转换，保存 md 的 MinIO 地址
            document.setStatus(DocumentStatus.CONVERTED);
            document.setConvertedDocUrl(mdMinIOUrl);
            result = knowledgeDocumentService.updateById(document);
            Assert.isTrue(result, "更新文档状态CONVERTED失败");

            log.info("文档 ZIP 转换完成，documentId:{}, mdurl:{}", document.getDocId(), mdMinIOUrl);
            return mdMinIOUrl;
        } catch (Exception e) {
            log.error("文档 ZIP 转换失败，documentId:{}", document.getDocId(), e);
            document.setStatus(DocumentStatus.UPLOADED);
            result = knowledgeDocumentService.updateById(document);
            Assert.isTrue(result, "更新文档状态UPLOADED失败");
            throw new RuntimeException(String.format("文档 ZIP 转换失败: %s", e.getMessage()), e);
        } finally {
            closeQuietly(inputStream);
            // 异步清理临时文件
            cleanupTempFilesAsync(zipFilePath, extractDir);
        }
    }

    private void cleanupTempFilesAsync(String zipFilePath, String extractDir) {
        if (zipFilePath == null && extractDir == null) {
            return;
        }
        Thread.startVirtualThread(() -> {
            try {
                if (zipFilePath != null) {
                    Files.deleteIfExists(Paths.get(zipFilePath));
                    log.info("临时 ZIP 文件已删除：{}", zipFilePath);
                }

                // 删除解压目录
                if (extractDir != null) {
                    deleteDirectory(Paths.get(extractDir));
                    log.info("临时解压目录已删除：{}", extractDir);
                }
            } catch (IOException e) {
                log.warn("清理临时文件失败", e);
            }
        });
    }

    /**
     * 递归删除目录及其内容
     *
     * @param directory 目录路径
     */
    private void deleteDirectory(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }

        try (Stream<Path> paths = Files.walk(directory)) {
            paths.sorted((a, b) -> b.compareTo(a)) // 反向排序，先删除子文件/目录
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            log.warn("删除文件失败: {}", path, e);
                        }
                    });
        }
    }

    private String processExtractedFiles(KnowledgeDocumentEntity document, String extractDir) throws Exception {
        Path extractPath = Paths.get(extractDir);

        // 查找所有的 md 文件和图片文件
        Path mdFile = null;
        List<Path> imageFiles = Lists.newArrayList();
        try (Stream<Path> paths = Files.walk(extractPath)) {
            for (Path path : paths.toList()) {
                if (Files.isRegularFile(path)) {
                    String fileName = path.getFileName().toString().toLowerCase();
                    if (fileName.endsWith(".md")) {
                        mdFile = path;
                    } else if (fileName.endsWith(".png") || fileName.endsWith(".jpg")
                            || fileName.endsWith(".jpeg") || fileName.endsWith(".gif")
                            || fileName.endsWith(".webp") || fileName.endsWith(".bmp")) {
                        imageFiles.add(path);
                    }
                }
            }
        }

        if (mdFile == null) {
            throw new RuntimeException("解压后的文件未找到 Markdown 文件");
        }
        log.info("找到 Markdown 文件：{}, 图片数量为：{}", mdFile, imageFiles.size());

        // 上传图片到 MinIO,并建立本地文件名到 MinIO URL 的映射
        Map<String, String> imageUrlMap = Maps.newHashMap();
        String baseObjectName = CONVERTED_FILE_DIR + document.getDocTitle() + "/";
        for (Path imageFile : imageFiles) {
            String imageName = imageFile.getFileName().toString();
            byte[] imageBytes = Files.readAllBytes(imageFile);
            String contentType = getImageContentType(imageName);
            String objectName = baseObjectName + "images/" + imageName;
            String imageUrl = fileStorageService.uploadFile(objectName, imageBytes, contentType);
            imageUrlMap.put(imageName, imageUrl);
            log.info("图片已上传到 MinIO：{} -> {}", imageName, imageUrl);
        }

        // 读取 md 的文件内容
        String mdContent = Files.readString(mdFile, StandardCharsets.UTF_8);

//         替换 md中图片地址为 MinIO 地址，并生成图片描述
        String processedMdContent = processMarkdownImages(mdContent, imageUrlMap);

        // 上传处理后的 md 文件到 MinIO
        String mdObjectName = baseObjectName + mdFile.getFileName().toString();
        String mdUrl = fileStorageService.uploadFile(mdObjectName, processedMdContent.getBytes(StandardCharsets.UTF_8), ContentType.TEXT_MARKDOWN);
        log.info("Markdown 文件已上传到 MinIO：{}", mdUrl);
        return mdUrl;
    }

    /**
     * 处理 Markdown 文件中的图片地址
     * 替换地址并生成图片描述
     *
     * @param mdContent   Markdown 文件内容
     * @param imageUrlMap 图片文件名到 MinIO URL 的映射
     * @return 处理后的 Markdown 文件内容
     */
    private String processMarkdownImages(String mdContent, Map<String, String> imageUrlMap) {
        // 匹配图片标签的正则表达式: ![alt](path)
        Pattern pattern = Pattern.compile("!\\[(.*?)\\]\\(([^)]+)\\)");
        Matcher matcher = pattern.matcher(mdContent);

        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String imagePath = matcher.group(2);

            // 提取图片文件名
            String imageName = Paths.get(imagePath).getFileName().toString();

            // 获取 MinIO 上的图片 URL
            String minioUrl = imageUrlMap.get(imageName);
            if (minioUrl == null) {
                // 如果找不到对应的 MinIO URL，保持原样
                log.warn("未找到图片 {} 对应的 MinIO URL", imageName);
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group(0)));
                continue;
            }

            // 生成图片描述（mock 实现）
            String imageDescription = generateImageDescription(minioUrl);

            // 构建新的图片标签: ![描述](minio_url)
            String newImageTag = "![" + imageDescription + "](" + minioUrl + ")";
            matcher.appendReplacement(result, Matcher.quoteReplacement(newImageTag));

            log.info("图片标签已处理: {} -> {}", imagePath, minioUrl);
        }
        matcher.appendTail(result);
        return result.toString();
    }


    @Value("${langchain4j.open-ai.chat-model.api-key}")
    private String chatModelApiKey;

    @Value("${langchain4j.open-ai.chat-model.base-url}")
    private String chatModelBaseUrl;

    /**
     * 生成图片描述
     * <p>
     * 需要注意的是：如果你用的是外部模型，这个 url 需要是公网可以访问的 url，否则模型需要能和 MinIO 进行内网通信
     *
     * @param imageUrl 图片的 MinIO URL
     */
    public String generateImageDescription(String imageUrl) {
        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .apiKey(chatModelApiKey)
                .baseUrl(chatModelBaseUrl)
                .modelName("qwen3-vl-plus")
                .temperature(0.7)
                .logResponses(true)
                .logRequests(true)
                .build();
        UserMessage userMessage = UserMessage.from(
                new TextContent("请描述这张图片的内容，包括场景、对象、布局、颜色、文字信息，直接输出纯文本描述，不要多余说明，不要增加任何特殊符号，特别是换行符"),
                new ImageContent(imageUrl)
        );
        return chatModel.chat(userMessage).aiMessage().text();
    }

    /**
     * 获取图片的 Content-Type
     */
    private String getImageContentType(String fileName) {
        String lowerName = fileName.toLowerCase();
        if (lowerName.endsWith(".png")) {
            return "image/png";
        }
        if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (lowerName.endsWith(".gif")) {
            return "image/gif";
        }
        if (lowerName.endsWith(".webp")) {
            return "image/webp";
        }
        if (lowerName.endsWith(".bmp")) {
            return "image/bmp";
        }
        return "application/octet-stream";
    }

    /**
     * 解压 ZIP 文件
     *
     * @param zipFilePath ZIP 文件路径
     * @param extractDir  解压目录
     */
    private void extractZip(String zipFilePath, String extractDir) throws IOException {
        // 使用绝对规范路径作为基准目录，防止路径遍历攻击
        Path extractPath = Paths.get(extractDir).toAbsolutePath().normalize();
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFilePath))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String entryName = entry.getName();

                // 前置检查：拒绝包含路径遍历字符的条目名称
                if (entryName.contains("..") || entryName.startsWith("/") || entryName.startsWith("\\")) {
                    log.warn("跳过不安全的 ZIP 条目（包含非法路径字符）：{}", entryName);
                    continue;
                }

                Path entryPath = extractPath.resolve(entryName).toAbsolutePath().normalize();

                // 安全检查：确保解压路径严格在目标目录内（使用规范路径比较）
                if (!entryPath.startsWith(extractPath)) {
                    log.warn("跳过不安全的 ZIP 条目（路径遍历攻击）：{}", entryName);
                    continue;
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    Files.createDirectories(entryPath.getParent());
                    Files.copy(zis, entryPath, StandardCopyOption.REPLACE_EXISTING);
                }
                zis.closeEntry();
            }
        }
    }


    private byte[] parseDocumentToZip(String fileName, InputStream inputStream) {
        String url = fileParseApiUrl + "/file_parse";
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofMicroseconds(connectTimeout))
                .setResponseTimeout(Timeout.ofMilliseconds(responseTimeout))
                .build();
        try (CloseableHttpClient httpClient = HttpClients.custom().setDefaultRequestConfig(requestConfig).build()) {
            HttpPost httpPost = new HttpPost(url);
            httpPost.setHeader("Accept", "application/json");

            try (HttpEntity httpEntity = MultipartEntityBuilder.create()
                    .addBinaryBody("files", inputStream, org.apache.hc.core5.http.ContentType.APPLICATION_OCTET_STREAM, fileName)
                    .addTextBody("response_format_zip", "true")
                    .addTextBody("return_images", "true")
                    .addTextBody("return_model_output", "false")
                    .addTextBody("return_model_json", "false")
                    .build()) {

                httpPost.setEntity(httpEntity);

                log.info("开始调用文件解析接口：{}", url);
                return httpClient.execute(httpPost, response -> {
                    int statusCode = response.getCode();
                    log.info("文件解析接口响应码：{}", statusCode);

                    HttpEntity responseEntity = response.getEntity();
                    if (statusCode == 200 && responseEntity != null) {
                        byte[] zipBytes = EntityUtils.toByteArray(responseEntity);
                        log.info("文件解析接口调用成功，ZIP文件大小：{} bytes", zipBytes.length);
                        return zipBytes;
                    } else {
                        String responseBody = responseEntity != null ? EntityUtils.toString(responseEntity, "UTF-8") : "";
                        log.error("文件解析接口调用失败, 状态码：{}, 响应：{}", statusCode, responseBody);
                        throw new RuntimeException(String.format("文件解析接口调用失败, 状态码：%s, 响应：%s", statusCode, responseBody));
                    }
                });
            }

        } catch (Exception e) {
            log.error("调用文件解析接口异常", e);
            throw new RuntimeException("调用文件解析接口失败: " + e.getMessage(), e);
        } finally {
            closeQuietly(inputStream);
        }
    }

    /**
     * 安静关闭输入流，忽略异常
     *
     * @param inputStream 输入流
     */
    private void closeQuietly(InputStream inputStream) {
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (Exception ignored) {
                // 忽略关闭异常
            }
        }
    }
}
