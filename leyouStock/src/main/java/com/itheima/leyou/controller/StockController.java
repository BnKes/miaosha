package com.itheima.leyou.controller;

import com.itheima.leyou.service.IStockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class StockController {

    @Autowired
    private IStockService iStockService;

    @RequestMapping(value = "/getStockList")
    public Map<String, Object> getStockList(){
        return iStockService.getStockList();
    }
}
