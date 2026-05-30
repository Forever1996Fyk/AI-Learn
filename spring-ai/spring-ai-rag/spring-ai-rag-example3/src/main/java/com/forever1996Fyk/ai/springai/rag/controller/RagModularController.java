package com.forever1996Fyk.ai.springai.rag.controller;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.preretrieval.query.expansion.MultiQueryExpander;
import org.springframework.ai.rag.preretrieval.query.expansion.QueryExpander;
import org.springframework.ai.rag.preretrieval.query.transformation.CompressionQueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.TranslationQueryTransformer;
import org.springframework.ai.rag.retrieval.join.ConcatenationDocumentJoiner;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/5/30 22:02
 **/
@RestController
@RequestMapping("/rag/modular")
public class RagModularController implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(RagModularController.class);
    @Autowired
    private DashScopeChatModel chatModel;

    @Autowired
    private VectorStore vectorStore;


    private ChatClient chatClient;

    @GetMapping("/retriever")
    public String retriever(String query, String fileName) {
        VectorStoreDocumentRetriever documentRetriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .topK(5)
                .similarityThreshold(0.5)
//                .filterExpression(new Filter.Expression(Filter.ExpressionType.EQ, new Filter.Key("fileName"), new Filter.Value(fileName)))
                .build();
        QueryTransformer rewriteQueryTransformer = RewriteQueryTransformer.builder()
                .chatClientBuilder(ChatClient.builder(chatModel))
                .promptTemplate(new PromptTemplate(
                        """
                                Given a user query, rewrite it to provide better results when querying a {target}.
                                Remove any irrelevant information, and ensure the query is concise and specific.
                                           
                                Original query:
                                {query}
                                            
                                Rewritten query:
                                """
                ))
                .build();

        QueryTransformer compressionQueryTransformer = CompressionQueryTransformer.builder()
                .chatClientBuilder(ChatClient.builder(chatModel).build().mutate())
                .build();
        QueryTransformer translationQueryTransformer = TranslationQueryTransformer.builder()
                .chatClientBuilder(ChatClient.builder(chatModel).build().mutate())
                .targetLanguage("zh")
                .build();

        QueryExpander queryExpander = MultiQueryExpander.builder()
                .chatClientBuilder(ChatClient.builder(chatModel))
                .numberOfQueries(3)
                .includeOriginal(true)
                .build();
        Advisor advisor = RetrievalAugmentationAdvisor.builder()
                .documentRetriever(documentRetriever)
                .queryTransformers(rewriteQueryTransformer, compressionQueryTransformer, translationQueryTransformer)
                .queryExpander(queryExpander)
                .build();
        return chatClient.prompt(query).advisors(advisor).call().content();
    }

    @GetMapping("/queryTransformer")
    public void queryTransformer(String text) {
        QueryTransformer rewriteQueryTransformer = RewriteQueryTransformer.builder()
                .chatClientBuilder(ChatClient.builder(chatModel))
                .build();
        Query query = new Query(text);
        QueryTransformer compressionQueryTransformer = CompressionQueryTransformer.builder()
                .chatClientBuilder(ChatClient.builder(chatModel).build().mutate())
                .build();
        QueryTransformer translationQueryTransformer = TranslationQueryTransformer.builder()
                .chatClientBuilder(ChatClient.builder(chatModel).build().mutate())
                .targetLanguage("en")
                .build();
        System.out.printf(
                """
                        rewriteQueryTransformer is %s 
                                                
                        =============================
                        compressionQueryTransformer is %s 
                                                
                        =============================
                        translationQueryTransformer is %s 
                                                
                        %n""",
                rewriteQueryTransformer.transform(query).text(),
                compressionQueryTransformer.transform(query).text(),
                translationQueryTransformer.transform(query).text()
        );
    }

    @GetMapping("/expander")
    public void queryExpander(String query) {
        QueryExpander queryExpander = MultiQueryExpander.builder()
                .chatClientBuilder(ChatClient.builder(chatModel))
                .numberOfQueries(3)
                .includeOriginal(true)
                .build();

        List<Query> expand = queryExpander.expand(new Query(query));
        log.info("扩展后的Query is {}", expand);
    }

    @GetMapping("/documentJoiner")
    public void documentJoiner(String text) {
        VectorStoreDocumentRetriever documentRetriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .topK(5)
                .similarityThreshold(0.5)
                .build();
        Query query = new Query(text);
        List<Document> documents = documentRetriever.retrieve(query);
        ConcatenationDocumentJoiner concatenationDocumentJoiner = new ConcatenationDocumentJoiner();
        List<Document> documentList = concatenationDocumentJoiner.join(Map.of(query, List.of(documents)));
        log.info("documentList is {}", documentList);
    }

    @GetMapping("/queryAugmenter")
    public void queryAugmenter(String text) {
        VectorStoreDocumentRetriever documentRetriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .topK(5)
                .similarityThreshold(0.5)
                .build();
        Query query = new Query(text);
        List<Document> documents = documentRetriever.retrieve(query);
        ContextualQueryAugmenter queryAugmenter = ContextualQueryAugmenter.builder()
                .allowEmptyContext(true)
                .emptyContextPromptTemplate(new PromptTemplate("请回答一下用户问题"))
                .build();
        Query newQuery = queryAugmenter.augment(query, documents);
        log.info("newQuery is {}", newQuery);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.chatClient = ChatClient.builder(chatModel).build();
    }
}
