package com.example.filetool.parser.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.example.filetool.parser.FileParser;
import com.example.filetool.util.ExcelProcessUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Excel文件解析器实现
 * 基于EasyExcel实现Excel文件的解析和生成
 */
@Slf4j
@Component
public class ExcelFileParser implements FileParser {

    @Autowired
    private ExcelProcessUtil excelProcessUtil;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 默认批处理大小
     */
    private static final int DEFAULT_BATCH_SIZE = 1000;

    @Override
    public int parseFile(InputStream inputStream, String fieldMapping, Consumer<List<Map<String, Object>>> batchConsumer) {
        try {
            // 解析字段映射
            Map<Integer, String> columnMapping = parseFieldMapping(fieldMapping);
            final AtomicInteger totalRows = new AtomicInteger(0);

            // 使用EasyExcel读取Excel文件
            excelProcessUtil.readExcel(inputStream, excelRows -> {
                List<Map<String, Object>> dataRows = new ArrayList<>();
                
                // 转换数据格式
                for (Map<Integer, String> excelRow : excelRows) {
                    Map<String, Object> dataRow = new HashMap<>();
                    for (Map.Entry<Integer, String> entry : columnMapping.entrySet()) {
                        Integer columnIndex = entry.getKey();
                        String fieldName = entry.getValue();
                        String value = excelRow.get(columnIndex);
                        dataRow.put(fieldName, value);
                    }
                    dataRows.add(dataRow);
                }
                
                // 处理转换后的数据
                batchConsumer.accept(dataRows);
            }, DEFAULT_BATCH_SIZE);
            
            return totalRows.get();
        } catch (Exception e) {
            log.error("解析Excel文件失败", e);
            throw new RuntimeException("解析Excel文件失败: " + e.getMessage(), e);
        }
    }

    @Override
    public InputStream generateFile(DataProvider dataProvider, String fieldMapping) {
        try {
            // 解析字段映射
            final Map<String, Integer> fieldToColumnMap = parseFieldMappingReverse(fieldMapping);
            final Map<Integer, String> headerMap = getHeaderMapping(fieldMapping);
            
            // 创建输出流
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            
            // 使用ExcelProcessUtil写入Excel
            excelProcessUtil.writeExcel(outputStream, headerMap, batchSize -> {
                // 获取一批数据
                List<Map<String, Object>> dataRows = dataProvider.provide(batchSize);
                List<Map<Integer, Object>> excelRows = new ArrayList<>();
                
                // 转换数据格式
                for (Map<String, Object> dataRow : dataRows) {
                    Map<Integer, Object> excelRow = new HashMap<>();
                    for (Map.Entry<String, Object> entry : dataRow.entrySet()) {
                        String fieldName = entry.getKey();
                        Object value = entry.getValue();
                        Integer columnIndex = fieldToColumnMap.get(fieldName);
                        if (columnIndex != null) {
                            excelRow.put(columnIndex, value);
                        }
                    }
                    excelRows.add(excelRow);
                }
                
                return excelRows;
            });
            
            // 返回输入流
            return new ByteArrayInputStream(outputStream.toByteArray());
        } catch (Exception e) {
            log.error("生成Excel文件失败", e);
            throw new RuntimeException("生成Excel文件失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解析字段映射
     * 将JSON格式的字段映射转换为列索引到字段名的映射
     * 格式：{"0":{"field":"username","label":"用户名"},"1":{"field":"age","label":"年龄"}}
     *
     * @param fieldMapping 字段映射（JSON格式）
     * @return 列索引到字段名的映射
     */
    private Map<Integer, String> parseFieldMapping(String fieldMapping) {
        if (fieldMapping == null || fieldMapping.isEmpty()) {
            // 默认映射
            Map<Integer, String> defaultMapping = new HashMap<>();
            defaultMapping.put(0, "column0");
            defaultMapping.put(1, "column1");
            defaultMapping.put(2, "column2");
            return defaultMapping;
        }
        
        try {
            Map<Integer, Map<String, String>> mapping = objectMapper.readValue(fieldMapping, 
                new TypeReference<Map<Integer, Map<String, String>>>() {});
            
            Map<Integer, String> result = new HashMap<>();
            for (Map.Entry<Integer, Map<String, String>> entry : mapping.entrySet()) {
                result.put(entry.getKey(), entry.getValue().get("field"));
            }
            return result;
        } catch (JsonProcessingException e) {
            log.error("解析字段映射失败", e);
            throw new RuntimeException("解析字段映射失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解析字段映射（反向）
     * 将JSON格式的字段映射转换为字段名到列索引的映射
     * 格式：{"0":{"field":"username","label":"用户名"},"1":{"field":"age","label":"年龄"}}
     *
     * @param fieldMapping 字段映射（JSON格式）
     * @return 字段名到列索引的映射
     */
    private Map<String, Integer> parseFieldMappingReverse(String fieldMapping) {
        if (fieldMapping == null || fieldMapping.isEmpty()) {
            return new HashMap<>();
        }
        
        try {
            Map<Integer, Map<String, String>> mapping = objectMapper.readValue(fieldMapping, 
                new TypeReference<Map<Integer, Map<String, String>>>() {});
            
            Map<String, Integer> result = new HashMap<>();
            for (Map.Entry<Integer, Map<String, String>> entry : mapping.entrySet()) {
                result.put(entry.getValue().get("field"), entry.getKey());
            }
            return result;
        } catch (JsonProcessingException e) {
            log.error("解析字段映射失败", e);
            throw new RuntimeException("解析字段映射失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取表头映射
     * 将JSON格式的字段映射转换为列索引到中文标签的映射
     * 格式：{"0":{"field":"username","label":"用户名"},"1":{"field":"age","label":"年龄"}}
     *
     * @param fieldMapping 字段映射（JSON格式）
     * @return 列索引到中文标签的映射
     */
    private Map<Integer, String> getHeaderMapping(String fieldMapping) {
        if (fieldMapping == null || fieldMapping.isEmpty()) {
            return new HashMap<>();
        }
        
        try {
            Map<Integer, Map<String, String>> mapping = objectMapper.readValue(fieldMapping, 
                new TypeReference<Map<Integer, Map<String, String>>>() {});
            
            Map<Integer, String> result = new HashMap<>();
            for (Map.Entry<Integer, Map<String, String>> entry : mapping.entrySet()) {
                result.put(entry.getKey(), entry.getValue().get("label"));
            }
            return result;
        } catch (JsonProcessingException e) {
            log.error("解析字段映射失败", e);
            throw new RuntimeException("解析字段映射失败: " + e.getMessage(), e);
        }
    }
}