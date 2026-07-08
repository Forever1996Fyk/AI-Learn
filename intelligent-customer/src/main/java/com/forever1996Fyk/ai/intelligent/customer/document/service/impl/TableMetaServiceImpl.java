package com.forever1996Fyk.ai.intelligent.customer.document.service.impl;

import com.forever1996Fyk.ai.intelligent.customer.document.repository.bean.TableMetaEntity;
import com.forever1996Fyk.ai.intelligent.customer.document.repository.mapper.TableMetaMapper;
import com.forever1996Fyk.ai.intelligent.customer.document.service.TableMetaService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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

    @Override
    public List<TableMetaEntity> listActiveForQuery() {
        // DATA_QUERY 同一逻辑表在所有版本中复用同一个物理表，
        // 因此只要表元数据存在且未被逻辑删除，就暴露给 Text2SQL。
        List<TableMetaEntity> allMetas = list();
        if (CollectionUtils.isEmpty(allMetas)) {
            return Collections.emptyList();
        }
        return allMetas.stream()
                .filter(meta -> meta.getCreateSql() != null && !meta.getCreateSql().isBlank())
                .collect(Collectors.toList());
    }
}
