package com.example.filetool.repository;

import com.example.filetool.entity.FileTask;
import com.example.filetool.entity.FileTask.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

/**
 * 文件任务数据访问层
 */
@Repository
public interface FileTaskRepository extends JpaRepository<FileTask, Long> {

    /**
     * 根据任务状态查询任务列表
     *
     * @param status 任务状态
     * @return 任务列表
     */
    List<FileTask> findByStatus(TaskStatus status);

    /**
     * 根据任务类型和状态查询任务列表
     *
     * @param taskType 任务类型
     * @param status   任务状态
     * @return 任务列表
     */
    List<FileTask> findByTaskTypeAndStatus(FileTask.TaskType taskType, TaskStatus status);
    
    /**
     * 查询指定日期之前的已完成或失败的任务
     *
     * @param date 日期
     * @return 任务列表
     */
    @Query("SELECT t FROM FileTask t WHERE t.updateTime < :date AND (t.status = 'COMPLETED' OR t.status = 'FAILED')")
    List<FileTask> findExpiredTasks(@Param("date") Date date);
}