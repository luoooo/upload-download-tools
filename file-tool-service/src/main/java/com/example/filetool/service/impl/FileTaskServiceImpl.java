package com.example.filetool.service.impl;

import com.example.filetool.entity.FileTask;
import com.example.filetool.parser.FileParser;
import com.example.filetool.parser.impl.CsvFileParser;
import com.example.filetool.parser.impl.ExcelFileParser;
import com.example.filetool.repository.FileTaskRepository;
import com.example.filetool.service.FileTaskService;
import com.example.filetool.util.FileStorageUtil;
import com.example.filetool.util.HttpClientUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 文件任务服务实现类
 */
@Slf4j
@Service
public class FileTaskServiceImpl implements FileTaskService {

    @Autowired
    private FileTaskRepository fileTaskRepository;

    @Autowired
    private FileStorageUtil fileStorageUtil;
    
    @Autowired
    private ExcelFileParser excelFileParser;
    
    @Autowired
    private CsvFileParser csvFileParser;
    
    @Autowired
    private HttpClientUtil httpClientUtil;
    
    @Autowired
    private ObjectMapper objectMapper;

    @Override
    @Transactional
    public FileTask createUploadTask(String taskName, String originalFilename, Long fileSize,
                                    String fieldMapping, String callbackUrl, String callbackParams) {
        FileTask task = new FileTask();
        task.setTaskName(taskName);
        task.setTaskType(FileTask.TaskType.UPLOAD);
        task.setStatus(FileTask.TaskStatus.PENDING);
        task.setOriginalFilename(originalFilename);
        task.setFileSize(fileSize);
        task.setFieldMapping(fieldMapping);
        task.setCallbackUrl(callbackUrl);
        task.setCallbackParams(callbackParams);
        return fileTaskRepository.save(task);
    }

    @Override
    @Transactional
    public FileTask createDownloadTask(String taskName, String fieldMapping, String callbackUrl, String callbackParams) {
        FileTask task = new FileTask();
        task.setTaskName(taskName);
        task.setTaskType(FileTask.TaskType.DOWNLOAD);
        task.setStatus(FileTask.TaskStatus.PENDING);
        task.setFieldMapping(fieldMapping);
        task.setCallbackUrl(callbackUrl);
        task.setCallbackParams(callbackParams);
        return fileTaskRepository.save(task);
    }

    @Override
    public FileTask getTaskById(Long taskId) {
        return fileTaskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("任务不存在：" + taskId));
    }

    @Override
    public List<FileTask> getPendingTasks() {
        return fileTaskRepository.findByStatus(FileTask.TaskStatus.PENDING);
    }

    @Override
    @Transactional
    public FileTask updateTaskStatus(Long taskId, FileTask.TaskStatus status) {
        FileTask task = getTaskById(taskId);
        task.setStatus(status);
        return fileTaskRepository.save(task);
    }

    @Override
    @Transactional
    public FileTask updateTaskResult(Long taskId, Integer processedRows, Integer successRows,
                                    Integer failedRows, String errorMessage) {
        FileTask task = getTaskById(taskId);
        task.setProcessedRows(processedRows);
        task.setSuccessRows(successRows);
        task.setFailedRows(failedRows);
        task.setErrorMessage(errorMessage);
        if (failedRows > 0) {
            task.setStatus(FileTask.TaskStatus.FAILED);
        } else {
            task.setStatus(FileTask.TaskStatus.COMPLETED);
        }
        return fileTaskRepository.save(task);
    }

    @Override
    @Transactional
    public boolean processUploadFile(Long taskId, InputStream inputStream) {
        try {
            // 更新任务状态为处理中
            updateTaskStatus(taskId, FileTask.TaskStatus.PROCESSING);
            FileTask task = getTaskById(taskId);
            
            // 保存文件到存储系统
            String filePath = fileStorageUtil.saveFile(task.getOriginalFilename(), inputStream);
            task.setFilePath(filePath);
            fileTaskRepository.save(task);
            
            // 根据文件类型选择解析器
            FileParser fileParser = getFileParser(task.getOriginalFilename());
            
            // 解析文件并处理数据
            AtomicInteger processedRows = new AtomicInteger(0);
            AtomicInteger successRows = new AtomicInteger(0);
            AtomicInteger failedRows = new AtomicInteger(0);
            
            // 获取文件输入流
            try (InputStream fileInputStream = fileStorageUtil.getFileInputStream(filePath)) {
                // 解析文件
                fileParser.parseFile(fileInputStream, task.getFieldMapping(), dataRows -> {
                    try {
                        // 更新处理行数
                        processedRows.addAndGet(dataRows.size());
                        
                        // 发送数据到业务系统
                        if (task.getCallbackUrl() != null && !task.getCallbackUrl().isEmpty()) {
                            String response = httpClientUtil.sendCallback(
                                task.getCallbackUrl(),
                                task.getId(),
                                "PROCESSING",
                                dataRows
                            );
                            
                            // 根据回调结果更新成功/失败行数
                            if (response != null) {
                                successRows.addAndGet(dataRows.size());
                            } else {
                                failedRows.addAndGet(dataRows.size());
                            }
                        } else {
                            // 没有回调URL，默认为成功
                            successRows.addAndGet(dataRows.size());
                        }
                    } catch (Exception e) {
                        log.error("处理数据批次失败", e);
                        failedRows.addAndGet(dataRows.size());
                    }
                });
                
                // 更新任务处理结果
                updateTaskResult(
                    taskId,
                    processedRows.get(),
                    successRows.get(),
                    failedRows.get(),
                    failedRows.get() > 0 ? "部分数据处理失败" : null
                );
                
                // 发送最终回调
                if (task.getCallbackUrl() != null && !task.getCallbackUrl().isEmpty()) {
                    Map<String, Object> resultData = new HashMap<>();
                    resultData.put("processedRows", processedRows.get());
                    resultData.put("successRows", successRows.get());
                    resultData.put("failedRows", failedRows.get());
                    
                    httpClientUtil.sendCallback(
                        task.getCallbackUrl(),
                        task.getId(),
                        failedRows.get() > 0 ? "FAILED" : "COMPLETED",
                        resultData
                    );
                }
            }
            
            log.info("文件上传任务处理完成：{}，处理行数：{}，成功行数：{}，失败行数：{}", 
                    taskId, processedRows.get(), successRows.get(), failedRows.get());
            return true;
        } catch (Exception e) {
            log.error("处理上传文件失败：" + taskId, e);
            updateTaskResult(taskId, 0, 0, 0, e.getMessage());
            return false;
        }
    }

    @Override
    @Transactional
    public boolean processDownloadFile(Long taskId) {
        try {
            // 更新任务状态为处理中
            updateTaskStatus(taskId, FileTask.TaskStatus.PROCESSING);
            FileTask task = getTaskById(taskId);
            
            // 确定文件类型和文件名
            String fileExtension = ".xlsx"; // 默认为Excel
            if (task.getOriginalFilename() != null && !task.getOriginalFilename().isEmpty()) {
                // 使用原始文件名中的扩展名
                int lastDotIndex = task.getOriginalFilename().lastIndexOf(".");
                if (lastDotIndex > 0) {
                    fileExtension = task.getOriginalFilename().substring(lastDotIndex);
                }
            } else {
                // 设置默认文件名
                task.setOriginalFilename("export_" + System.currentTimeMillis() + fileExtension);
            }
            
            // 根据文件类型选择解析器
            FileParser fileParser = getFileParser(task.getOriginalFilename());
            
            // 从业务系统获取数据并生成文件
            AtomicInteger totalRows = new AtomicInteger(0);
            
            // 创建数据提供者
            FileParser.DataProvider dataProvider = new FileParser.DataProvider() {
                private int offset = 0;
                private static final int BATCH_SIZE = 1000;
                private boolean hasMoreData = true;
                
                @Override
                public List<Map<String, Object>> provide(int batchSize) {
                    if (!hasMoreData) {
                        return new ArrayList<>();
                    }
                    
                    try {
                        // 构建请求参数
                        Map<String, Object> requestParams = new HashMap<>();
                        requestParams.put("taskId", taskId);
                        requestParams.put("offset", offset);
                        requestParams.put("limit", batchSize);
                        if (task.getCallbackParams() != null) {
                            requestParams.put("callbackParams", task.getCallbackParams());
                        }
                        
                        // 打印请求参数
                        log.info("准备发送请求到 {}，参数：{}", task.getCallbackUrl(), objectMapper.writeValueAsString(requestParams));
                        
                        // 发送请求获取数据
                        String response = httpClientUtil.postForm(task.getCallbackUrl(), requestParams);
                        log.info("收到响应：{}", response);
                        
                        Map<String, Object> responseData = objectMapper.readValue(response, Map.class);
                        
                        // 解析响应数据
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> dataList = (List<Map<String, Object>>) responseData.get("data");
                        if (dataList != null && !dataList.isEmpty()) {
                            totalRows.addAndGet(dataList.size());
                            offset += dataList.size();
                            hasMoreData = (Boolean) responseData.get("hasMore");
                            return dataList;
                        }
                    } catch (Exception e) {
                        log.error("获取数据失败", e);
                    }
                    
                    hasMoreData = false;
                    return new ArrayList<>();
                }
            };
            
            // 生成文件
            InputStream fileContent = fileParser.generateFile(dataProvider, task.getFieldMapping());
            
            // 保存文件到存储系统
            String filePath = fileStorageUtil.saveFile(task.getOriginalFilename(), fileContent);
            task.setFilePath(filePath);
            fileTaskRepository.save(task);
            
            // 更新任务处理结果
            updateTaskResult(taskId, totalRows.get(), totalRows.get(), 0, null);
            
            // 发送最终回调
            // if (task.getCallbackUrl() != null && !task.getCallbackUrl().isEmpty()) {
            //     Map<String, Object> resultData = new HashMap<>();
            //     resultData.put("taskId", task.getId());
            //     resultData.put("status", "COMPLETED");
            //     resultData.put("processedRows", totalRows.get());
            //     resultData.put("successRows", totalRows.get());
            //     resultData.put("failedRows", 0);
            //     resultData.put("fileName", task.getOriginalFilename());
            //     resultData.put("fileSize", task.getFileSize());
                
            //     // 添加回调参数
            //     if (task.getCallbackParams() != null) {
            //         resultData.put("callbackParams", task.getCallbackParams());
            //     }
                
            //     httpClientUtil.post(task.getCallbackUrl(), resultData);
            // }
            
            log.info("文件下载任务处理完成：{}", taskId);
            return true;
        } catch (Exception e) {
            log.error("处理下载文件失败：" + taskId, e);
            updateTaskResult(taskId, 0, 0, 0, e.getMessage());
            
            // 发送失败回调
            // try {
            //     FileTask task = getTaskById(taskId);
            //     if (task.getCallbackUrl() != null && !task.getCallbackUrl().isEmpty()) {
            //         Map<String, Object> resultData = new HashMap<>();
            //         resultData.put("taskId", task.getId());
            //         resultData.put("status", "FAILED");
            //         resultData.put("errorMessage", e.getMessage());
                    
            //         // 添加回调参数
            //         if (task.getCallbackParams() != null) {
            //             resultData.put("callbackParams", task.getCallbackParams());
            //         }
                    
            //         httpClientUtil.post(task.getCallbackUrl(), resultData);
            //     }
            // } catch (Exception callbackError) {
            //     log.error("发送失败回调失败：" + taskId, callbackError);
            // }
            
            return false;
        }
    }

    @Override
    public InputStream getFileInputStream(Long taskId) {
        FileTask task = getTaskById(taskId);
        if (task.getFilePath() == null) {
            throw new RuntimeException("文件不存在");
        }
        try {
            return fileStorageUtil.getFileInputStream(task.getFilePath());
        } catch (IOException e) {
            log.error("获取文件流失败：" + taskId, e);
            throw new RuntimeException("获取文件流失败", e);
        }
    }
    
    @Override
    @Transactional
    public int cleanupExpiredTasks(int days) {
        // 计算过期日期（当前日期减去指定天数）
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -days);
        Date expirationDate = calendar.getTime();
        
        // 查询过期任务
        List<FileTask> expiredTasks = fileTaskRepository.findExpiredTasks(expirationDate);
        log.info("找到{}个过期任务（{}天前）", expiredTasks.size(), days);
        
        int deletedCount = 0;
        for (FileTask task : expiredTasks) {
            try {
                // 删除关联的文件
                if (task.getFilePath() != null && !task.getFilePath().isEmpty()) {
                    boolean fileDeleted = fileStorageUtil.deleteFile(task.getFilePath());
                    if (fileDeleted) {
                        log.info("已删除任务{}的文件：{}", task.getId(), task.getFilePath());
                    } else {
                        log.warn("无法删除任务{}的文件：{}", task.getId(), task.getFilePath());
                    }
                }
                
                // 从数据库中删除任务记录
                fileTaskRepository.delete(task);
                log.info("已删除过期任务：{}, 任务名称：{}", task.getId(), task.getTaskName());
                deletedCount++;
            } catch (Exception e) {
                log.error("删除过期任务失败：" + task.getId(), e);
            }
        }
        
        log.info("清理过期任务完成，共删除{}个任务", deletedCount);
        return deletedCount;
    }
    
    /**
     * 根据文件名获取对应的文件解析器
     *
     * @param filename 文件名
     * @return 文件解析器
     */
    private FileParser getFileParser(String filename) {
        if (filename == null || filename.isEmpty()) {
            // 默认使用Excel解析器
            return excelFileParser;
        }
        
        String lowerFilename = filename.toLowerCase();
        if (lowerFilename.endsWith(".csv")) {
            return csvFileParser;
        } else if (lowerFilename.endsWith(".xls") || lowerFilename.endsWith(".xlsx")) {
            return excelFileParser;
        } else if (lowerFilename.endsWith(".txt")) {
            // 对于TXT文件，使用CSV解析器处理
            return csvFileParser;
        } else {
            // 默认使用Excel解析器
            return excelFileParser;
        }
    }

    private void sendCallback(FileTask task) {
        try {
            Map<String, Object> requestParams = new HashMap<>();
            requestParams.put("taskId", task.getId());
            requestParams.put("status", task.getStatus());
            requestParams.put("processedRows", task.getProcessedRows());
            requestParams.put("successRows", task.getSuccessRows());
            requestParams.put("failedRows", task.getFailedRows());
            
            // 添加回调参数
            if (task.getCallbackParams() != null) {
                requestParams.put("callbackParams", task.getCallbackParams());
            }

            String response = httpClientUtil.post(task.getCallbackUrl(), requestParams);
            log.info("Callback response for task {}: {}", task.getId(), response);
        } catch (Exception e) {
            log.error("Error sending callback for task: " + task.getId(), e);
        }
    }
}