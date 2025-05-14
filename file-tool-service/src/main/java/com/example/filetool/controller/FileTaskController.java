package com.example.filetool.controller;

import com.example.filetool.entity.FileTask;
import com.example.filetool.service.FileTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * 文件任务控制器
 * 提供文件上传下载的REST API接口
 */
@Slf4j
@RestController
@RequestMapping("/api")
public class FileTaskController {

    @Autowired
    private FileTaskService fileTaskService;

    /**
     * 创建文件上传任务
     *
     * @param file         上传的文件
     * @param taskName     任务名称
     * @param fieldMapping 字段映射（JSON格式）
     * @param callbackUrl  回调URL
     * @param callbackParams 回调参数
     * @return 任务信息
     */
    @PostMapping("/upload")
    public Map<String, Object> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("taskName") String taskName,
            @RequestParam(value = "fieldMapping", required = false) String fieldMapping,
            @RequestParam(value = "callbackUrl", required = false) String callbackUrl,
            @RequestParam(value = "callbackParams", required = false) String callbackParams) {

        try {
            // 创建上传任务
            FileTask task = fileTaskService.createUploadTask(
                    taskName,
                    file.getOriginalFilename(),
                    file.getSize(),
                    fieldMapping,
                    callbackUrl,
                    callbackParams
            );

            // 异步处理文件
            fileTaskService.processUploadFile(task.getId(), file.getInputStream());

            // 返回任务信息
            Map<String, Object> result = new HashMap<>();
            result.put("taskId", task.getId());
            result.put("status", task.getStatus());
            result.put("message", "文件上传任务已创建");
            return result;
        } catch (Exception e) {
            log.error("文件上传失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("error", "文件上传失败：" + e.getMessage());
            return result;
        }
    }

    /**
     * 创建文件下载任务
     *
     * @param taskName     任务名称
     * @param fieldMapping 字段映射（JSON格式）
     * @param callbackUrl  回调URL
     * @param callbackParams 回调参数
     * @return 任务信息
     */
    @PostMapping("/export")
    public Map<String, Object> createExportTask(
            @RequestParam("taskName") String taskName,
            @RequestParam(value = "fieldMapping", required = false) String fieldMapping,
            @RequestParam(value = "callbackUrl", required = false) String callbackUrl,
            @RequestParam(value = "callbackParams", required = false) String callbackParams) {

        try {
            // 创建下载任务
            FileTask task = fileTaskService.createDownloadTask(
                    taskName,
                    fieldMapping,
                    callbackUrl,
                    callbackParams
            );

            // 异步处理文件生成
            fileTaskService.processDownloadFile(task.getId());

            // 返回任务信息
            Map<String, Object> result = new HashMap<>();
            result.put("taskId", task.getId());
            result.put("status", task.getStatus());
            result.put("message", "文件导出任务已创建");
            return result;
        } catch (Exception e) {
            log.error("创建导出任务失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("error", "创建导出任务失败：" + e.getMessage());
            return result;
        }
    }

    /**
     * 获取任务状态
     *
     * @param taskId 任务ID
     * @return 任务状态
     */
    @GetMapping("/task/{taskId}")
    public Map<String, Object> getTaskStatus(@PathVariable Long taskId) {
        try {
            FileTask task = fileTaskService.getTaskById(taskId);
            Map<String, Object> result = new HashMap<>();
            result.put("taskId", task.getId());
            result.put("taskName", task.getTaskName());
            result.put("status", task.getStatus());
            result.put("processedRows", task.getProcessedRows());
            result.put("successRows", task.getSuccessRows());
            result.put("failedRows", task.getFailedRows());
            if (task.getErrorMessage() != null) {
                result.put("errorMessage", task.getErrorMessage());
            }
            return result;
        } catch (Exception e) {
            log.error("获取任务状态失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("error", "获取任务状态失败：" + e.getMessage());
            return result;
        }
    }

    /**
     * 下载文件
     *
     * @param taskId 任务ID
     * @return 文件流
     */
    @GetMapping("/download/{taskId}")
    public ResponseEntity<InputStreamResource> downloadFile(@PathVariable Long taskId) {
        try {
            FileTask task = fileTaskService.getTaskById(taskId);
            if (task.getStatus() != FileTask.TaskStatus.COMPLETED) {
                throw new RuntimeException("文件尚未准备好，当前状态：" + task.getStatus());
            }

            InputStream inputStream = fileTaskService.getFileInputStream(taskId);
            InputStreamResource resource = new InputStreamResource(inputStream);

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + task.getOriginalFilename() + "\"");

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        } catch (Exception e) {
            log.error("文件下载失败", e);
            throw new RuntimeException("文件下载失败：" + e.getMessage());
        }
    }
}