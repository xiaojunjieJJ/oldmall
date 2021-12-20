package com.example.mall.product;

import com.example.mall.product.dao.AttrGroupDao;
import com.example.mall.product.entity.BrandEntity;
import com.example.mall.product.service.BrandService;
import com.example.mall.product.service.CategoryBrandRelationService;
import com.example.mall.product.service.CategoryService;
import com.example.mall.product.service.SkuSaleAttrValueService;
import com.example.mall.product.vo.SkuItemSaleAttrsVo;
import com.example.mall.product.vo.SpuItemAttrGroupVo;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@SpringBootTest
@RunWith(SpringRunner.class)
public class MallProductApplicationTests {
    @Autowired
    BrandService brandService;
    @Autowired
    CategoryService categoryService;
    @Autowired
    CategoryBrandRelationService relationService;
    @Autowired
    StringRedisTemplate stringRedisTemplate;
    @Autowired
    RedissonClient redissonClient;
//    @Autowired
//    OSSClient ossClient;
    @Autowired
    AttrGroupDao attrGroupDao;
    @Autowired
    SkuSaleAttrValueService saleDao;

    @Test
    public void contextLoads() {
        BrandEntity xiaomi = new BrandEntity();
        xiaomi.setName("小米");
        xiaomi.setDescript("Are you OK?");
        xiaomi.setFirstLetter("X");
        brandService.save(xiaomi);
    }

    @Test
    public void testOSSUpLoad() throws FileNotFoundException {
//        InputStream inputStream = new FileInputStream("E:\\Dev\\Demo\\shopping-mall\\OSStest.jpg");
//        // 依次填写Bucket名称（例如examplebucket）和Object完整路径（例如exampledir/exampleobject.txt）。Object完整路径中不能包含Bucket名称。
//        ossClient.putObject("jjmall", "test.jpg", inputStream);
//
//        // 关闭OSSClient。
//        ossClient.shutdown();
//
//        System.out.println("上传完成");
    }

//    @Test
//    public void testFindPath() {
//        Long[] catelogPath = categoryService.findCatelogPath(225L);
//        log.info("完整路径：{}", Arrays.asList(catelogPath));
//    }

    @Test
    public void testRelation() {
        List<BrandEntity> vos = relationService.getBrandsByCatId(225L);
        for (BrandEntity vo : vos) {
            System.out.println(vo);
        }
    }

    @Test
    public void testRedis() {
        ValueOperations<String, String> ops = stringRedisTemplate.opsForValue();
        ops.set("hello", "world" + UUID.randomUUID().toString());
        String hello = ops.get("hello");
        System.out.println(hello);

    }

    @Test
    public void testRedisson() {
        System.out.println(redissonClient);
    }

    @Test
    public void testAttr() {
        List<SpuItemAttrGroupVo> spuId = attrGroupDao.getAttrGroupWithAttrsBySpuId(10L, 225L);
        System.out.println(spuId);
    }

    @Test
    public void saleDaotest(){
        List<SkuItemSaleAttrsVo> spuId = saleDao.getSaleAttrsBySpuId(10L);
        System.out.println(spuId);
    }
}
