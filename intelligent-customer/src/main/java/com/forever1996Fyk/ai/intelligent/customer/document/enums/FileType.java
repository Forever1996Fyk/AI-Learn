package com.forever1996Fyk.ai.intelligent.customer.document.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/6/3 11:15
 **/
@Getter
@RequiredArgsConstructor
public enum FileType {
    PDF("pdf"),
    DOC("doc"),
    TXT("txt"),
    HTML("html"),
    MARKDOWN("markdown"),
    CSV("csv"),
    EXCEL("excel"),

    ;

    private final String type;
}
