package com.example.filetooltest.controller;

import com.example.filetooltest.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户数据查询控制器
 */
@Slf4j
@RestController
@RequestMapping("/queryUser")
public class UserQueryController {

    /**
     * 查询用户数据
     *
     * @param taskId 任务ID
     * @param offset 偏移量
     * @param limit 每页数量
     * @param callbackParams 回调参数（JSON格式）
     * @return 用户数据列表
     */
    @PostMapping
    public Map<String, Object> queryUserData(
            @RequestParam("taskId") Long taskId,
            @RequestParam("offset") Integer offset,
            @RequestParam("limit") Integer limit,
            @RequestParam(value = "callbackParams", required = false) String callbackParams) {
        
        log.info("收到用户数据查询请求 - taskId: {}, offset: {}, limit: {}, callbackParams: {}",
                taskId, offset, limit, callbackParams);

        try {
            // 模拟从数据库查询用户数据
            List<Map<String, Object>> userList = generateMockUserData(offset, limit);
            
            // 构建响应数据
            Map<String, Object> response = new HashMap<>();
            response.put("taskId", taskId);
            response.put("data", userList);
            response.put("total", 100); // 模拟总数据量
            response.put("offset", offset);
            response.put("limit", limit);
            response.put("hasMore", (offset + limit) < 100); // 模拟是否还有更多数据
            
            log.info("用户数据查询完成，返回数据条数: {}", userList.size());
            return response;
        } catch (Exception e) {
            log.error("查询用户数据失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "查询用户数据失败: " + e.getMessage());
            return errorResponse;
        }
    }

    /**
     * 生成模拟用户数据
     */
    private List<Map<String, Object>> generateMockUserData(int offset, int limit) {
        List<Map<String, Object>> userList = new ArrayList<>();
        for (int i = 0; i < limit; i++) {
            Map<String, Object> user = new HashMap<>();
            user.put("id", offset + i + 1);
            user.put("username", "user_" + (offset + i + 1));
            user.put("age", 20 + (offset + i) % 40);
            user.put("email", "user_" + (offset + i + 1) + "@example.com");
            user.put("phone", "138" + String.format("%08d", offset + i + 1));
            user.put("address", "城市" + (offset + i) % 10 + "区" + (offset + i) % 100 + "号");
            user.put("createTime", System.currentTimeMillis() - (offset + i) * 86400000L);
            userList.add(user);
        }
        return userList;
    }

    /**
     * 获取用户数据字段映射
     * 返回字段映射配置，包含字段名和中文标签
     *
     * @return 字段映射配置
     */
    @GetMapping("/fieldMapping")
    public Map<String, Object> getFieldMapping() {
        try {
            // 构建字段映射
            Map<Integer, Map<String, String>> fieldMapping = new HashMap<>();
            
            // 用户ID
            Map<String, String> idMapping = new HashMap<>();
            idMapping.put("field", "id");
            idMapping.put("label", "用户ID");
            fieldMapping.put(0, idMapping);
            
            // 用户名
            Map<String, String> usernameMapping = new HashMap<>();
            usernameMapping.put("field", "username");
            usernameMapping.put("label", "用户名");
            fieldMapping.put(1, usernameMapping);
            
            // 年龄
            Map<String, String> ageMapping = new HashMap<>();
            ageMapping.put("field", "age");
            ageMapping.put("label", "年龄");
            fieldMapping.put(2, ageMapping);
            
            // 邮箱
            Map<String, String> emailMapping = new HashMap<>();
            emailMapping.put("field", "email");
            emailMapping.put("label", "邮箱");
            fieldMapping.put(3, emailMapping);
            
            // 电话
            Map<String, String> phoneMapping = new HashMap<>();
            phoneMapping.put("field", "phone");
            phoneMapping.put("label", "电话");
            fieldMapping.put(4, phoneMapping);
            
            // 地址
            Map<String, String> addressMapping = new HashMap<>();
            addressMapping.put("field", "address");
            addressMapping.put("label", "地址");
            fieldMapping.put(5, addressMapping);
            
            // 创建时间
            Map<String, String> createTimeMapping = new HashMap<>();
            createTimeMapping.put("field", "createTime");
            createTimeMapping.put("label", "创建时间");
            fieldMapping.put(6, createTimeMapping);
            
            Map<String, Object> response = new HashMap<>();
            response.put("fieldMapping", fieldMapping);
            
            return response;
        } catch (Exception e) {
            log.error("获取字段映射失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "获取字段映射失败: " + e.getMessage());
            return errorResponse;
        }
    }
} 