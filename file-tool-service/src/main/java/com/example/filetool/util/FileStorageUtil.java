package com.example.filetool.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * 文件存储工具类
 * 负责文件的存储和读取操作
 */
@Slf4j
@Component
public class FileStorageUtil {

    @Value("${file.storage.path:./sources}")
    private String storagePath;

    /**
     * 初始化存储目录
     */
    public void init() {
        try {
            Path path = Paths.get(storagePath);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                log.info("创建文件存储目录：{}", path);
            }
        } catch (IOException e) {
            log.error("初始化存储目录失败", e);
            throw new RuntimeException("无法创建文件存储目录", e);
        }
    }

    /**
     * 保存文件到存储系统
     *
     * @param originalFilename 原始文件名
     * @param inputStream      文件输入流
     * @return 存储路径
     * @throws IOException IO异常
     */
    public String saveFile(String originalFilename, InputStream inputStream) throws IOException {
        // 确保存储目录存在
        init();

        // 生成唯一文件名
        String fileExtension = getFileExtension(originalFilename);
        String uniqueFilename = UUID.randomUUID().toString() + fileExtension;
        String relativePath = getYearMonthPath() + uniqueFilename;
        Path targetPath = Paths.get(storagePath, relativePath);

        // 确保父目录存在
        Files.createDirectories(targetPath.getParent());

        // 保存文件
        try (FileOutputStream outputStream = new FileOutputStream(targetPath.toFile())) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }

        log.info("文件保存成功：{}", targetPath);
        return relativePath;
    }

    /**
     * 获取文件输入流
     *
     * @param filePath 文件路径
     * @return 文件输入流
     * @throws IOException IO异常
     */
    public InputStream getFileInputStream(String filePath) throws IOException {
        Path path = Paths.get(storagePath, filePath);
        File file = path.toFile();
        if (!file.exists()) {
            throw new IOException("文件不存在：" + filePath);
        }
        return new FileInputStream(file);
    }

    /**
     * 删除文件
     *
     * @param filePath 文件路径
     * @return 是否删除成功
     */
    public boolean deleteFile(String filePath) {
        try {
            Path path = Paths.get(storagePath, filePath);
            return Files.deleteIfExists(path);
        } catch (IOException e) {
            log.error("删除文件失败：{}", filePath, e);
            return false;
        }
    }

    /**
     * 获取文件扩展名
     *
     * @param filename 文件名
     * @return 扩展名（包含点号）
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex == -1) {
            return "";
        }
        return filename.substring(dotIndex);
    }

    /**
     * 获取年月路径，用于文件分类存储
     *
     * @return 年月路径，如 2023/05/
     */
    private String getYearMonthPath() {
        java.time.LocalDate now = java.time.LocalDate.now();
        return String.format("%d/%02d/", now.getYear(), now.getMonthValue());
    }
}