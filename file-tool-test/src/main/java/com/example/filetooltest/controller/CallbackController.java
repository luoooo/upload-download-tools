package com.example.filetooltest.controller;

import com.example.filetooltest.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * 回调接口控制器
 */
@Slf4j
@RestController
@RequestMapping("/callback")
public class CallbackController {

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 处理文件处理回调
     *
     * @param taskId 任务ID
     * @param status 任务状态
     * @param processedRows 处理行数
     * @param successRows 成功行数
     * @param failedRows 失败行数
     * @param callbackParams 回调参数（JSON字符串）
     * @return 处理结果
     */
    @PostMapping
    public Map<String, Object> handleCallback(
            @RequestParam("taskId") Long taskId,
            @RequestParam("status") String status,
            @RequestParam(value = "processedRows", required = false) Integer processedRows,
            @RequestParam(value = "successRows", required = false) Integer successRows,
            @RequestParam(value = "failedRows", required = false) Integer failedRows,
            @RequestParam(value = "callbackParams", required = false) String callbackParams) {
        
        log.info("收到回调请求 - taskId: {}, status: {}, processedRows: {}, successRows: {}, failedRows: {}, callbackParams: {}",
                taskId, status, processedRows, successRows, failedRows, callbackParams);

        try {
            // 解析回调参数中的User对象
            User user = null;
            if (callbackParams != null && !callbackParams.isEmpty()) {
                user = objectMapper.readValue(callbackParams, User.class);
                log.info("解析到用户信息: {}", user);
            }

            // 构建响应数据
            Map<String, Object> response = new HashMap<>();
            response.put("taskId", taskId);
            response.put("status", status);
            response.put("processedRows", processedRows);
            response.put("successRows", successRows);
            response.put("failedRows", failedRows);
            
            // 添加用户信息到响应中
            if (user != null) {
                response.put("username", user.getUsername());
                response.put("age", user.getAge());
            }

            // 模拟一些额外的处理结果
            Random random = new Random();
            response.put("fileName", "processed_file_" + taskId + ".xlsx");
            response.put("fileSize", random.nextInt(1000000));
            response.put("processTime", random.nextInt(1000));
            
            if ("FAILED".equals(status)) {
                response.put("errorMessage", "处理失败，请检查数据格式");
            }

            log.info("回调处理完成，返回响应: {}", response);
            return response;
        } catch (Exception e) {
            log.error("处理回调请求失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "处理回调请求失败: " + e.getMessage());
            return errorResponse;
        }
    }
} 