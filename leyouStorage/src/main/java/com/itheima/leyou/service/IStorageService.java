package com.itheima.leyou.service;

import java.util.Map;

public interface IStorageService {

    Map<String, Object> insertStorage(String sku_id, double inquanty, double outquanty);
}


