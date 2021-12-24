package com.example.mall.seckill.service;

import com.example.mall.seckill.to.SeckillSkuRedisTo;

import java.util.List;

public interface SeckillService {
    void upSeckillSkuLatest3Days();

    List<SeckillSkuRedisTo> getCurrentSeckillSkus();

    SeckillSkuRedisTo getSkuSeckillInfo(Long skuId);

    String kill(String killId, String key, Integer num);

}
