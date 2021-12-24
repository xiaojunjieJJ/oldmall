package com.example.mall.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.common.to.SocialUser;
import com.example.common.utils.PageUtils;
import com.example.mall.member.entity.MemberEntity;
import com.example.mall.member.member.PhoneExistException;
import com.example.mall.member.member.UserNameExistException;
import com.example.mall.member.vo.MemberLoginVo;
import com.example.mall.member.vo.MemberRegistVo;

import java.util.Map;

/**
 * 会员
 *
 * @author xiaojunjie
 * @email hninee@163.com
 * @date 2021-12-07 22:02:03
 */
public interface MemberService extends IService<MemberEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void regist(MemberRegistVo vo);

    void checkPhoneUnique(String phone) throws PhoneExistException;

    void checkUserNameUnique(String userName) throws UserNameExistException;

    MemberEntity login(MemberLoginVo vo);

    MemberEntity socialLogin(SocialUser vo);
}

