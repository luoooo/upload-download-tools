package com.example.filetool.scheduler;

import com.example.filetool.entity.FileTask;
import com.example.filetool.service.FileTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 文件任务调度器
 * 定期检查和处理待处理的文件任务
 */
@Slf4j
@Component
public class FileTaskScheduler {

    @Autowired
    private FileTaskService fileTaskService;

    /**
     * 每分钟检查一次待处理的任务
     */
    @Scheduled(fixedRate = 60000)
    public void processPendingTasks() {
        log.info("开始检查待处理任务...");
        List<FileTask> pendingTasks = fileTaskService.getPendingTasks();
        log.info("发现{}个待处理任务", pendingTasks.size());

        for (FileTask task : pendingTasks) {
            try {
                log.info("开始处理任务：{}, 类型：{}", task.getId(), task.getTaskType());
                
                if (FileTask.TaskType.DOWNLOAD.equals(task.getTaskType())) {
                    // 处理下载任务
                    fileTaskService.processDownloadFile(task.getId());
                } else if (FileTask.TaskType.UPLOAD.equals(task.getTaskType())) {
                    // 上传任务通常在控制器中直接处理，这里只处理可能的遗漏任务
                    log.warn("发现未处理的上传任务：{}", task.getId());
                }
                
                log.info("任务处理完成：{}", task.getId());
            } catch (Exception e) {
                log.error("处理任务失败：" + task.getId(), e);
                // 更新任务状态为失败
                fileTaskService.updateTaskResult(
                        task.getId(), 
                        0, 
                        0, 
                        0, 
                        "任务处理失败：" + e.getMessage()
                );
            }
        }
    }
    
    /**
     * 每天凌晨2点执行清理过期文件任务
     * 清理7天前已完成或失败的任务及其相关文件
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupExpiredTasks() {
        log.info("开始清理过期任务...");
        try {
            // 清理7天前的已完成或失败任务
            int days = 7;
            int deletedCount = fileTaskService.cleanupExpiredTasks(days);
            log.info("清理过期任务完成，共删除{}个{}天前的任务", deletedCount, days);
        } catch (Exception e) {
            log.error("清理过期任务失败", e);
        }
    }
}