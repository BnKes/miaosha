package com.itheima.leyou.dao;

import java.util.ArrayList;
import java.util.Map;

public interface IOrderDao {

    boolean insertOrder(Map<String, Object> orderInfo);

    ArrayList<Map<String ,Object>> getOrder(String order_id);

    boolean updateOrderStatus(String order_id);

}
