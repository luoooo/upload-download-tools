package com.example.filetool.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.HashMap;
import java.util.Map;

/**
 * HTTP客户端工具类
 * 用于向业务系统发送回调请求
 */
@Slf4j
@Component
public class HttpClientUtil {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 发送POST请求
     *
     * @param url  请求URL
     * @param data 请求数据
     * @return 响应结果
     */
    public String post(String url, Object data) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Object> entity = new HttpEntity<>(data, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            
            log.info("发送HTTP请求成功: {}, 状态码: {}", url, response.getStatusCodeValue());
            return response.getBody();
        } catch (Exception e) {
            log.error("发送HTTP请求失败: " + url, e);
            throw new RuntimeException("发送HTTP请求失败: " + e.getMessage(), e);
        }
    }

    /**
     * 发送回调请求
     *
     * @param callbackUrl 回调URL
     * @param taskId      任务ID
     * @param status      任务状态
     * @param data        回调数据
     * @return 响应结果
     */
    public String sendCallback(String callbackUrl, Long taskId, String status, Object data) {
        if (callbackUrl == null || callbackUrl.isEmpty()) {
            log.warn("回调URL为空，跳过回调");
            return null;
        }
        
        try {
            Map<String, Object> callbackData = new HashMap<>();
            callbackData.put("taskId", taskId);
            callbackData.put("status", status);
            callbackData.put("data", data);
            
            return post(callbackUrl, callbackData);
        } catch (Exception e) {
            log.error("发送回调请求失败: " + callbackUrl, e);
            return null;
        }
    }

    /**
     * 发送POST表单请求
     *
     * @param url  请求URL
     * @param data 请求数据
     * @return 响应结果
     */
    public String postForm(String url, Object data) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            
            // 将数据转换为表单参数
            Map<String, Object> formData = objectMapper.convertValue(data, new TypeReference<Map<String, Object>>() {});
            MultiValueMap<String, String> formParams = new LinkedMultiValueMap<>();
            formData.forEach((key, value) -> formParams.add(key, String.valueOf(value)));
            
            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(formParams, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            
            log.info("发送HTTP表单请求成功: {}, 状态码: {}", url, response.getStatusCodeValue());
            return response.getBody();
        } catch (Exception e) {
            log.error("发送HTTP表单请求失败: " + url, e);
            throw new RuntimeException("发送HTTP表单请求失败: " + e.getMessage(), e);
        }
    }
}