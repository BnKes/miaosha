package com.itheima.leyou.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.itheima.leyou.dao.IStockDao;
import com.netflix.discovery.converters.Auto;
import netscape.javascript.JSObject;
import org.hibernate.sql.OracleJoinFragment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;

import javax.transaction.Transactional;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Repository
public class StockServiceImpl implements IStockService {

    @Autowired
    private IStockDao iStockDao;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;//操作redis

    @Autowired
    private RestTemplate restTemplate;     //访问url

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


    @Override
    public Map<String, Object> getStock(String sku_id) {
        Map<String, Object> resultMap = new HashMap<String, Object>();

        //1、判断传入的参数是否为空
        if (sku_id == null || sku_id.equals("")) {
            resultMap.put("result", false);
            resultMap.put("msg", "您传入的参数有误！");
            return resultMap;
        }

        //2、取自iStockDao的方法
        ArrayList<Map<String, Object>> list = iStockDao.getStock(sku_id);

        //3、如果没有取出来，返回一个错误信息
        if (list == null || list.size() == 0) {
            resultMap.put("result", false);
            resultMap.put("msg", "您没有取出商品信息！");
            return resultMap;
        }

        //4.取出产品相应的政策
        resultMap = getLimitPolicy(list);

        //5、返回正常信息
        resultMap.put("sku", list);
        return resultMap;
    }


    //工具类，将信息写入MySQL和redis
    @Override
    @Transactional(rollbackOn = Exception.class)
    public Map<String, Object> insertLimitPolicy(Map<String, Object> policyInfo) {
        HashMap<String, Object> resultMap = new HashMap<>();

        //1.传入参数判读
        if (policyInfo == null || policyInfo.isEmpty()) {
            resultMap.put("result", false);
            resultMap.put("msg", "您传入的参数有误");
            return resultMap;
        }

        //2. 写入redis,StringRedisTemplate
        //redis 中取key： LIMIT_POLICY_{sku_id}, value: policyInfo --> String
        //设置redis存在的有效期， 有效期：结束时间减去当前时间

        long diff = 0;
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        String now = restTemplate.getForObject("http://leyou-time-server/getTime", String.class);
        try {
            Date end_time = simpleDateFormat.parse(policyInfo.get("end_time").toString());
            Date now_time = simpleDateFormat.parse(now);

            diff = (end_time.getTime() - now_time.getTime()) / 1000; //有效期为秒

            if (diff < 0) {
                resultMap.put("result", false);
                resultMap.put("msg", "结束时间不能小于当前时间");
                return resultMap;
            }

        } catch (ParseException e) {
            e.printStackTrace();
        }

        //3. 存入MySQL
        boolean result = iStockDao.insertLimitPolicy(policyInfo);

        //如果没有执行成功，返回错误信息
        if (!result) {
            resultMap.put("result", false);
            resultMap.put("msg", "数据库写入政策时失败！");
            return resultMap;
        }


        String policy = JSON.toJSONString(policyInfo);
        //4.1 商品政策写入Redis,并设置redis的有效期
        stringRedisTemplate.opsForValue().set("LIMIT_POLICY_" + policyInfo.get("sku_id"), policy, diff, TimeUnit.SECONDS);

        //4.2 从MySQL中将该商品信息取出写入redis
        ArrayList<Map<String, Object>> list = iStockDao.getStock(policyInfo.get("sku_id").toString());
        String sku = JSON.toJSONString(list.get(0));
        stringRedisTemplate.opsForValue().set("SKU_" + policyInfo.get("sku_id").toString(), sku, diff, TimeUnit.SECONDS);

        //5. 返回正常信息
        resultMap.put("result", true);
        resultMap.put("msg", "政策写入完毕！");
        return resultMap;
    }


    //工具类，从redis中得到商品政策
    private Map<String, Object> getLimitPolicy(ArrayList<Map<String, Object>> list) {
        HashMap<String, Object> resultMap = new HashMap<>();

        for (Map<String, Object> skuMap : list) {
            //3.1 根据sku_id,从redis取出产品相应的政策
            String policy = stringRedisTemplate.opsForValue().get("LIMIT_POLICY_" + skuMap.get("sku_id").toString());
            System.out.println(policy);

            //3.2 判断输入的政策是否为空
            if (policy != null && !policy.equals("")) {
                //把Map形的String转换为Map
                Map policyInfo = JSONObject.parseObject(policy, Map.class);

                //3.3 判断开始时间小于等于当前时间，并且当前时间小于等于结束时间
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

                //3.3.1用RestTemplate操作URL，获取值
                String now = restTemplate.getForObject("http://leyou-time-server/getTime", String.class);
                try {
                    Date end_time = simpleDateFormat.parse(policyInfo.get("end_time").toString());
                    Date begin_time = simpleDateFormat.parse(policyInfo.get("begin_time").toString());
                    Date now_time = simpleDateFormat.parse(now);
                    if (begin_time.getTime() <= now_time.getTime() && now_time.getTime() <= end_time.getTime()) {
                        skuMap.put("limitPrice", policyInfo.get("price"));
                        skuMap.put("limitQuanty", policyInfo.get("quanty"));
                        skuMap.put("limitBeginTime", policyInfo.get("begin_time"));
                        skuMap.put("limitEndTime", policyInfo.get("end_time"));
                        skuMap.put("nowTime", now);
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        }
        resultMap.put("result", true);
        resultMap.put("msg", "");
        return resultMap;
    }

}
