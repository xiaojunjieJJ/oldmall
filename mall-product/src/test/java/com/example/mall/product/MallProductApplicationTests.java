package com.example.mall.product;

import com.example.mall.product.entity.BrandEntity;
import com.example.mall.product.service.BrandService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class MallProductApplicationTests {
    @Autowired
    BrandService brandService;

    @Test
    void contextLoads() {
        BrandEntity xiaomi = new BrandEntity();
        xiaomi.setName("小米");
        xiaomi.setDescript("Are you OK?");
        xiaomi.setFirstLetter("X");
        brandService.save(xiaomi);
    }

}
