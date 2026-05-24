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
public class OrderService {

    @Tool(name = "order_getInfo", description = "根据订单 id 获取详情")
    public String getOrderInfo(@ToolParam(description = " 订单 id") String  id) {
        return "订单：" + id + ", 订单详情：这是一个订单";
    }
}
