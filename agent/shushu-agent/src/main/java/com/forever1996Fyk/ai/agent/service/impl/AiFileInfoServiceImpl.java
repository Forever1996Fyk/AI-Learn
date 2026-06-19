package com.forever1996Fyk.ai.agent.service.impl;

import com.forever1996Fyk.ai.agent.repository.bean.AiFileInfoEntity;
import com.forever1996Fyk.ai.agent.repository.mapper.AiFileInfoMapper;
import com.forever1996Fyk.ai.agent.service.AiFileInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 文件元数据表，存储文件基本信息和解析后的内容 服务实现类
 * </p>
 *
 * @author MichaelKai
 * @since 2026-06-19
 */
@Service
public class AiFileInfoServiceImpl extends ServiceImpl<AiFileInfoMapper, AiFileInfoEntity> implements AiFileInfoService {

}
