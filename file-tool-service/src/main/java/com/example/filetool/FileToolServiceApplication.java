package com.example.filetool;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 文件工具服务应用启动类
 */
@SpringBootApplication
@EnableScheduling // 启用定时任务
public class FileToolServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FileToolServiceApplication.class, args);
    }
}