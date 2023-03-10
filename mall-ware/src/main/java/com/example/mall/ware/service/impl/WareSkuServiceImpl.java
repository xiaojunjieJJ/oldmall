package com.example.mall.ware.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.example.common.exception.NoStockException;
import com.example.common.to.SkuHasStockVo;
import com.example.common.to.mq.OrderTo;
import com.example.common.to.mq.StockDetailTo;
import com.example.common.to.mq.StockLockedTo;
import com.example.common.utils.R;
import com.example.mall.ware.entity.WareOrderTaskDetailEntity;
import com.example.mall.ware.entity.WareOrderTaskEntity;
import com.example.mall.ware.feign.OrderFeignService;
import com.example.mall.ware.feign.ProductFeignService;
import com.example.mall.ware.service.WareOrderTaskDetailService;
import com.example.mall.ware.service.WareOrderTaskService;
import com.example.mall.ware.vo.OrderItemVo;
import com.example.mall.ware.vo.OrderVo;
import com.example.mall.ware.vo.WareSkuLockVo;
import lombok.Data;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.common.utils.PageUtils;
import com.example.common.utils.Query;

import com.example.mall.ware.dao.WareSkuDao;
import com.example.mall.ware.entity.WareSkuEntity;
import com.example.mall.ware.service.WareSkuService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuDao, WareSkuEntity> implements WareSkuService {

    @Autowired
    WareSkuDao wareSkuDao;
    @Autowired
    ProductFeignService productFeignService;
    @Autowired
    WareOrderTaskService orderService;
    @Autowired
    WareOrderTaskDetailService orderDetailService;
    @Autowired
    WareOrderTaskService wareOrderTaskService;
    @Autowired
    OrderFeignService orderFeignService;
    @Autowired
    RabbitTemplate rabbitTemplate;


    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        QueryWrapper<WareSkuEntity> queryWrapper = new QueryWrapper<>();
        String skuId = (String) params.get("skuId");
        if (!StringUtils.isEmpty(skuId)) {
            queryWrapper.eq("sku_id", skuId);
        }
        String wareId = (String) params.get("wareId");
        if (!StringUtils.isEmpty(wareId)) {
            queryWrapper.eq("ware_id", wareId);
        }
        IPage<WareSkuEntity> page = this.page(
                new Query<WareSkuEntity>().getPage(params),
                queryWrapper
        );
        return new PageUtils(page);
    }

    @Transactional
    @Override
    public void addStock(Long skuId, Long wareId, Integer skuNum) {
        //?????????????????????????????????,??????????????????;???????????????????????????
        List<WareSkuEntity> wareSkuEntities = wareSkuDao.selectList(new QueryWrapper<WareSkuEntity>().eq("sku_id", skuId).eq("ware_id", wareId));
        if (wareSkuEntities == null || wareSkuEntities.size() == 0) {
            WareSkuEntity wareSkuEntity = new WareSkuEntity();
            wareSkuEntity.setSkuId(skuId);
            wareSkuEntity.setStock(skuNum);
            wareSkuEntity.setWareId(wareId);

            try {
                R info = productFeignService.info(skuId);
                Map<String, Object> data = (Map<String, Object>) info.get("skuInfo");
                if (info.getCode() == 0) {
                    wareSkuEntity.setSkuName((String) data.get("skuName"));
                }
            } catch (Exception e) {

            }

            wareSkuDao.insert(wareSkuEntity);
        } else {
            wareSkuDao.addStock(skuId, wareId, skuNum);
        }
    }

    @Override
    public List<SkuHasStockVo> getSkuHasStock(List<Long> skuIds) {
        List<SkuHasStockVo> collect = skuIds.stream().map(skuId -> {
            SkuHasStockVo skuHasStockVo = new SkuHasStockVo();
            //????????????????????????
            Long count = baseMapper.getSkuStock(skuId);
            skuHasStockVo.setSkuId(skuId);
            //count==null?false:count>0 = count==null?false:&&count>0
            skuHasStockVo.setHasStock(count == null ? false : count > 0);
            return skuHasStockVo;
        }).collect(Collectors.toList());
        return collect;

    }

    @Transactional(rollbackFor = NoStockException.class)
    @Override
    public Boolean orderLockStock(WareSkuLockVo vo) {
        //???????????????????????????,????????????
        WareOrderTaskEntity taskEntity = new WareOrderTaskEntity();
        taskEntity.setOrderSn(vo.getOrderSn());
        orderService.save(taskEntity);
        List<OrderItemVo> locks = vo.getLocks();
        List<SkuWareHasStock> collect = locks.stream().map(item -> {
            SkuWareHasStock stock = new SkuWareHasStock();
            Long skuId = item.getSkuId();
            stock.setSkuId(skuId);
            stock.setNum(item.getCount());
            //????????????????????????
            List<Long> wareIds = wareSkuDao.listWareIdHasSkuStock(skuId);
            stock.setWareId(wareIds);
            return stock;
        }).collect(Collectors.toList());

        //????????????
        for (SkuWareHasStock hasStock : collect) {
            Boolean skuStocked = false;
            Long skuId = hasStock.getSkuId();
            List<Long> wareIds = hasStock.getWareId();
            if (wareIds == null || wareIds.size() == 0) {
                //????????????
                throw new NoStockException(skuId);
            }
            for (Long wareId : wareIds) {
                //??????1??????(1????????????),??????0??????(0????????????)
                Long count = wareSkuDao.lockSkuStock(skuId, wareId, hasStock.getNum());
                if (count == 1) {
                    skuStocked = true;
                    WareOrderTaskDetailEntity detailEntity = new WareOrderTaskDetailEntity(null, skuId, "", hasStock.getNum(), taskEntity.getId(), wareId, 1);
                    orderDetailService.save(detailEntity);
                    StockLockedTo to = new StockLockedTo();
                    StockDetailTo detailTo = new StockDetailTo();
                    to.setId(taskEntity.getId());
                    BeanUtils.copyProperties(detailEntity, detailTo);
                    //?????????????????????????????????,???????????????????????????
                    to.setDetail(detailTo);
                    rabbitTemplate.convertAndSend("stock-event-exchange", "stock.locked", to);
                    break;
                } else {
                    //????????????????????????????????????????????????
                }
            }
            if (skuStocked == false) {
                //?????????????????????????????????(???????????????)
                throw new NoStockException(skuId);
            }
        }
        return true;
    }

    @Data
    class SkuWareHasStock {
        private Long skuId;
        private Integer num;
        private List<Long> wareId;
    }

    @Override
    public void unlockStock(StockLockedTo to) {
        StockDetailTo detail = to.getDetail();
        Long detailId = detail.getId();
        //????????????
        //1.?????????????????????????????????????????????
        WareOrderTaskDetailEntity orderTaskDetailEntity = orderDetailService.getById(detailId);
        if (orderTaskDetailEntity != null) {
            //???,?????????????????????????????????????????????
            Long id = to.getId();//???????????????Id
            WareOrderTaskEntity taskEntity = wareOrderTaskService.getById(id);
            String orderSn = taskEntity.getOrderSn();
            R r = orderFeignService.getOrderStatus(orderSn);
            if (r.getCode() == 0) {
                OrderVo data = r.getData(new TypeReference<OrderVo>() {
                });
                if (data == null || data.getStatus() == 4) {
                    //?????????????????? ?????? ????????????????????????????????????,????????????
                    //???????????????1,????????????
                    if (orderTaskDetailEntity.getLockStatus() == 1) {
                        unLockStock(detail.getSkuId(), detail.getWareId(), detail.getSkuNum(), detailId);
                    }
                }
            } else {
                //????????????(??????????????????)?????????
                throw new RuntimeException("??????????????????");
            }
        } else {
            //?????????????????????????????????????????????????????????????????????
        }
    }

    /*
     *??????????????????????????????,??????????????????????????????????????????????????????
     */
    @Transactional
    @Override
    public void unlockStock(OrderTo orderTo) {
        String orderSn = orderTo.getOrderSn();
        //????????????????????????
        WareOrderTaskEntity task = wareOrderTaskService.getOrderTaskByOrderSn(orderSn);
        Long id = task.getId();
        List<WareOrderTaskDetailEntity> list = orderDetailService.list(new QueryWrapper<WareOrderTaskDetailEntity>().eq("task_id", id)
                .eq("lock_status", 1));
        for (WareOrderTaskDetailEntity entity : list) {
            unLockStock(entity.getSkuId(), entity.getWareId(), entity.getSkuNum(), entity.getId());
        }
    }

    public void unLockStock(Long skuId, Long wareId, Integer num, Long taskDetailId) {
        //????????????
        wareSkuDao.unlockStock(skuId, wareId, num);
        //???????????????????????????
        WareOrderTaskDetailEntity entity = new WareOrderTaskDetailEntity();
        entity.setId(taskDetailId);
        entity.setLockStatus(2);
        orderDetailService.updateById(entity);
    }

}