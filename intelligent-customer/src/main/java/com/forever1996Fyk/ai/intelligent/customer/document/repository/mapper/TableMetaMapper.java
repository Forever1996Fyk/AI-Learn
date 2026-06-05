package com.forever1996Fyk.ai.intelligent.customer.document.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.forever1996Fyk.ai.intelligent.customer.document.repository.bean.TableMetaEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * <p>
 * 表元数据表 Mapper 接口
 * </p>
 *
 * @author MichaelKai
 * @since 2026-06-05
 */
public interface TableMetaMapper extends BaseMapper<TableMetaEntity> {

    /**
     * 检查表是否存在
     *
     * @param tableName 表名
     * @return 存在返回1，不存在返回0
     */
    @Select("SELECT COUNT(*) FROM information_schema.tables WHERE table_name = #{tableName} AND table_schema = DATABASE()")
    int checkTableExists(@Param("tableName") String tableName);

    /**
     * 删除动态表
     *
     * @param tableName 表名
     */
    @Update("DROP TABLE IF EXISTS ${tableName}")
    void dropTable(String tableName);

    /**
     * 执行动态SQL（建表）
     *
     * @param sql 建表SQL语句
     */
    @Update("${sql}")
    void executeCreateTable(@Param("sql") String sql);
}
