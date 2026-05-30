package com.forever1996Fyk.ai.springai.rag.controller;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.forever1996Fyk.ai.springai.rag.neo4j.model.Director;
import com.forever1996Fyk.ai.springai.rag.neo4j.model.Movie;
import com.forever1996Fyk.ai.springai.rag.neo4j.service.GraphService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/5/29 22:22
 **/
@RestController
@RequestMapping("/rag/graph")
public class GraphRagController {
    @Autowired
    private Neo4jTemplate neo4jTemplate;
    @Autowired
    private Neo4jClient neo4jClient;
    @Autowired
    private GraphService graphService;

    @Autowired
    private DashScopeChatModel chatModel;

    @GetMapping("/init")
    public String initData() {
        neo4jTemplate.save(new Director("张艺谋"));
        neo4jTemplate.save(new Director("陈思成"));
        neo4jTemplate.save(new Movie("十面埋伏", 2004));
        neo4jTemplate.save(new Movie("影", 2016));
        neo4jTemplate.save(new Movie("英雄", 2002));
        neo4jTemplate.save(new Movie("误杀", 2019));

        // 这里虽然叫 query，但是可以执行 写操作
        neo4jClient.query("""
                        MATCH (p:Director {name:$name}), (m:Movie {title: $movieTitle})
                        MERGE (p) -[:DIRECTED] ->(m)
                        """)
                .bind("张艺谋").to("name")
                .bind("十面埋伏").to("movieTitle")
                .run();

        neo4jClient.query("""
                        MATCH (p:Director {name:$name}), (m:Movie {title: $movieTitle})
                        MERGE (p) -[:DIRECTED] ->(m)
                        """)
                .bind("张艺谋").to("name")
                .bind("影").to("movieTitle")
                .run();
        neo4jClient.query("""
                        MATCH (p:Director {name:$name}), (m:Movie {title: $movieTitle})
                        MERGE (p) -[:DIRECTED] ->(m)
                        """)
                .bind("张艺谋").to("name")
                .bind("英雄").to("movieTitle")
                .run();
        neo4jClient.query("""
                        MATCH (p:Director {name:$name}), (m:Movie {title: $movieTitle})
                        MERGE (p) -[:DIRECTED] ->(m)
                        """)
                .bind("陈思成").to("name")
                .bind("误杀").to("movieTitle")
                .run();
        return "success";
    }

    @GetMapping("/ask")
    public String ask(String movieName) {
        String context = graphService.retrieveContext(movieName);
        String prompt = """
                你是一个电影知识助手，请根据以下上下文回答问题。
                如果上下文没有足够信息，请回答“我不知道”。
                
                上下文：
                %s
                
                问题：%s
                回答：
                """.formatted(context, movieName + "的导演还执导过哪些电影?");
        return chatModel.call(prompt);
    }
}
