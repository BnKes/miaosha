package com.itheima.leyou.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.itheima.leyou.dao.IOrderDao;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class OrderServiceImpl implements IOrderService {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private AmqpTemplate amqpTemplate;

    @Autowired
    private IOrderDao iOrderDao;

    @Override
    public Map<String,Object> createOrder(String sku_id, String user_id){
        Map<String,Object> resultMap = new HashMap<String,Object>();

        //1.判断sku_id
        if(sku_id==null || sku_id.equals("")){
            resultMap.put("result",false);
            resultMap.put("msg","无sku_id参数");
            return resultMap;
        }

        //订单id取当前的时间
        String order_id = String.valueOf((int)(Math.random()*10000+1))+String.valueOf((int)(Math.random()*10000+1))+String.valueOf((int)(Math.random()*10000+1))+String.valueOf((int)(Math.random()*10000+1));
//        String order_id = UUID.randomUUID().toString().replace("-", "").substring(6);

        //2.从Redis中取存入的活动政策limitpolicy
        String key = "LIMIT_POLICY_"+sku_id;
        String policy = stringRedisTemplate.opsForValue().get(key);
//        String policy = stringRedisTemplate.opsForValue().get("LIMIT_POLICY_" + sku_id);

        if(policy!=null && !policy.equals("")){
            Map<String,Object> policyMap = JSONObject.parseObject(policy, Map.class);

            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            String now = restTemplate.getForObject("http://leyou-time-server/getTime",String.class);

            try {
                Date end_time = simpleDateFormat.parse(policyMap.get("end_time").toString());
                Date begin_time = simpleDateFormat.parse(policyMap.get("begin_time").toString());
                Date now_time = simpleDateFormat.parse(now);

                //3. 判断时间是否合法
                if(begin_time.getTime()<=now_time.getTime() && now_time.getTime()<=end_time.getTime()) {
                    int limitQuanty = Integer.parseInt(policyMap.get("quanty").toString());

                    //4. Redis计数器
                    if (stringRedisTemplate.opsForValue().increment("SKU_QUANTY_" + sku_id, 1) <= limitQuanty) {

                        //5、写入队列
                        // tb_order: order_id, total_fee, actual_fee, post_fee, payment_type, user_id, status, create_time
                        // tb_order_detail: order_id, sku_id, num, title, own_spec, price, image, create_time
                        // tb_sku: sku_id, title, images, stock, price, indexes, own_spec
                        String sku = stringRedisTemplate.opsForValue().get("SKU_" + sku_id);
                        Map skuMap = JSONObject.parseObject(sku, Map.class);

                        Map<String, Object> orderInfo = new HashMap<String, Object>();
                        orderInfo.put("order_id", order_id);
                        orderInfo.put("total_fee", skuMap.get("price"));
                        orderInfo.put("actual_fee", policyMap.get("price"));
                        orderInfo.put("post_fee", 0);
                        orderInfo.put("payment_type", 1);
                        orderInfo.put("user_id", user_id);
                        orderInfo.put("status", 1);
                        orderInfo.put("create_time", now);

                        orderInfo.put("sku_id", skuMap.get("sku_id"));
                        orderInfo.put("num", 1);
                        orderInfo.put("title", skuMap.get("title"));
                        orderInfo.put("own_spec", skuMap.get("own_spec"));
                        orderInfo.put("price", policyMap.get("price"));
                        orderInfo.put("image", skuMap.get("images"));

                        String order = JSON.toJSONString(orderInfo);

                        try {
                            amqpTemplate.convertAndSend("order_queue",order);
                        }catch (Exception e){
                            resultMap.put("result", false);
                            resultMap.put("msg", "写入队列异常！");
                            return resultMap;
                        }
                    }else {
                        //超出Redis计数器,直接踢回
                        //如果超出了计数器，返回商品已经售完了
                        resultMap.put("result", false);
                        resultMap.put("msg", "3亿9被踢回去了！");
                        return resultMap;
                    }
                }else {
                    //结束时间大于当前时间，活动已过期
                    resultMap.put("result", false);
                    resultMap.put("msg", "活动已过期或时间还未到！");
                    return resultMap;
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }else {
            //policy为空
            resultMap.put("result", false);
            resultMap.put("msg", "Redis里面无LIMIT_POLICY！");
            return resultMap;
        }

        //正常返回
        resultMap.put("result",true);
        resultMap.put("msg","数据正常返回");
        resultMap.put("order_id",order_id);

        return resultMap;
    }

    @Override
    public Map<String, Object> getOrder(String order_id) {
        Map<String, Object> resultMap = new HashMap<>();
        if(order_id==null || order_id.equals("")){
            resultMap.put("result", false);
            resultMap.put("msg", "参数传入有误！");
            return resultMap;
        }
        ArrayList<Map<String, Object>> list = iOrderDao.getOrder(order_id);
        resultMap.put("order",list);
        return resultMap;
    }


    @Override
    public Map<String, Object> insertOrder(Map<String,Object> orderInfo) {
        Map<String, Object> resultMap = new HashMap<>();

        if(orderInfo==null || orderInfo.isEmpty()){
            resultMap.put("result", false);
            resultMap.put("msg", "传入参数有误！");
            return resultMap;
        }
        boolean result = iOrderDao.insertOrder(orderInfo);
        if(!result){
            resultMap.put("result", false);
            resultMap.put("msg", "订单写入失败！");
            return resultMap;
        }

        resultMap.put("result", true);
        resultMap.put("msg", "");
        return resultMap;
    }

    @Override
    public Map<String, Object> payOrder(String order_id, String sku_id) {
        Map<String, Object> resultMap = new HashMap<String, Object>();
        if (order_id==null||order_id.equals("")){
            resultMap.put("result", false);
            resultMap.put("msg", "订单有误！");
            return resultMap;
        }
        boolean result = iOrderDao.updateOrderStatus(order_id);

        if(!result){
            resultMap.put("result", false);
            resultMap.put("msg", "更新状态失败！");
            return resultMap;
        }

        try {
            amqpTemplate.convertAndSend("storage_queue",sku_id);

        }catch (Exception e){
            resultMap.put("result", false);
            resultMap.put("msg", "写入队列失败！");
            return resultMap;
        }
        resultMap.put("result", true);
        resultMap.put("msg", "支付成功");
        return resultMap;
    }
}
