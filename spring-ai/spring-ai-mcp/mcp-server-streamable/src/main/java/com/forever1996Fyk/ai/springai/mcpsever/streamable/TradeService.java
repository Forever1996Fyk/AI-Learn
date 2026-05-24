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
public class TradeService {

    @Tool(name = "trade_getInfo", description = "根据交易 id 获取详情")
    public String getTradeInfo(@ToolParam(description = " 交易 id") String  id) {
        return "交易：" + id + ", 交易详情：这是一个交易";
    }
}
