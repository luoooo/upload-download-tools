package com.example.filetool.parser;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 文件解析器接口
 * 定义文件解析的通用方法
 */
public interface FileParser {

    /**
     * 解析文件
     *
     * @param inputStream  文件输入流
     * @param fieldMapping 字段映射（JSON格式）
     * @param batchConsumer 批量数据处理函数
     * @return 处理的总行数
     */
    int parseFile(InputStream inputStream, String fieldMapping, Consumer<List<Map<String, Object>>> batchConsumer);

    /**
     * 生成文件
     *
     * @param dataProvider 数据提供者
     * @param fieldMapping 字段映射（JSON格式）
     * @return 生成的文件输入流
     */
    InputStream generateFile(DataProvider dataProvider, String fieldMapping);

    /**
     * 数据提供者接口
     */
    interface DataProvider {
        /**
         * 提供一批数据
         *
         * @param batchSize 批量大小
         * @return 数据列表，返回空集合表示结束
         */
        List<Map<String, Object>> provide(int batchSize);
    }
}