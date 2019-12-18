package com.itheima.leyou.controller;

import com.alibaba.fastjson.JSONObject;
import com.itheima.leyou.bean.Data;
import com.itheima.leyou.service.IOrderService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class OrderController {

    @Autowired
    private IOrderService iOrderService;

    @ResponseBody
    @RequestMapping("/createOrder/{sku_id}")
    public Map<String,Object> createOrder(@PathVariable("sku_id") String sku_id, HttpServletRequest request){
        Map<String, Object> resultMap = new HashMap<>();
        HttpSession session = request.getSession();
        //从session中取登录用户的信息
        Object userObj = session.getAttribute("user");

        if(userObj==null){
            resultMap.put("result", false);
            resultMap.put("msg", "会员没有登录不能购买！");
            return resultMap;
        }
        Map<String,Object> userMap = JSONObject.parseObject(userObj.toString(),Map.class);

        return iOrderService.createOrder(sku_id, userMap.get("user_id").toString());


    }

    @ResponseBody
    @RequestMapping(value="/getOrder/{order_id}")
    public Object getOrder(@PathVariable("order_id") String order_id){

        Data data = new Data();
        Map<String,Object>  order = iOrderService.getOrder(order_id);
        List ordermsg =(List) order.get("order");

        Map ordermap = (Map) ordermsg.get(0);
        String sku_id = String.valueOf(ordermap.get("sku_id"));
        String  price = String.valueOf(ordermap.get("price"));
        data.setSku_id(sku_id);
        data.setPrice(price);
        data.setOrder_id(order_id);
        return data;

    }

    @ResponseBody
    @RequestMapping(value = "/payOrder/{order_id}/{sku_id}")
    public Object payOrder(@PathVariable("order_id") String order_id, @PathVariable("sku_id") String sku_id){
        //正常情况下在这里会调用支付接口，我们这里模拟支付已经返回正常数据
        Data data = new Data();
        boolean isPay = true;
        Map<String, Object> resultMap = new HashMap<String, Object>();
        if (!isPay){
            resultMap.put("result", false);
            resultMap.put("msg", "支付接口调用失败！");
            return resultMap;
        }
        Map<String,Object> map = iOrderService.payOrder(order_id, sku_id);
        boolean result = (Boolean) map.get("result");
        data.setResult(result);
        return data;
    }

}
















