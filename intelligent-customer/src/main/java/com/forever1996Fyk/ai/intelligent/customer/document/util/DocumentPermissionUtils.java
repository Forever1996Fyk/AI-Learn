package com.forever1996Fyk.ai.intelligent.customer.document.util;

import com.forever1996Fyk.ai.intelligent.customer.rag.constant.RoleEnum;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/7/9 23:19
 **/
public class DocumentPermissionUtils {

    public static String getDocumentPermission(RoleEnum roleEnum) {

        return switch (roleEnum) {
            case VISITOR -> RoleEnum.VISITOR.name();
            case OWNER -> RoleEnum.OWNER.name();
            case CUSTOMER_SERVICE -> RoleEnum.CUSTOMER_SERVICE.name();
        };
    }
}
