package com.forever1996Fyk.ai.intelligent.customer.rag.modules.spltter;

import com.forever1996Fyk.ai.intelligent.customer.document.constant.SplitType;
import com.forever1996Fyk.ai.intelligent.customer.document.entity.DocumentSplitParam;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentByRegexSplitter;
import dev.langchain4j.data.document.splitter.DocumentByWordSplitter;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/6/4 09:26
 **/
public class DocumentSplitterFactory {

    public static DocumentSplitter getInstance(DocumentSplitParam param) {
        SplitType splitType = SplitType.valueOf(param.splitType());
        return switch (splitType) {
            case REGEX -> new DocumentByRegexSplitter(param.regex(), "\\n\\n", param.chunkSize(), param.overlap());
            case LENGTH -> new DocumentByWordSplitter(param.chunkSize(), param.overlap());
            case SEPARATOR ->
                    new DocumentByRegexSplitter(param.separator(), "\\n\\n", param.chunkSize(), param.overlap());
            case TITLE ->
                    new MarkdownHeaderParentTextSplitter(param.titleLevel(), false, false, param.chunkSize(), param.overlap());
            case SMART -> new MarkdownHeaderParentTextSplitter(param.chunkSize(), (int) ( param.chunkSize() * 0.1));
        };
    }
}
