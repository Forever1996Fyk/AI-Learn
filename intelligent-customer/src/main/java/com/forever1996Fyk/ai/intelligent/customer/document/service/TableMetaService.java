package com.forever1996Fyk.ai.intelligent.customer.document.service;

import com.forever1996Fyk.ai.intelligent.customer.document.repository.bean.TableMetaEntity;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

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

    /**
     * 查询当前应暴露给 Text2SQL 的动态表元数据。
     * <p>
     * 只返回满足以下条件的表：
     * <ul>
     *   <li>version_id 不为空，且等于对应 knowledge_document.current_version_id</li>
     *   <li>version_id 为空的老数据（兼容旧表）</li>
     * </ul>
     * 这样新本版上传期间，旧版本的物理表仍然可以被查询；新版本数据就绪、
     * current_version_id 切换后，LLM 自动感知到新表，实现不停机更新。
     */
    List<TableMetaEntity> listActiveForQuery();
}
