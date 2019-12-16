package com.itheima.leyou.queue;

import com.alibaba.fastjson.JSONObject;
import com.itheima.leyou.service.IOrderService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class OrderQueue {

    @Autowired
    private IOrderService iOrderService;

    @RabbitListener(queues = "order_queue")
    public void insertOrder(String msg){  //此处的msg就是订单信息
        //1. 接受消息并输出
        System.out.println("order_queue接收到消息："+msg);

        //2.调用写入订单的方法
        Map orderInfo = JSONObject.parseObject(msg, Map.class);
        Map<String, Object>  resulthMap = new HashMap<>();

        resulthMap = iOrderService.insertOrder(orderInfo);

        //3.如果没写成功输出错误消息
        if(!(Boolean) resulthMap.get("result")){
            System.out.println("order_queue处理消息失败！ ");
        }

        System.out.println("order_queue处理消息成功 ");
    }


}
