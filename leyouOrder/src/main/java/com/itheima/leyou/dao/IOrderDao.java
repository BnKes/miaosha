package com.itheima.leyou.dao;

import java.util.Map;

public interface IOrderDao {

    boolean insertOrder(Map<String, Object> orderInfo);
}
