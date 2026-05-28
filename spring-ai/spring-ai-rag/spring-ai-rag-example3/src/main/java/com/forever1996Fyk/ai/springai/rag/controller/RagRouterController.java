package com.forever1996Fyk.ai.springai.rag.controller;

import com.forever1996Fyk.ai.springai.rag.router.GraphDatabaseService;
import com.forever1996Fyk.ai.springai.rag.router.QueryRouteService;
import com.forever1996Fyk.ai.springai.rag.router.RelationalDatabaseService;
import com.forever1996Fyk.ai.springai.rag.router.VectorDatabaseService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/5/28 22:48
 **/
@RestController
@RequestMapping("/rag/router")
public class RagRouterController {
    @Autowired
    private QueryRouteService queryRouteService;
    @Autowired
    private VectorDatabaseService vectorDatabaseService;
    @Autowired
    private GraphDatabaseService graphDatabaseService;
    @Autowired
    private RelationalDatabaseService relationalDatabaseService;

    @GetMapping("/route")
    public String route(String query) {
        return queryRouteService.route(query);
    }

    @RequestMapping("/query")
    public String ragQuery(HttpServletResponse response, @RequestParam String question) {
        response.setCharacterEncoding("UTF-8");
        String databaseType = queryRouteService.route(question);
        String result = switch (databaseType.trim()) {
            case "VECTOR" -> vectorDatabaseService.searchVectorDatabase(question);
            case "GRAPH" -> graphDatabaseService.searchGraphDatabase(question);
            case "RELATIONAL" -> relationalDatabaseService.searchRelationalDatabase(question);
            default -> "无法确定合适的数据库类型，默认使用向量数据库: " +
                    vectorDatabaseService.searchVectorDatabase(question);
        };

        return String.format("路由到: %s 数据库\n\n查询结果:\n%s", databaseType, result);
    }
}
