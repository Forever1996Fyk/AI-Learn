package com.forever1996Fyk.ai.agentx.core.model;

import org.springframework.core.ParameterizedTypeReference;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/9/9 15:49
 **/
public class OutputType {

    private final Type type;

    private OutputType(Type type) {
        this.type = type;
    }


    /**
     * 单对象类型。
     *
     * @param clazz 目标类型
     * @return OutputType 实例
     */
    public static OutputType of(Class<?> clazz) {
        return new OutputType(clazz);
    }

    /**
     * List 集合类型。
     *
     * @param elementType List 的元素类型
     * @return OutputType 实例，内部表示为 {@code List<elementType>}
     */
    public static OutputType listOf(Class<?> elementType) {
        return new OutputType(new ParameterizedType() {
            @Override
            public Type[] getActualTypeArguments() {
                return new Type[]{elementType};
            }

            @Override
            public Type getRawType() {
                return List.class;
            }

            @Override
            public Type getOwnerType() {
                return null;
            }
        });
    }

    /**
     * 转为 ParameterizedTypeReference，供 BeanOutputConverter 使用。
     */
    public ParameterizedTypeReference<?> toTypeReference() {
        return new ParameterizedTypeReference<Object>() {
            @Override
            public Type getType() {
                return OutputType.this.type;
            }
        };
    }

    /**
     * 获取底层 Type。
     */
    public Type getType() {
        return type;
    }
}
