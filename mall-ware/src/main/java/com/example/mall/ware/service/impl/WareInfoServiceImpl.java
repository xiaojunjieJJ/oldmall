package com.example.mall.ware.service.impl;

import org.springframework.stereotype.Service;

import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.common.utils.PageUtils;
import com.example.common.utils.Query;

import com.example.mall.ware.dao.WareInfoDao;
import com.example.mall.ware.entity.WareInfoEntity;
import com.example.mall.ware.service.WareInfoService;
import org.springframework.util.StringUtils;


@Service("wareInfoService")
public class WareInfoServiceImpl extends ServiceImpl<WareInfoDao, WareInfoEntity> implements WareInfoService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        QueryWrapper<WareInfoEntity> queryWrapper = new QueryWrapper<>();
        String key = (String) params.get("key");
        if (!StringUtils.isEmpty(key)) {
            queryWrapper.eq("id", key)
                    .or().like("name", key)
                    .or().like("address", key)
                    .or().like("areacode", key);
        }
        IPage<WareInfoEntity> page = this.page(
                new Query<WareInfoEntity>().getPage(params),
                queryWrapper
        );
        return new PageUtils(page);
    }

//    //根据收货地址，计算运费
//    @Override
//    public FareVo getFare(Long addrId) {
//        FareVo fareVo = new FareVo();
//        R r = memberFeignService.addrInfo(addrId);
//        MemberAddressVo data = r.getData2("memberReceiveAddress", new TypeReference<MemberAddressVo>() {
//        });
//        if (data != null) {
//            String phone = data.getPhone();
//            String substring = phone.substring(phone.length() - 2, phone.length());
//            fareVo.setAddress(data);
//            fareVo.setFare(new BigDecimal(substring));
//            return fareVo;
//        }
//        return null;
//    }

}