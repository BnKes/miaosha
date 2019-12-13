package com.itheima.leyou.service;

import com.itheima.leyou.dao.IStockDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Repository
public class StockServiceImpl implements IStockService {

    @Autowired
    private IStockDao iStockDao;

    @Override
    public Map<String, Object> getStockList() {

        HashMap<String, Object> resultMap = new HashMap<>();
        ArrayList<Map<String, Object>> list = iStockDao.getStockList();

        if (list == null || list.size() == 0) {
            resultMap.put("result", false);
            resultMap.put("msg", "您没有取出商品信息！");
            return resultMap;
        }

        resultMap.put("sku_list", list);

        return resultMap;
    }
}
