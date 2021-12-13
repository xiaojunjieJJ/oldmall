package com.example.mall.member.feign;

import com.example.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;

//是注册中心的服务名字
@FeignClient("mall-coupon")
public interface CouponFeignService {

    //把需要调用的方法的方法签名，补全地址
    @RequestMapping("/coupon/coupon/member/list")
    public R memberCoupon();

}
