package com.itheima.leyou.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
public class StockDaoImpl implements IStockDao {

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Override
    public ArrayList<Map<String, Object>> getStockList() {

            //1、创建商品查询的SQL
            String sql = "select id AS sku_id, title, images, stock, price, indexes, own_spec " +
                    "from tb_sku";

            //2、执行这个SQL
            ArrayList<Map<String, Object>> list = (ArrayList<Map<String, Object>>) jdbcTemplate.queryForList(sql);

            //3、返回信息
            return list;
    }
}
