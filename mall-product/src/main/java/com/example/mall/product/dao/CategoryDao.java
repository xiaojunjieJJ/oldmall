package com.example.mall.product.dao;

import com.example.mall.product.entity.CategoryEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品三级分类
 * 
 * @author xiaojunjie
 * @email hninee@163.com
 * @date 2021-12-07 20:51:57
 */
@Mapper
public interface CategoryDao extends BaseMapper<CategoryEntity> {
	
}
