package com.forever1996Fyk.ai.springai.rag.controller;

import com.forever1996Fyk.ai.springai.rag.splitter.ModalTextSplitter;
import org.apache.poi.ss.formula.functions.T;
import org.springframework.ai.document.Document;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/6/2 00:28
 **/
@RestController
@RequestMapping("/rag/splitter")
public class RagSplitterController {

    @PostMapping("/modalSplitText")
    public String modalSplitText(@RequestBody Text text) {
        ModalTextSplitter modalTextSplitter = new ModalTextSplitter(300, 20);
        List<Document> documents = modalTextSplitter.split(new Document(text.getText()));
        StringBuilder sb = new StringBuilder();
        for (Document document : documents) {
            sb.append("=============================\n").append(document.getText()).append("\n");
        }
        return sb.toString();
    }

    public static class Text {
        private String text;

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }
}
