package com.example.mall.search.service;

import com.example.mall.search.vo.SearchParamVo;
import com.example.mall.search.vo.SearchResult;

public interface MallSearchService {
    SearchResult search(SearchParamVo param);
}
