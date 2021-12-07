package com.example.mall.coupon.dao;

import com.example.mall.coupon.entity.CouponEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 优惠券信息
 * 
 * @author xiaojunjie
 * @email hninee@163.com
 * @date 2021-12-07 21:49:13
 */
@Mapper
public interface CouponDao extends BaseMapper<CouponEntity> {
	
}
