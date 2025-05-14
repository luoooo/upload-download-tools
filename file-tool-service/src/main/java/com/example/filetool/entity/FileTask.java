package com.example.filetool.entity;

import lombok.Data;

import javax.persistence.*;
import java.util.Date;

/**
 * 文件处理任务实体类
 */
@Data
@Entity
@Table(name = "file_task")
public class FileTask {

    /**
     * 任务ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 任务名称
     */
    private String taskName;

    /**
     * 任务类型：UPLOAD-上传任务，DOWNLOAD-下载任务
     */
    @Enumerated(EnumType.STRING)
    private TaskType taskType;

    /**
     * 任务状态：PENDING-等待处理，PROCESSING-处理中，COMPLETED-已完成，FAILED-失败
     */
    @Enumerated(EnumType.STRING)
    private TaskStatus status;

    /**
     * 原始文件名
     */
    private String originalFilename;

    /**
     * 存储文件路径
     */
    private String filePath;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 处理的数据行数
     */
    private Integer processedRows;

    /**
     * 成功处理的行数
     */
    private Integer successRows;

    /**
     * 失败处理的行数
     */
    private Integer failedRows;

    /**
     * 错误信息
     */
    @Column(length = 1000)
    private String errorMessage;

    /**
     * 回调URL
     */
    private String callbackUrl;

    /**
     * 回调参数（JSON格式）
     */
    @Column(columnDefinition = "TEXT")
    private String callbackParams;

    /**
     * 字段映射（JSON格式）
     */
    @Column(columnDefinition = "TEXT")
    private String fieldMapping;

    /**
     * 创建时间
     */
    @Temporal(TemporalType.TIMESTAMP)
    private Date createTime;

    /**
     * 更新时间
     */
    @Temporal(TemporalType.TIMESTAMP)
    private Date updateTime;

    /**
     * 任务类型枚举
     */
    public enum TaskType {
        UPLOAD, DOWNLOAD
    }

    /**
     * 任务状态枚举
     */
    public enum TaskStatus {
        PENDING, PROCESSING, COMPLETED, FAILED
    }

    @PrePersist
    public void prePersist() {
        this.createTime = new Date();
        this.updateTime = new Date();
        if (this.status == null) {
            this.status = TaskStatus.PENDING;
        }
        if (this.processedRows == null) {
            this.processedRows = 0;
        }
        if (this.successRows == null) {
            this.successRows = 0;
        }
        if (this.failedRows == null) {
            this.failedRows = 0;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updateTime = new Date();
    }
}