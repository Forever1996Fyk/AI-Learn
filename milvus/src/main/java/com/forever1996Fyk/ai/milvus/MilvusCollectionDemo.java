package com.forever1996Fyk.ai.milvus;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.common.DataType;
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.service.collection.request.AddFieldReq;
import io.milvus.v2.service.collection.request.CreateCollectionReq;
import io.milvus.v2.service.utility.request.FlushReq;
import io.milvus.v2.service.vector.request.DeleteReq;
import io.milvus.v2.service.vector.request.GetReq;
import io.milvus.v2.service.vector.request.InsertReq;
import io.milvus.v2.service.vector.response.DeleteResp;
import io.milvus.v2.service.vector.response.GetResp;
import io.milvus.v2.service.vector.response.InsertResp;
import io.milvus.v2.service.vector.response.QueryResp;

import java.util.ArrayList;
import java.util.List;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/4/27 23:27
 **/
public class MilvusCollectionDemo {

    private final static String MILVUS_URL = "http://localhost:19530";

    private final static String TOKEN = "root:Milvus";

    public static void main(String[] args) {
        // 连接 milvus
        ConnectConfig connectConfig = ConnectConfig.builder()
                .uri(MILVUS_URL)
                .token(TOKEN)
                .build();

        MilvusClientV2 client = new MilvusClientV2(connectConfig);
        // 创建 Collection
        String collectionName = "demo_collection";
        createCollection(client, collectionName);

        // 列出 Collections
//        listCollections(client);

        // 向 Collection 插入数据
//        insertDataIntoCollection(client, collectionName);

        // 查询数据
//        getData(client, collectionName);

        // 删除数据
//        deleteData(client, collectionName);
    }

    private static void listCollections(MilvusClientV2 client) {
        List<String> collectionNames = client.listCollections().getCollectionNames();
        System.out.println(collectionNames);
    }

    private static void createCollection(MilvusClientV2 client, String collectionName) {
        // 创建 schema
        var schema = MilvusClientV2.CreateSchema()
                .addField(
                        AddFieldReq.builder()
                                .fieldName("id")
                                .dataType(DataType.Int64)
                                // 表示主键
                                .isPrimaryKey(true)
                                .autoID(false).build()
                )
                .addField(
                        AddFieldReq.builder()
                                .fieldName("vector")
                                .dataType(DataType.FloatVector)
                                // 向量维度，这里表示 5 维向量
                                .dimension(5)
                                .build()
                )
                .addField(
                        AddFieldReq.builder()
                                .fieldName("color")
                                .dataType(DataType.VarChar)
                                .maxLength(512)
                                .build()
                );

        // 构建索引，方便高效查询数据
        List<IndexParam> indexParams = new ArrayList<>();
        var vector = IndexParam.builder()
                .fieldName("vector")
                // 默认索引类型就是IVF_FLAT
                .indexType(IndexParam.IndexType.IVF_FLAT)
                // 余弦相似度
                .metricType(IndexParam.MetricType.COSINE)
                .build();
        indexParams.add(vector);

        // 创建 Collection
        client.createCollection(CreateCollectionReq.builder()
                .collectionName(collectionName)
                .collectionSchema(schema)
                .indexParams(indexParams)
                .build());
    }


    private static void insertDataIntoCollection(MilvusClientV2 client, String collectionName) {
        // 准备数据
        Gson gson = new Gson();
        List<JsonObject> data = new ArrayList<>();


        // 插入数据
        // 如果多次插入，那么相同的数据会替换，也是如果未存在主键，则插入，否则即更新
        InsertResp resp = client.insert(
                InsertReq.builder()
                        .collectionName(collectionName)
                        .data(data)
                        .build()
        );
        System.out.println("插入数据行数：" + resp.getInsertCnt());
        // 这里要注意，milvus 默认是异步落盘，所以这里需要等待一下，或者强制刷盘
        client.flush(
                FlushReq.builder()
                        .collectionNames(List.of(collectionName))
                        .build()
        );

        System.out.println("插入数据成功");
    }


    private static void getData(MilvusClientV2 client, String collectionName) {
        // 这里 search 表示检索向量
//        client.search();

        // 如果想像关系型数据库一样查询数据使用get方法
        GetResp getResp = client.get(
                GetReq.builder()
                        .collectionName(collectionName)
                        .ids(List.of(1, 2, 3))
                        .outputFields(List.of("id", "color"))
                        .build()
        );
        List<QueryResp.QueryResult> getResults = getResp.getGetResults();
        for (QueryResp.QueryResult getResult : getResults) {
            System.out.println(getResult.toString());
        }
    }


    private static void deleteData(MilvusClientV2 client, String collectionName) {
        DeleteResp deleteResp = client.delete(
                DeleteReq.builder()
                        .collectionName(collectionName)
                        .ids(List.of(0, 1))
                        .build()
        );
        System.out.println("删除数据行：" + deleteResp.getDeleteCnt());
    }
}
