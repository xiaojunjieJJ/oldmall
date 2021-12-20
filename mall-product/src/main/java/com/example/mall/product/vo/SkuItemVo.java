package com.example.mall.product.vo;

import com.example.mall.product.entity.SkuImagesEntity;
import com.example.mall.product.entity.SkuInfoEntity;
import com.example.mall.product.entity.SpuInfoDescEntity;
import lombok.Data;

import java.util.List;

@Data
public class SkuItemVo {
    SkuInfoEntity info;
    boolean hasStock = true;
    List<SkuImagesEntity> imagesEntites;
    SpuInfoDescEntity desp;
    List<SkuItemSaleAttrsVo> saleAttr;
    List<SpuItemAttrGroupVo> groupAttrs;
}
