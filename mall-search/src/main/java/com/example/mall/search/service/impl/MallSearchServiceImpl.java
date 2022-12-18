package com.example.mall.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.example.common.to.es.SkuEsModel;
import com.example.common.utils.R;
import com.example.mall.search.config.MallElasticSearchConfig;
import com.example.mall.search.constant.EsConstant;
import com.example.mall.search.feign.ProductFeignService;
import com.example.mall.search.service.MallSearchService;
import com.example.mall.search.vo.AttrResponseVo;
import com.example.mall.search.vo.BrandVo;
import com.example.mall.search.vo.SearchParamVo;
import com.example.mall.search.vo.SearchResult;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MallSearchServiceImpl implements MallSearchService {

    @Autowired
    RestHighLevelClient client;

    @Autowired
    ProductFeignService productFeignService;

    @Override
    public SearchResult search(SearchParamVo param) {
        SearchResult result = null;
        //1.构建DSL语句
        SearchRequest searchRequest = buildSearchRequest(param);
        try {
            //2.执行检索请求
            SearchResponse response = client.search(searchRequest, MallElasticSearchConfig.COMMON_OPTIONS);
            //3.分析响应数据，封装为期望的格式
            result = buildSearchResult(response, param);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    //构建检索请求
    private SearchRequest buildSearchRequest(SearchParamVo param) {

        SearchSourceBuilder builder = new SearchSourceBuilder();

        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

        // 1.1:must-模糊匹配
        if (!StringUtils.isEmpty(param.getKeyword())) {
            boolQuery.must(QueryBuilders.matchQuery("skuTitle", param.getKeyword()));
        }

        // 1.2:filter-按三级分类ID查询
        if (param.getCatalog3Id() != null) {
            boolQuery.filter(QueryBuilders.termQuery("catalogId", param.getCatalog3Id()));
        }

        //1.2:filter-按品牌ID查询
        if (param.getBrandId() != null && param.getBrandId().size() > 0) {
            boolQuery.filter(QueryBuilders.termsQuery("brandId", param.getBrandId()));
        }

        //1.2:filter-按照指定属性查询(嵌入式查询)
        if (param.getAttrs() != null && param.getAttrs().size() > 0) {
            for (String attrStr : param.getAttrs()) {
                BoolQueryBuilder nestedBoolQuery = QueryBuilders.boolQuery();
                String[] s = attrStr.split("_");
                String attrId = s[0];
                String[] attrValues = s[1].split(":");
                nestedBoolQuery.must(QueryBuilders.termQuery("attrs.attrId", attrId));
                nestedBoolQuery.must(QueryBuilders.termsQuery("attrs.attrValue", attrValues));
                NestedQueryBuilder nestedQuery = QueryBuilders.nestedQuery("attrs", nestedBoolQuery, ScoreMode.None);
                boolQuery.filter(nestedQuery);
            }
        }

        //1.2:filter-按照库存进行查询
        if (param.getHasStock() != null) {
            boolQuery.filter(QueryBuilders.termQuery("hasStock", param.getHasStock() == 1));
        }

        //1.2:filter-按照价格区间查询
        if (!StringUtils.isEmpty(param.getSkuPrice())) {
            //1_500/_500/500_
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("skuPrice");
            String[] s = param.getSkuPrice().split("_");
            BigDecimal bigDecimal1 = new BigDecimal(s[0]);
            if (s.length == 2) {
                //价格区间1_500
                BigDecimal bigDecimal2 = new BigDecimal(s[1]);
                rangeQuery.gte(bigDecimal1).lte(bigDecimal2);
            } else if (s.length == 1) {
                //小于_500
                if (param.getSkuPrice().startsWith("_")) {
                    rangeQuery.lte(bigDecimal1);
                }
                //大于500_
                if (param.getSkuPrice().endsWith("_")) {
                    rangeQuery.gte(bigDecimal1);
                }
            }
            boolQuery.filter(rangeQuery);
        }

        //所有查询条件拼装完成
        builder.query(boolQuery);

        //2:排序
        if (!StringUtils.isEmpty(param.getSort())) {
            String sort = param.getSort();
            String[] s = sort.split("_");
            SortOrder order = s[1].equalsIgnoreCase("asc") ? SortOrder.ASC : SortOrder.DESC;
            builder.sort(s[0], order);
        }

        //3:分页
        builder.from((param.getPageNum() - 1) * EsConstant.PRODUCT_PAGESIZE);
        builder.size(EsConstant.PRODUCT_PAGESIZE);

        //4:高亮
        if (!StringUtils.isEmpty(param.getKeyword())) {
            HighlightBuilder highlightBuilder = new HighlightBuilder();
            highlightBuilder.field("skuTitle");
            highlightBuilder.preTags("<b style='color:red'>");
            highlightBuilder.postTags("</b>");
            builder.highlighter(highlightBuilder);
        }

        //1.品牌聚合
        TermsAggregationBuilder brand_agg = AggregationBuilders.terms("brand_agg");
        brand_agg.field("brandId").size(50);
        //子聚合拿品牌名字和品牌图片
        brand_agg.subAggregation(AggregationBuilders.terms("brand_name_agg").field("brandName").size(1));
        brand_agg.subAggregation(AggregationBuilders.terms("brand_img_agg").field("brandImg").size(1));
        builder.aggregation(brand_agg);

        //2.分类聚合
        TermsAggregationBuilder catalog_agg = AggregationBuilders.terms("catalog_agg").field("catalogId").size(20);
        //子聚合拿分类名字
        catalog_agg.subAggregation(AggregationBuilders.terms("catalog_name_agg").field("catalogName").size(1));
        builder.aggregation(catalog_agg);

        //3.属性聚合(嵌入式聚合)
        NestedAggregationBuilder attr_agg = AggregationBuilders.nested("attr_agg", "attrs");
        //聚合出当前所有的attr_id
        TermsAggregationBuilder attr_id_agg = AggregationBuilders.terms("attr_id_agg").field("attrs.attrId");
        //聚合分析当前attr_id对应的名字
        attr_id_agg.subAggregation(AggregationBuilders.terms("attr_name_agg").field("attrs.attrName").size(1));
        //聚合分析当前attr_id对应的所有可能的属性值attrValue
        attr_id_agg.subAggregation(AggregationBuilders.terms("attr_value_agg").field("attrs.attrValue").size(50));
        attr_agg.subAggregation(attr_id_agg);

        //聚合拼装完成
        builder.aggregation(attr_agg);

        SearchRequest request = new SearchRequest(new String[]{EsConstant.PRODUCT_INDEX}, builder);
        return request;
    }

    //封装检索结果
    private SearchResult buildSearchResult(SearchResponse response, SearchParamVo paramVo) {

        SearchResult result = new SearchResult();

        //1.返回所有查询到的商品
        SearchHits hits = response.getHits();
        List<SkuEsModel> esModelList = new ArrayList<>();
        if (hits.getHits() != null && hits.getHits().length > 0) {
            for (SearchHit hit : hits.getHits()) {
                String sourceAsString = hit.getSourceAsString();
                SkuEsModel skuEsModel = JSON.parseObject(sourceAsString, SkuEsModel.class);
                if (!StringUtils.isEmpty(paramVo.getKeyword())) {
                    //设置高亮内容
                    HighlightField skuTitle = hit.getHighlightFields().get("skuTitle");
                    String string = skuTitle.getFragments()[0].string();
                    skuEsModel.setSkuTitle(string);
                }
                esModelList.add(skuEsModel);
            }
        }
        result.setProducts(esModelList);

        //2.当前所有商品涉及到的所有属性信息
        List<SearchResult.AttrVo> attrVos = new ArrayList<>();
        ParsedNested attr_agg = response.getAggregations().get("attr_agg");
        ParsedLongTerms attr_id_agg = attr_agg.getAggregations().get("attr_id_agg");//根据返回值确定数据类ParsedLongTerms，ParsedNested
        for (Terms.Bucket bucket : attr_id_agg.getBuckets()) {
            SearchResult.AttrVo attrVo = new SearchResult.AttrVo();
            //1.得到属性的id
            long attrId = bucket.getKeyAsNumber().longValue();
            //2.得到属性的名字
            String attrName = ((ParsedStringTerms) bucket.getAggregations().get("attr_name_agg")).getBuckets().get(0).getKeyAsString();
            //3.得到属性的所有值
            List<String> attrValues = ((ParsedStringTerms) bucket.getAggregations().get("attr_value_agg")).getBuckets().stream().map(item -> {
                String keyAsString = ((Terms.Bucket) item).getKeyAsString();
                return keyAsString;
            }).collect(Collectors.toList());
            attrVo.setAttrId(attrId);
            attrVo.setAttrName(attrName);
            attrVo.setAttrValue(attrValues);
            attrVos.add(attrVo);
        }
        result.setAttrs(attrVos);

        //3.当前所有商品涉及到的所有品牌信息
        List<SearchResult.BrandVo> brandVos = new ArrayList<>();
        ParsedLongTerms brand_agg = response.getAggregations().get("brand_agg");
        for (Terms.Bucket bucket : brand_agg.getBuckets()) {
            SearchResult.BrandVo brandVo = new SearchResult.BrandVo();
            //1.品牌id
            long brandId = bucket.getKeyAsNumber().longValue();
            //2.品牌名字
            String brandName = ((ParsedStringTerms) bucket.getAggregations().get("brand_name_agg")).getBuckets().get(0).getKeyAsString();
            //3.品牌图片
            String brandImg = ((ParsedStringTerms) bucket.getAggregations().get("brand_img_agg")).getBuckets().get(0).getKeyAsString();
            brandVo.setBrandId(brandId);
            brandVo.setBrandName(brandName);
            brandVo.setBrandImg(brandImg);
            brandVos.add(brandVo);
        }
        result.setBrands(brandVos);

        //4.当前所有商品涉及到的所有分类信息
        ParsedLongTerms catalog_agg = response.getAggregations().get("catalog_agg");
        List<SearchResult.CatalogVo> catalogVos = new ArrayList<>();
        List<? extends Terms.Bucket> buckets = catalog_agg.getBuckets();
        for (Terms.Bucket bucket : buckets) {
            SearchResult.CatalogVo catalogVo = new SearchResult.CatalogVo();
            //得到分类id
            String keyAsString = bucket.getKeyAsString();
            catalogVo.setCatalogId(Long.parseLong(keyAsString));
            //得到分类名
            ParsedStringTerms catalog_name_agg = bucket.getAggregations().get("catalog_name_agg");
            String catalog_name = catalog_name_agg.getBuckets().get(0).getKeyAsString();
            catalogVo.setCatalogName(catalog_name);
            catalogVos.add(catalogVo);
        }
        result.setCatalogs(catalogVos);

        //5.当前所有商品涉及到的所有分页信息
        //当前页
        result.setPageNum(paramVo.getPageNum());
        //总计录数
        long total = hits.getTotalHits().value;
        result.setTotal(total);
        //总页码
        Long totalPages = total % EsConstant.PRODUCT_PAGESIZE == 0 ? total / EsConstant.PRODUCT_PAGESIZE : (total / EsConstant.PRODUCT_PAGESIZE) + 1;
        result.setTotalPage(totalPages);

        List<Integer> pageNavs = new ArrayList<>();
        for (int i = 1; i <= totalPages; i++) {
            pageNavs.add(i);
        }
        result.setPageNavs(pageNavs);

        //6.属性面包屑导航
        if (paramVo.getAttrs() != null && paramVo.getAttrs().size() > 0) {
            List<SearchResult.NavVo> collect = paramVo.getAttrs().stream().map(attr -> {
                SearchResult.NavVo navVo = new SearchResult.NavVo();
                String[] s = attr.split("_");
                navVo.setNavValue(s[1]);
                R r = productFeignService.attrInfo(Long.parseLong(s[0]));
                result.getAttrIds().add(Long.parseLong(s[0]));
                if (r.getCode() == 0) {
                    AttrResponseVo attrs = r.getData2("attr", new TypeReference<AttrResponseVo>() {
                    });
                    navVo.setNavName(attrs.getAttrName());
                } else {
                    navVo.setNavName(s[0]);
                }
                //取消该面包屑
                String replace = replaceQueryString(paramVo, attr, "attrs");
                navVo.setLink("http://search.jiegemall.com/list.html?" + replace);
                return navVo;
            }).collect(Collectors.toList());
            result.setNavs(collect);
        }

        //7.品牌、分类面包屑导航
        if (paramVo.getBrandId() != null && paramVo.getBrandId().size() > 0) {
            List<SearchResult.NavVo> navs = result.getNavs();
            SearchResult.NavVo navVo = new SearchResult.NavVo();
            navVo.setNavName("品牌");
            //远程查询所有品牌
            R r = productFeignService.brandsInfo(paramVo.getBrandId());
            if (r.getCode() == 0) {
                List<BrandVo> brands = r.getData2("brands", new TypeReference<List<BrandVo>>() {
                });
                StringBuffer buffer = new StringBuffer();
                String replace = "";
                for (BrandVo brandVo : brands) {
                    buffer.append(brandVo.getName() + ";");
                    replace = replaceQueryString(paramVo, brandVo.getBrandId() + "", "brandId");
                }
                navVo.setNavValue(buffer.toString());
                navVo.setLink("http://search.jiegemall.com/list.html?" + replace);
            }
            navs.add(navVo);
        }

        return result;
    }

    private String replaceQueryString(SearchParamVo paramVo, String value, String key) {
        //取消了面包屑之后,跳转的位置(将请求地址的url替换,置空)
        String encode = null;
        try {
            //编码
            encode = URLEncoder.encode(value, "UTF-8");
            encode = encode.replace("+", "%20");//对空格特殊处理(将空格变为%20)
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return paramVo.get_queryString().replace("&" + key + "=" + encode, "");
    }

}
