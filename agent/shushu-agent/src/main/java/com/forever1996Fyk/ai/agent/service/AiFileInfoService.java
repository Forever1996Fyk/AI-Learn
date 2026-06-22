package com.forever1996Fyk.ai.agent.service;

import com.forever1996Fyk.ai.agent.domain.FileInfo;
import com.forever1996Fyk.ai.agent.repository.bean.AiFileInfoEntity;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 文件元数据表，存储文件基本信息和解析后的内容 服务类
 * </p>
 *
 * @author MichaelKai
 * @since 2026-06-19
 */
public interface AiFileInfoService extends IService<AiFileInfoEntity> {

    void saveFileInfo(FileInfo fileInfo);

    void updateFileInfo(FileInfo fileInfo);

    FileInfo getFileInfoById(String fileId);

    AiFileInfoEntity getEntityById(String fileId);

    void deleteFileInfo(String fileId);

    List<FileInfo> getAllFiles();

    boolean exists(String fileId);

    int getFileCount();
}
