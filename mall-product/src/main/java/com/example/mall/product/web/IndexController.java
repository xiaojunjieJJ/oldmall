package com.example.mall.product.web;

import com.example.mall.product.entity.CategoryEntity;
import com.example.mall.product.service.CategoryService;
import com.example.mall.product.vo.Catelog2Vo;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

@Controller
public class IndexController {
    @Autowired
    CategoryService categoryService;

    @Autowired
    RedissonClient redisson;

    @GetMapping(value = {"/", "/index.html"})
    public String indexPage(Model model) {
        List<CategoryEntity> entityList = categoryService.getLevel1Categorys();
        model.addAttribute("categorys", entityList);
        return "index";
    }

    @ResponseBody
    @GetMapping("index/catalog.json")
    public Map<String, List<Catelog2Vo>> getCatalogJson() {
        Map<String, List<Catelog2Vo>> catalogJson = categoryService.getCatalogJson();
        return catalogJson;
    }

    @ResponseBody
    @GetMapping("/hello")
    public String hello() {
        //测试Redisson的lock
        RLock mylock = redisson.getLock("mylock");
        mylock.lock();
        try {
            System.out.println("枷锁成功");
            Thread.sleep(30000);
        } catch (Exception e) {
        } finally {
            System.out.println("解锁");
            mylock.unlock();
        }
        return "hello";
    }
}
