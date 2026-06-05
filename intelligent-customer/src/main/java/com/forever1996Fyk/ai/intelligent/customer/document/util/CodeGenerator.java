package com.forever1996Fyk.ai.intelligent.customer.document.util;

import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.GlobalConfig;
import com.baomidou.mybatisplus.generator.config.ITypeConvert;
import com.baomidou.mybatisplus.generator.config.converts.PostgreSqlTypeConvert;
import com.baomidou.mybatisplus.generator.config.rules.DateType;
import com.baomidou.mybatisplus.generator.config.rules.DbColumnType;
import com.baomidou.mybatisplus.generator.config.rules.IColumnType;
import com.baomidou.mybatisplus.generator.config.rules.NamingStrategy;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;
import com.baomidou.mybatisplus.generator.keywords.PostgreSqlKeyWordsHandler;
import com.baomidou.mybatisplus.generator.query.SQLQuery;
import org.springframework.lang.NonNull;

/**
 * @program: idp-onews
 * @description:
 * @author: YuKai Fan
 * @create: 2026/4/28 16:14
 **/
public class CodeGenerator {
    public static void main(String[] args) {

        String projectPath = System.getProperty("user.dir");
        String srcPath = projectPath + "/intelligent-customer/src/main/java";
        // 使用 FastAutoGenerator 快速配置代码生成器
        FastAutoGenerator.create("jdbc:mysql://172.30.47.249:3306/intelligent_customer?nullCatalogMeansCurrent=true&serverTimezone=UTC&useUnicode=true&characterEncoding=UTF-8&importsSL=false",
                        "wanyun_ppc_test",
                        "hLGQK4H3nz9h0f2qkfuf7lm0nYzv1ll9")
                .globalConfig(builder -> {
                    builder.author("MichaelKai") // 设置作者
                            .dateType(DateType.ONLY_DATE)
                            .outputDir(srcPath); // 输出目录
                })
                .packageConfig(builder -> {
                    builder // 设置父包名
                            .parent("com.forever1996Fyk.ai.intelligent.customer.chat")
                            .entity("repository.bean") // 设置实体类包名
                            .mapper("repository.mapper") // 设置 Mapper 接口包名
                            .service("service") // 设置 Service 接口包名
                            .serviceImpl("service.impl"); // 设置 Service 实现类包名
                })
                .strategyConfig(builder -> {
                    builder
                            .enableCapitalMode()
                            .enableSkipView()
                            .addInclude("chat_conversation", "chat_message") // 设置需要生成的表名
                            .controllerBuilder().disable()
                            .mapperBuilder().disableMapperXml()
                            .entityBuilder()
                            .enableFileOverride()
                            .disableSerialVersionUID()
                            .formatFileName("%sEntity")
                            .enableFileOverride()
                            .enableRemoveIsPrefix()
                            .versionColumnName("version")
                            .naming(NamingStrategy.underline_to_camel)
                            .enableLombok() // 启用 Lombok
                            .serviceBuilder()
                            .formatServiceFileName("%sService")
                            .formatServiceImplFileName("%sServiceImpl")
                            .mapperBuilder()
                            .formatMapperFileName("%sMapper");
                })
                // 使用 Freemarker 模板引擎
                .templateEngine(new FreemarkerTemplateEngine())
                // 执行生成
                .execute();
    }
}
