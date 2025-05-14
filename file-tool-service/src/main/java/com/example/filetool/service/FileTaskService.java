package com.example.filetool.service;

import com.example.filetool.entity.FileTask;

import java.io.InputStream;
import java.util.List;

/**
 * 文件任务服务接口
 */
public interface FileTaskService {

    /**
     * 创建文件上传任务
     *
     * @param taskName         任务名称
     * @param originalFilename 原始文件名
     * @param fileSize         文件大小
     * @param fieldMapping     字段映射（JSON格式）
     * @param callbackUrl      回调URL
     * @param callbackParams   回调参数（JSON格式）
     * @return 文件任务
     */
    FileTask createUploadTask(String taskName, String originalFilename, Long fileSize, 
                             String fieldMapping, String callbackUrl, String callbackParams);

    /**
     * 创建文件下载任务
     *
     * @param taskName       任务名称
     * @param fieldMapping   字段映射（JSON格式）
     * @param callbackUrl    回调URL
     * @param callbackParams 回调参数（JSON格式）
     * @return 文件任务
     */
    FileTask createDownloadTask(String taskName, String fieldMapping, String callbackUrl, String callbackParams);

    /**
     * 根据ID查询任务
     *
     * @param taskId 任务ID
     * @return 文件任务
     */
    FileTask getTaskById(Long taskId);

    /**
     * 查询待处理的任务列表
     *
     * @return 待处理任务列表
     */
    List<FileTask> getPendingTasks();

    /**
     * 更新任务状态
     *
     * @param taskId 任务ID
     * @param status 任务状态
     * @return 更新后的任务
     */
    FileTask updateTaskStatus(Long taskId, FileTask.TaskStatus status);

    /**
     * 更新任务处理结果
     *
     * @param taskId        任务ID
     * @param processedRows 处理行数
     * @param successRows   成功行数
     * @param failedRows    失败行数
     * @param errorMessage  错误信息
     * @return 更新后的任务
     */
    FileTask updateTaskResult(Long taskId, Integer processedRows, Integer successRows, 
                             Integer failedRows, String errorMessage);

    /**
     * 处理文件上传
     *
     * @param taskId     任务ID
     * @param inputStream 文件输入流
     * @return 处理结果
     */
    boolean processUploadFile(Long taskId, InputStream inputStream);

    /**
     * 处理文件下载
     *
     * @param taskId 任务ID
     * @return 处理结果
     */
    boolean processDownloadFile(Long taskId);

    /**
     * 获取文件下载流
     *
     * @param taskId 任务ID
     * @return 文件输入流
     */
    InputStream getFileInputStream(Long taskId);

    /**
     * 清理指定天数前的已完成或失败的任务
     *
     * @param days 天数
     * @return 清理的任务数量
     */
    int cleanupExpiredTasks(int days);
}