package com.forever1996Fyk.ai.intelligent.customer.rag.modules.aggregator;

import com.forever1996Fyk.ai.intelligent.customer.rag.util.ContentUtil;
import com.google.common.collect.Lists;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.aggregator.ContentAggregator;
import dev.langchain4j.rag.query.Query;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/7/11 23:09
 **/
public class IntelligentCustomerHybridContentAggregator implements ContentAggregator {
    private final ContentAggregator unstructuredAggregator;

    public IntelligentCustomerHybridContentAggregator(ContentAggregator unstructuredAggregator) {
        this.unstructuredAggregator = unstructuredAggregator;
    }
    @Override
    public List<Content> aggregate(Map<Query, Collection<List<Content>>> queryToContents) {
        if (MapUtils.isEmpty(queryToContents)) {
            return Lists.newArrayList();
        }
        List<Content> structuredContents = new ArrayList<>();
        Map<Query, Collection<List<Content>>> unstructuredQueryToContents = new LinkedHashMap<>();

        for (Map.Entry<Query, Collection<List<Content>>> entry : queryToContents.entrySet()) {
            Query query = entry.getKey();
            Collection<List<Content>> contentLists = entry.getValue();

            List<List<Content>> unstructuredLists = Lists.newArrayList();
            for (List<Content> contentList : contentLists) {
                List<Content> unstructured = new ArrayList<>();
                for (Content content : contentList) {
                    if (ContentUtil.isSkipRerank(content)) {
                        structuredContents.add(content);
                    } else {
                        unstructuredLists.add(contentList);
                    }
                }
                if (CollectionUtils.isNotEmpty(unstructured)) {
                    unstructuredLists.add(unstructured);
                }
            }

            if (CollectionUtils.isNotEmpty(unstructuredLists)) {
                unstructuredQueryToContents.put(query, unstructuredLists);
            }
        }
        List<Content> unstructuredResults = unstructuredAggregator.aggregate(unstructuredQueryToContents);
        List<Content> combined = Lists.newArrayListWithCapacity(structuredContents.size() + unstructuredResults.size());
        combined.addAll(structuredContents);
        combined.addAll(unstructuredResults);
        return combined;
    }
}
