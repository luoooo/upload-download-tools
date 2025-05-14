package com.example.filetooltest.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 订单数据查询控制器
 */
@Slf4j
@RestController
@RequestMapping("/queryOrder")
public class OrderQueryController {

    /**
     * 查询订单数据
     *
     * @param taskId 任务ID
     * @param offset 偏移量
     * @param limit 每页数量
     * @param callbackParams 回调参数（JSON格式）
     * @return 订单数据列表
     */
    @PostMapping
    public Map<String, Object> queryOrderData(
            @RequestParam("taskId") Long taskId,
            @RequestParam("offset") Integer offset,
            @RequestParam("limit") Integer limit,
            @RequestParam(value = "callbackParams", required = false) String callbackParams) {
        
        log.info("收到订单数据查询请求 - taskId: {}, offset: {}, limit: {}, callbackParams: {}",
                taskId, offset, limit, callbackParams);

        try {
            // 模拟从数据库查询订单数据
            List<Map<String, Object>> orderList = generateMockOrderData(offset, limit);
            
            // 构建响应数据
            Map<String, Object> response = new HashMap<>();
            response.put("taskId", taskId);
            response.put("data", orderList);
            response.put("total", 200); // 模拟总数据量
            response.put("offset", offset);
            response.put("limit", limit);
            response.put("hasMore", (offset + limit) < 200); // 模拟是否还有更多数据
            
            log.info("订单数据查询完成，返回数据条数: {}", orderList.size());
            return response;
        } catch (Exception e) {
            log.error("查询订单数据失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "查询订单数据失败: " + e.getMessage());
            return errorResponse;
        }
    }

    /**
     * 生成模拟订单数据
     */
    private List<Map<String, Object>> generateMockOrderData(int offset, int limit) {
        List<Map<String, Object>> orderList = new ArrayList<>();
        for (int i = 0; i < limit; i++) {
            Map<String, Object> order = new HashMap<>();
            order.put("id", offset + i + 1);
            order.put("orderNo", "ORD" + String.format("%08d", offset + i + 1));
            order.put("userId", 1000 + (offset + i) % 100);
            order.put("amount", 100.0 + (offset + i) * 10.0);
            order.put("status", getRandomOrderStatus());
            order.put("createTime", System.currentTimeMillis() - (offset + i) * 86400000L);
            order.put("payTime", System.currentTimeMillis() - (offset + i) * 43200000L);
            order.put("deliveryAddress", "城市" + (offset + i) % 10 + "区" + (offset + i) % 100 + "号");
            orderList.add(order);
        }
        return orderList;
    }

    /**
     * 获取随机订单状态
     */
    private String getRandomOrderStatus() {
        String[] statuses = {"PENDING", "PAID", "SHIPPED", "DELIVERED", "COMPLETED", "CANCELLED"};
        return statuses[(int) (Math.random() * statuses.length)];
    }
} 