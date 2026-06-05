package com.forever1996Fyk.ai.intelligent.customer.document.service.impl;

import com.forever1996Fyk.ai.intelligent.customer.document.repository.bean.TableMetaEntity;
import com.forever1996Fyk.ai.intelligent.customer.document.repository.mapper.TableMetaMapper;
import com.forever1996Fyk.ai.intelligent.customer.document.service.TableMetaService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 表元数据表 服务实现类
 * </p>
 *
 * @author MichaelKai
 * @since 2026-06-05
 */
@Service
public class TableMetaServiceImpl extends ServiceImpl<TableMetaMapper, TableMetaEntity> implements TableMetaService {

    @Override
    public int checkTableExists(String tableName) {
        return baseMapper.checkTableExists(tableName);
    }

    @Override
    public void dropTable(String tableName) {
        baseMapper.dropTable(tableName);
    }

    @Override
    public void executeCreateTable(String createTableSql) {
        baseMapper.executeCreateTable(createTableSql);
    }
}
