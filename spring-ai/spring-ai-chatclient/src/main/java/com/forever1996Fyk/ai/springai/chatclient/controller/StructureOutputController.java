package com.forever1996Fyk.ai.springai.chatclient.controller;

import com.forever1996Fyk.ai.springai.chatclient.model.Book;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.converter.ListOutputConverter;
import org.springframework.ai.converter.MapOutputConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/5/20 21:19
 **/
@RestController
@RequestMapping("/structure")
public class StructureOutputController {

    @Autowired
    private ChatClient deepseekClient;

    @GetMapping("/call")
    public String call(String message) {
        PromptTemplate promptTemplate = PromptTemplate.builder().template("请给我推荐几本Java有关的书，输出格式: {format}").build();
        return deepseekClient
                .prompt(
                        promptTemplate.create(
                                // 这里用了BeanOutputConverter 来转换输出结构
                                Map.of("format", new BeanOutputConverter<>(Book.class).getFormat())
                        )
                ).call().content();
    }

    @GetMapping("/call2")
    public String call2(String message) {
        BeanOutputConverter<Book> beanOutputConverter = new BeanOutputConverter<>(Book.class);
        PromptTemplate promptTemplate = PromptTemplate.builder().template("请给我推荐几本Java有关的书，输出格式: {format}").build();
        String result = deepseekClient
                .prompt(
                        promptTemplate.create(
                                // 这里用了BeanOutputConverter 来转换输出结构
                                Map.of("format", beanOutputConverter.getFormat())
                        )
                ).call().content();
        Book book = beanOutputConverter.convert(result);
        System.out.println(book);
        return book.name() + " " + book.author() + " " + book.description() + " " + book.price();
    }

    @GetMapping("/call3")
    public String call3(String message) {
        Book book = deepseekClient
                .prompt("请给我推荐几本Java有关的书").call().entity(Book.class);
        System.out.println(book);
        return book.name() + " " + book.author() + " " + book.description() + " " + book.price();
    }

    @GetMapping("/callList")
    public List<String> callList(String message) {
        List<String> result = deepseekClient
                .prompt("请给我推荐几本Java有关的书").call().entity(new ListOutputConverter());
        return result;
    }

    @GetMapping("/callMap")
    public Map<String, Object> callMap(String message) {
        Map<String, Object> result = deepseekClient
                .prompt("请给我推荐几本Java有关的书").call().entity(new MapOutputConverter());
        return result;
    }

    @GetMapping("/callList2")
    public List<Book> callList2(String message) {
        List<Book> result = deepseekClient
                .prompt("请给我推荐几本Java有关的书").call().entity(new ParameterizedTypeReference<List<Book>>() {
                });
        return result;
    }
}
