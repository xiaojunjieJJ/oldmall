package com.example.mall.search;

import com.alibaba.fastjson.JSON;
import com.example.mall.search.config.MallElasticSearchConfig;
import lombok.Data;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

@RunWith(SpringRunner.class)
@SpringBootTest
public class MallSearchApplicationTests {
    @Autowired
    RestHighLevelClient client;

    @Test
    public void contextLoads() {
        System.out.println(client);
    }

    //聚合查询
    @Test
    public void searchData() throws IOException {
        SearchRequest request = new SearchRequest();
        request.indices("bank");
        SearchSourceBuilder builder = new SearchSourceBuilder();
        builder.query(QueryBuilders.matchQuery("address", "mill"));


        builder.aggregation(AggregationBuilders.terms("ageAgg").field("age"));
        builder.aggregation(AggregationBuilders.avg("balanceAgg").field("balance"));
        System.out.println(builder.toString());


        request.source(builder);
        SearchResponse response = client.search(request, MallElasticSearchConfig.COMMON_OPTIONS);

        System.out.println(response.toString());
        Aggregations aggregations = response.getAggregations();
        Terms ageAgg = aggregations.get("ageAgg");
        for (Terms.Bucket bucket : ageAgg.getBuckets()) {
            String string = bucket.getKeyAsString();
            System.out.println("年龄" + string);
        }

    }

    //新增索引
    @Test
    public void indeData() throws IOException {
        IndexRequest users = new IndexRequest("users");
        users.id("1");
        User user = new User();
        user.setUserName("zhangsan");
        user.setGender("男");
        user.setAge(18);
        String s = JSON.toJSONString(user);
        users.source(s, XContentType.JSON);

        IndexResponse index = client.index(users, MallElasticSearchConfig.COMMON_OPTIONS);
        System.out.println(index);
    }

    @Data
    class User {
        String userName;
        String gender;
        Integer age;
    }

}
