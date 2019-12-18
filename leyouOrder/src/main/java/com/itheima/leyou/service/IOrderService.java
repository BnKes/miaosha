package com.itheima.leyou.service;

import java.util.Map;

public interface IOrderService {
    Map<String, Object> insertOrder(Map<String,Object> orderInfo);

    Map<String, Object> createOrder(String sku_id, String user_id);

    Map<String, Object> getOrder(String order_id);

    Map<String, Object> payOrder(String order_id, String sku_id);
}
