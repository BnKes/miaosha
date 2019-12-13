package com.itheima.leyou.dao;

import java.util.ArrayList;
import java.util.Map;

public interface IStockDao {

    ArrayList<Map<String, Object>> getStockList();
}
