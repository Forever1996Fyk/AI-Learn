package com.forever1996Fyk.ai.agent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.forever1996Fyk.ai.agent.domain.FileInfo;
import com.forever1996Fyk.ai.agent.repository.bean.AiFileInfoEntity;
import com.forever1996Fyk.ai.agent.repository.mapper.AiFileInfoMapper;
import com.forever1996Fyk.ai.agent.service.AiFileInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 文件元数据表，存储文件基本信息和解析后的内容 服务实现类
 * </p>
 *
 * @author MichaelKai
 * @since 2026-06-19
 */
@Slf4j
@Service
public class AiFileInfoServiceImpl extends ServiceImpl<AiFileInfoMapper, AiFileInfoEntity> implements AiFileInfoService {

    @Override
    public void saveFileInfo(FileInfo fileInfo) {
        AiFileInfoEntity entity = convertToEntity(fileInfo);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        this.save(entity);
        log.info("文件信息已保存: fileId={}", fileInfo.getFileId());
    }

    @Override
    public void updateFileInfo(FileInfo fileInfo) {
        AiFileInfoEntity entity = convertToEntity(fileInfo);
        entity.setUpdateTime(LocalDateTime.now());

        QueryWrapper<AiFileInfoEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("file_id", fileInfo.getFileId());
        this.update(entity, wrapper);
        log.info("文件信息已更新: fileId={}", fileInfo.getFileId());
    }

    @Override
    public FileInfo getFileInfoById(String fileId) {
        AiFileInfoEntity entity = getEntityById(fileId);
        if (entity == null) {
            return null;
        }
        return convertToDto(entity);
    }

    @Override
    public AiFileInfoEntity getEntityById(String fileId) {
        QueryWrapper<AiFileInfoEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("file_id", fileId);
        return this.getOne(wrapper);
    }

    @Override
    public void deleteFileInfo(String fileId) {
        QueryWrapper<AiFileInfoEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("file_id", fileId);
        this.remove(wrapper);
        log.info("文件信息已删除: fileId={}", fileId);
    }

    @Override
    public List<FileInfo> getAllFiles() {
        QueryWrapper<AiFileInfoEntity> wrapper = new QueryWrapper<>();
        List<AiFileInfoEntity> entities = this.list(wrapper);
        return entities.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    public boolean exists(String fileId) {
        QueryWrapper<AiFileInfoEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("file_id", fileId);
        return this.count(wrapper) > 0;
    }

    @Override
    public int getFileCount() {
        return Math.toIntExact(this.count());
    }

    /**
     * 将DTO转换为实体
     */
    private AiFileInfoEntity convertToEntity(FileInfo fileInfo) {
        AiFileInfoEntity entity = new AiFileInfoEntity();
        BeanUtils.copyProperties(fileInfo, entity);
        entity.setStatus(fileInfo.getStatus() != null ? fileInfo.getStatus().name() : "PENDING");
        return entity;
    }

    /**
     * 将实体转换为DTO
     */
    private FileInfo convertToDto(AiFileInfoEntity entity) {
        FileInfo fileInfo = new FileInfo();
        BeanUtils.copyProperties(entity, fileInfo);
        // 转换状态字符串为枚举
        if (entity.getStatus() != null) {
            try {
                fileInfo.setStatus(FileInfo.FileStatus.valueOf(entity.getStatus()));
            } catch (IllegalArgumentException e) {
                log.warn("无法识别的文件状态: {}, 使用默认状态PENDING", entity.getStatus());
                fileInfo.setStatus(FileInfo.FileStatus.PENDING);
            }
        }
        return fileInfo;
    }
}
