package com.forever1996Fyk.ai.springai.mcpsever.streamable;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/5/24 23:13
 **/
@Service
public class GoodsService {

    @Tool(name = "goods_getNum", description = "根据商品名称获取数量")
    public String getGoodsNum(@ToolParam(description = "商品名称") String name) {
        return "商品名称：" + name + ", 数量是：100";
    }

    @Tool(name = "goods_getDesc", description = "根据商品名称获取描述")
    public String getGoodsDesc(@ToolParam(description = "商品名称") String name) {
        return "商品名称：" + name + ", 描述：这是一个商品";
    }
}
