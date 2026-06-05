package com.forever1996Fyk.ai.intelligent.customer.document.service;

import com.forever1996Fyk.ai.intelligent.customer.document.repository.bean.TableMetaEntity;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 表元数据表 服务类
 * </p>
 *
 * @author MichaelKai
 * @since 2026-06-05
 */
public interface TableMetaService extends IService<TableMetaEntity> {

    /**
     * 检查表是否存在
     * @param tableName 表名
     * @return 存在返回1，不存在返回0
     */
    int checkTableExists(String tableName);

    /**
     * 删除表
     * @param tableName 表名
     */
    void dropTable(String tableName);

    /**
     * 执行创建表SQL
     * @param createTableSql 创建表SQL
     */
    void executeCreateTable(String createTableSql);
}
