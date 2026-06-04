package com.forever1996Fyk.ai.intelligent.customer.document.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/6/4 08:56
 **/
public interface FileStorageService {

    /**
     * 上传文件
     *
     * @param file       MultipartFile
     * @param objectName objectName
     * @return path
     */
    String uploadFile(MultipartFile file, String objectName) throws Exception;

    /**
     * 上传文件
     *
     * @param objectName objectName
     * @param content    content
     * @param contentType contentType
     * @return path
     */
    String uploadFile(String objectName, byte[] content, String contentType) throws Exception;

    /**
     * 下载文件
     *
     * @param objectName objectName
     * @return InputStream
     */
    InputStream downloadFile(String objectName) throws Exception;

    /**
     * 删除文件
     *
     * @param objectName objectName
     */
    void deleteFile(String objectName) throws Exception;

    /**
     * 获取预签名URL
     *
     * @param objectName objectName
     * @return String
     */
    String getPresignedUrl(String objectName) throws Exception;
}
