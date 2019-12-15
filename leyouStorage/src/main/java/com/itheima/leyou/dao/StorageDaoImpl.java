package com.itheima.leyou.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Repository
@Transactional
public class StorageDaoImpl implements  IStorageDao{

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Override
    public Map<String, Object> insertStorage(String sku_id, double in_quanty, double out_quanty) {

        Map<String, Object> resultMap = new HashMap<String, Object>();

        //1. 先查询库存主表是否有该商品
        String sql = "select * from tb_stock_storage where sku_id = ?";
        ArrayList<Map<String, Object>> list = (ArrayList<Map<String, Object>>) jdbcTemplate.queryForList(sql, sku_id);

        //2.判断主表有没有，有的话获取sku_id,再写入历史表
        int new_id = 0;
        double thisQuanty = in_quanty - out_quanty;
        boolean result = false;

        //获取id
        if(list!=null&&list.size()>0){
            //主表有的时候，直接写入历史表，并且获取id
            new_id = Integer.parseInt(list.get(0).get("id").toString());
        }else{
            //3、主表没有的时候，写入主表，并且获取id，再写入历史表
            GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
            sql = "insert into tb_stock_storage (warehouse_id,sku_id,quanty) values (1,"+sku_id+","+thisQuanty+")";
            final String finalSql = sql;

            result = jdbcTemplate.update(new PreparedStatementCreator() {
                @Override
                public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
                    PreparedStatement preparedStatement = connection.prepareStatement(finalSql, Statement.RETURN_GENERATED_KEYS);
                    return preparedStatement;
                }
            }, keyHolder) == 1;

            if(!result){
                resultMap.put("result",false);
                resultMap.put("msg","写入库存主表时失败");
                return resultMap;
            }
            //得到新增的id
            new_id = keyHolder.getKey().intValue();
        }

        //4.写入历史表
        sql = "INSERT INTO tb_stock_storage_history (stock_storage_id, in_quanty, out_quanty) " +
                "VALUES (?, ?, ?)";
        result = jdbcTemplate.update(sql, new_id, in_quanty, out_quanty)==1;
        if (!result){
            resultMap.put("result", false);
            resultMap.put("msg", "写入库存历史表时失败！");
            return resultMap;
        }

        //5. 更新主表库存
        if(list!=null && list.size()>0){
            sql = "update tb_stock_storage set quanty = quanty + "+thisQuanty+" where id = "+new_id;
             result = jdbcTemplate.update(sql)==1;
             if (!result){
                 resultMap.put("result", false);
                 resultMap.put("msg", "更新库存主表时失败！");
                 return resultMap;
             }
        }
        //6、返回信息
        resultMap.put("result", true);
        resultMap.put("msg", "写入库存成功！");
        return resultMap;
    }
}
