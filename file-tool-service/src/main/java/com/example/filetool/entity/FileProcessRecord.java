package com.example.filetool.entity;

import lombok.Data;

import javax.persistence.*;
import java.util.Date;

/**
 * 文件处理记录实体类
 */
@Data
@Entity
@Table(name = "file_process_record")
public class FileProcessRecord {

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 任务ID
     */
    @Column(name = "task_id")
    private Long taskId;

    /**
     * 行号
     */
    @Column(name = "line_num")
    private Integer lineNum;

    /**
     * 处理状态：SUCCESS-成功，FAILED-失败
     */
    @Enumerated(EnumType.STRING)
    private ProcessStatus status;

    /**
     * 错误信息
     */
    @Column(name = "error_message", length = 500)
    private String errorMessage;

    /**
     * 原始数据
     */
    @Column(name = "raw_data", columnDefinition = "TEXT")
    private String rawData;

    /**
     * 处理后的数据
     */
    @Column(name = "processed_data", columnDefinition = "TEXT")
    private String processedData;

    /**
     * 创建时间
     */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "create_time", nullable = false, updatable = false)
    private Date createTime;

    /**
     * 更新时间
     */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "update_time", nullable = false)
    private Date updateTime;

    /**
     * 处理状态枚举
     */
    public enum ProcessStatus {
        SUCCESS, FAILED
    }

    @PrePersist
    public void prePersist() {
        this.createTime = new Date();
        this.updateTime = new Date();
    }

    @PreUpdate
    public void preUpdate() {
        this.updateTime = new Date();
    }
} 