package com.forever1996Fyk.ai.springai.rag.controller;

import com.forever1996Fyk.ai.springai.rag.generate.SqlQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/5/28 23:04
 **/
@RestController
@RequestMapping("/rag/generate")
public class RagGenerateController {
    @Autowired
    private SqlQueryService sqlQueryService;

    @GetMapping("/text2Sql")
    public String text2Sql(String query) {
        return sqlQueryService.text2sql(query);
    }
}
