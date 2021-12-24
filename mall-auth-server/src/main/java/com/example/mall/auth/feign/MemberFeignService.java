package com.example.mall.auth.feign;

import com.example.common.to.SocialUser;
import com.example.common.utils.R;
import com.example.mall.auth.vo.UserLoginVo;
import com.example.mall.auth.vo.UserRegistVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient("mall-member")
public interface MemberFeignService {

    @PostMapping("/member/member/regist")
    R regist(@RequestBody UserRegistVo vo);

    @PostMapping("/member/member/login")
    R login(@RequestBody UserLoginVo vo);

    @PostMapping("/member/member/oauth2/login")
    R SocialLogin(@RequestBody SocialUser vo) throws Exception;
}
