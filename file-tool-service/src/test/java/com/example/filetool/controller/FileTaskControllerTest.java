package com.example.filetool.controller;

import com.example.filetool.entity.FileTask;
import com.example.filetool.service.FileTaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.io.InputStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FileTaskController.class)
public class FileTaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FileTaskService fileTaskService;

    private FileTask mockTask;

    @BeforeEach
    void setUp() {
        // 设置模拟任务
        mockTask = new FileTask();
        mockTask.setId(1L);
        mockTask.setTaskName("Test Task");
        mockTask.setStatus(FileTask.TaskStatus.PROCESSING);
        mockTask.setProcessedRows(0);
        mockTask.setSuccessRows(0);
        mockTask.setFailedRows(0);
    }

    @Test
    void testUploadFile() throws Exception {
        // 准备测试数据
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.xlsx",
            MediaType.MULTIPART_FORM_DATA_VALUE,
            "test data".getBytes()
        );

        when(fileTaskService.createUploadTask(any(), any(), any(), any(), any(), any()))
            .thenReturn(mockTask);

        // 执行测试
        mockMvc.perform(multipart("/api/upload")
                .file(file)
                .param("taskName", "Test Upload")
                .param("fieldMapping", "{}")
                .param("callbackUrl", "http://example.com/callback"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskId").value(1))
                .andExpect(jsonPath("$.status").value("PROCESSING"))
                .andExpect(jsonPath("$.message").value("文件上传任务已创建"));
    }

    @Test
    void testCreateExportTask() throws Exception {
        when(fileTaskService.createDownloadTask(any(), any(), any(), any()))
            .thenReturn(mockTask);

        // 执行测试
        mockMvc.perform(post("/api/export")
                .param("taskName", "Test Export")
                .param("fieldMapping", "{}")
                .param("callbackUrl", "http://example.com/callback"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskId").value(1))
                .andExpect(jsonPath("$.status").value("PROCESSING"))
                .andExpect(jsonPath("$.message").value("文件导出任务已创建"));
    }

    @Test
    void testGetTaskStatus() throws Exception {
        when(fileTaskService.getTaskById(anyLong()))
            .thenReturn(mockTask);

        // 执行测试
        mockMvc.perform(get("/api/task/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskId").value(1))
                .andExpect(jsonPath("$.taskName").value("Test Task"))
                .andExpect(jsonPath("$.status").value("PROCESSING"))
                .andExpect(jsonPath("$.processedRows").value(0))
                .andExpect(jsonPath("$.successRows").value(0))
                .andExpect(jsonPath("$.failedRows").value(0));
    }

    @Test
    void testDownloadFile() throws Exception {
        // 设置任务状态为已完成
        mockTask.setStatus(FileTask.TaskStatus.COMPLETED);
        mockTask.setOriginalFilename("test.xlsx");

        when(fileTaskService.getTaskById(anyLong()))
            .thenReturn(mockTask);
        when(fileTaskService.getFileInputStream(anyLong()))
            .thenReturn(new ClassPathResource("test.xlsx").getInputStream());

        // 执行测试
        mockMvc.perform(get("/api/download/1"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"test.xlsx\""))
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM));
    }

    @Test
    void testDownloadFileNotReady() throws Exception {
        when(fileTaskService.getTaskById(anyLong()))
            .thenReturn(mockTask);

        // 执行测试
        mockMvc.perform(get("/api/download/1"))
                .andExpect(status().isInternalServerError());
    }
} 