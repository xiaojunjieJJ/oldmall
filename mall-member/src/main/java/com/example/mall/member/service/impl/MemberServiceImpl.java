package com.example.mall.member.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.example.common.to.SocialUser;
import com.example.common.utils.HttpUtils;
import com.example.mall.member.dao.MemberLevelDao;
import com.example.mall.member.entity.MemberLevelEntity;
import com.example.mall.member.member.PhoneExistException;
import com.example.mall.member.member.UserNameExistException;
import com.example.mall.member.vo.MemberLoginVo;
import com.example.mall.member.vo.MemberRegistVo;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.common.utils.PageUtils;
import com.example.common.utils.Query;

import com.example.mall.member.dao.MemberDao;
import com.example.mall.member.entity.MemberEntity;
import com.example.mall.member.service.MemberService;


@Service("memberService")
public class MemberServiceImpl extends ServiceImpl<MemberDao, MemberEntity> implements MemberService {

    @Autowired
    MemberLevelDao memberLevelDao;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<MemberEntity> page = this.page(
                new Query<MemberEntity>().getPage(params),
                new QueryWrapper<MemberEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public void regist(MemberRegistVo vo) {

        MemberDao dao = this.baseMapper;
        MemberEntity entity = new MemberEntity();
        //设置默认用户等级
        MemberLevelEntity levelEntity = memberLevelDao.getDefaultLevel();
        entity.setLevelId(levelEntity.getId());

        //检查数据是否唯一,为了让controller感知异常,异常机制
        checkPhoneUnique(vo.getPhone());
        checkUserNameUnique(vo.getUserName());
        entity.setMobile(vo.getPhone());
        entity.setUsername(vo.getUserName());
        entity.setNickname(vo.getUserName());

        //密码加密存储
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String encode = passwordEncoder.encode(vo.getPassword());
        entity.setPassword(encode);

        dao.insert(entity);

    }

    @Override
    public void checkPhoneUnique(String phone) throws PhoneExistException {
        MemberDao dao = this.baseMapper;
        Integer mobile = dao.selectCount(new QueryWrapper<MemberEntity>().eq("mobile", phone));
        if (mobile > 0) {
            throw new PhoneExistException();
        }
    }

    @Override
    public void checkUserNameUnique(String userName) throws UserNameExistException {
        MemberDao dao = this.baseMapper;
        Integer cout = dao.selectCount(new QueryWrapper<MemberEntity>().eq("username", userName));
        if (cout > 0) {
            throw new UserNameExistException();
        }
    }

    @Override
    public MemberEntity login(MemberLoginVo vo) {
        String loginacct = vo.getLoginacct();
        String password = vo.getPassword();
        //1.数据库查询
        MemberDao dao = this.baseMapper;
        MemberEntity entity = dao.selectOne(new QueryWrapper<MemberEntity>().eq("username", loginacct).or().eq("mobile", loginacct));
        if (entity == null) {
            return null;
        } else {
            String passwordDb = entity.getPassword();
            BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
            //密码匹配
            boolean matches = passwordEncoder.matches(password, passwordDb);
            if (matches) {
                return entity;
            } else {
                return null;
            }
        }
    }

    @Override
    public MemberEntity socialLogin(SocialUser vo) {
        //登录和注册合并逻辑
        String uid = vo.getUid();
        //1.判断当前社交用户是否曾经登录过系统
        MemberDao memberDao = this.baseMapper;
        MemberEntity memberEntity = memberDao.selectOne(new QueryWrapper<MemberEntity>().eq("social_uid", uid));
        if (memberEntity != null) {
            //2.1用户已经注册,换令牌
            MemberEntity update = new MemberEntity();
            update.setId(memberEntity.getId());
            update.setAccessToken(vo.getAccess_token());
            update.setExpiresIn(vo.getExpires_in());
            memberDao.updateById(update);
            memberEntity.setAccessToken(vo.getAccess_token());
            memberEntity.setExpiresIn(vo.getExpires_in());
            return memberEntity;
        } else {
            //2.2没有查到对应用户,用户第一次登录系统,需要注册
            MemberEntity regist = new MemberEntity();
            //2.3查询当前社交账号的信息
            try {
                Map<String, String> query = new HashMap<>();
                query.put("access_token", vo.getAccess_token());
                query.put("uid", vo.getUid());
                HttpResponse response = HttpUtils.doGet("https://api.weibo.com", "/2/users/show.json", "get", new HashMap<String, String>(), query);
                if (response.getStatusLine().getStatusCode() == 200) {
                    //查询成功
                    String json = EntityUtils.toString(response.getEntity());
                    JSONObject jsonObject = JSON.parseObject(json);
                    String name = jsonObject.getString("name");
                    String gender = jsonObject.getString("gender");
                    regist.setNickname(name);
                    regist.setGender("m".equals(gender) ? 1 : 0);
                }
            } catch (Exception e) {
            }
            regist.setSocialUid(vo.getUid());
            regist.setAccessToken(vo.getAccess_token());
            regist.setExpiresIn(vo.getExpires_in());
            memberDao.insert(regist);
            return regist;
        }
    }

}