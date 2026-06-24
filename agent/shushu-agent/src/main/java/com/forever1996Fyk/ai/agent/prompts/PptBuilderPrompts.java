package com.forever1996Fyk.ai.agent.prompts;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/6/25 00:02
 **/
public class PptBuilderPrompts {

    /**
     * Schema修改提示词模板
     */
    public static final String getSchemaModifyPrompt(String userRequest, String currentSchema) {
        return """
                ## 角色
                你是专业的PPT Schema修改专家。

                ## 任务
                根据用户的修改需求，修改已有的PPT Schema。

                ## 用户修改需求（重点关注）
                %s

                ## 当前PPT Schema（必须保留用户不需要改动的部分）
                %s

                ## 输出格式要求
                输出JSON格式，结构如下：
                {
                  "slides": [
                    {
                      "pageType": "页面类型（大写）",
                      "pageDesc": "页面描述",
                      "templatePageIndex": 模板页码索引,
                      "data": {
                        "字段名": { ... },
                        ...
                      }
                    }
                  ]
                }

                ## 字段属性说明（固定格式）

                ### type = "text" （文本字段）
                {
                  "type": "text",
                  "content": "实际文本内容（字符数必须≤fontLimit）",
                  "fontLimit": 数字
                }

                硬性要求：
                - type 固定为 "text"
                - content 字符数必须 ≤ fontLimit（绝对不允许超过）
                - 超出视为错误输出
                - 必须在生成前自行计算字符数
                - fontLimit 必须与原Schema完全一致
                - 中文：1字=1，英文字符/标点/空格/换行=1

                ### type = "image" （图片字段）
                {
                  "type": "image",
                  "content": "图片生成提示词，描述需要生成什么样的图片",
                  "url": ""（保持原值或传空）
                }

                - type 固定值为 "image"
                - content：用于文生图的提示词，结合布局要求补充样式描述
                - url：图片URL地址，如果用户要求替换图片则设置为空字符串，否则保持原值

                ### type = "background" （背景字段）
                {
                  "type": "background",
                  "content": "图片生成提示词，描述需要生成什么样的图片，图片背景一般注重布局，不要带有文字",
                  "url": ""（默认传空）
                }
                    
                - type 固定值为 "background"
                - content：图片生成提示词，描述需要生成什么样的图片，图片背景一般注重布局，不要带有文字
                - url：图片URL地址，用于替换模板中对应图片，默认空字符串

                ## 修改规则
                1. 严格按照原Schema定义的字段名和类型生成
                2. pageType: 保持不变，必须是大写（COVER/CATALOG/CONTENT/COMPARE/END等）
                3. pageDesc: 页面描述，根据用户需求修改
                4. templatePageIndex: 保持不变（指向模板中的页码索引）
                5. data: 根据用户需求修改对应字段，字段名必须完全匹配，不能多也不能少
                6. fontLimit 是硬性约束：
                   - content字符数必须 ≤ fontLimit
                   - 必须先计算再输出
                   - 违规视为失败
                7. 如果用户要求替换image或background，将url设为空字符串，保留content作为生成提示词
                8. 如果用户只修改文字，保持图片url不变
                9. 保持不需要修改的部分原样输出

                ## 输出前自检
                1. 输出JSON前必须检查每个text字段：
                   - 实际字符数 ≤ fontLimit ?
                2. 如果超出：必须重新生成该字段，禁止直接输出
                3. 禁止跳过自检流程

                ## 修改范围判断
                1. 如果用户指定了页码，只修改指定页面
                2. 如果用户没有指定页码，分析需求判断需要修改哪些页面
                3. 未明确要求修改的部分保持原样

                ## 注意事项
                1. 必须输出完整JSON，不要有任何注释
                2. slides数组顺序保持不变
                3. 字段名必须与原Schema完全一致
                4. 字段type值必须正确（只能是text/image/background其中之一）
                5. 每个字段必须包含必需属性（text: type+content+fontLimit, image: type+content+url, background: type+content+url）
                6. fontLimit严格保证，不允许超出
                7. 严格按照Schema定义的字段名和类型生成
                """.formatted(userRequest, currentSchema);
    }
}
