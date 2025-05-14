package com.example.filetooltest.controller;

import com.example.filetooltest.client.FileToolServiceClient;
import com.example.filetooltest.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import lombok.extern.slf4j.Slf4j;
import java.util.Map;

@RestController
@RequestMapping("/test")
@Slf4j
public class TestController {

    @Autowired
    private FileToolServiceClient serviceClient;

    @Autowired
    private ObjectMapper objectMapper;

    @PostMapping("/upload")
    public Map<String, Object> testUpload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("taskName") String taskName,
            @RequestParam(value = "callbackUrl", required = false) String callbackUrl,
            @RequestParam(value = "callbackParams", required = false) String callbackParams) {
        
        log.info("Received test upload request - fileName: {}, taskName: {}, callbackUrl: {}, callbackParams: {}",
                file.getOriginalFilename(), taskName, callbackUrl, callbackParams);

        // 调用实际服务
        return serviceClient.uploadFile(file, taskName, callbackUrl, callbackParams);
    }

    @GetMapping("/download/{taskId}")
    public Map<String, Object> testDownload(@PathVariable Long taskId) {
        log.info("Received test download request for taskId: {}", taskId);
        return serviceClient.downloadFile(taskId);
    }

    @GetMapping("/status/{taskId}")
    public Map<String, Object> testStatus(@PathVariable Long taskId) {
        log.info("Received test status request for taskId: {}", taskId);
        return serviceClient.getTaskStatus(taskId);
    }

    @PostMapping("/callback-test")
    public Map<String, Object> testCallback(@RequestBody User user) {
        log.info("Testing callback with user: {}", user);
        return serviceClient.getTaskStatus(1L); // 使用模拟的任务ID
    }

    @PostMapping("/export")
    public Map<String, Object> createExportTask(
            @RequestParam("taskName") String taskName,
            @RequestParam(value = "callbackUrl", required = false) String callbackUrl,
            @RequestParam(value = "callbackParams", required = false) String callbackParams) {
        
        log.info("Received create export task request - taskName: {}, callbackUrl: {}, callbackParams: {}",
                taskName, callbackUrl, callbackParams);

        // 调用实际服务创建导出任务
        return serviceClient.createExportTask(taskName, callbackUrl, callbackParams);
    }
} 