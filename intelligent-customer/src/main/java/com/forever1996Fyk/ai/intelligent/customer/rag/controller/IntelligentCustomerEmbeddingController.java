package com.forever1996Fyk.ai.intelligent.customer.rag.controller;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.elasticsearch.ElasticsearchEmbeddingStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/6/4 11:31
 **/
@RestController
@RequestMapping("/intelligent/customer/embedding")
public class IntelligentCustomerEmbeddingController {
    @Autowired
    private OpenAiEmbeddingModel embeddingModel;
    @Autowired
    private ElasticsearchEmbeddingStore embeddingStore;

    @RequestMapping("/adder")
    public String adder(String query) {
        TextSegment segment1 = TextSegment.from("I like football.", new Metadata(Map.of("version", "1")));
        Embedding embedding1 = embeddingModel.embed(segment1).content();
        embeddingStore.add(embedding1, segment1);

        TextSegment segment2 = TextSegment.from("The weather is good today.");
        Embedding embedding2 = embeddingModel.embed(segment2).content();
        embeddingStore.add(embedding2, segment2);

        Embedding queryEmbedding = embeddingModel.embed("What is your favourite sport?").content();
        EmbeddingSearchResult<TextSegment> relevant = embeddingStore.search(
                EmbeddingSearchRequest.builder()
                        .queryEmbedding(queryEmbedding)
                        .build());

        EmbeddingMatch<TextSegment> embeddingMatch = relevant.matches().getFirst();
        System.out.println(embeddingMatch.score());
        System.out.println(embeddingMatch.embedded().text());
        return embeddingMatch.embedded().text();
    }
}
