package com.example.mall.product;

import com.example.mall.product.entity.BrandEntity;
import com.example.mall.product.service.BrandService;
import com.example.mall.product.service.CategoryBrandRelationService;
import com.example.mall.product.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.List;

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
//    OSSClient ossClient;

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

    @Test
    public void testFindPath(){
        Long[] catelogPath = categoryService.findCatelogPath(225L);
        log.info("完整路径：{}",Arrays.asList(catelogPath));
    }

    @Test
    public void testRelation(){
        List<BrandEntity> vos = relationService.getBrandsByCatId(225L);
        for (BrandEntity vo : vos) {
            System.out.println(vo);
        }
    }

}
