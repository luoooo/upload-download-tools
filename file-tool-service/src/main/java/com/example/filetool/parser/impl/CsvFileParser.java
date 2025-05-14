package com.example.filetool.parser.impl;

import com.example.filetool.parser.FileParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * CSV文件解析器实现
 * 实现CSV文件的解析和生成
 */
@Slf4j
@Component
public class CsvFileParser implements FileParser {

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 默认批处理大小
     */
    private static final int DEFAULT_BATCH_SIZE = 1000;

    /**
     * CSV分隔符
     */
    private static final char SEPARATOR = ',';

    /**
     * 引号字符
     */
    private static final char QUOTE = '"';

    @Override
    public int parseFile(InputStream inputStream, String fieldMapping, Consumer<List<Map<String, Object>>> batchConsumer) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            // 解析字段映射
            Map<Integer, String> columnMapping = parseFieldMapping(fieldMapping);
            AtomicInteger totalRows = new AtomicInteger(0);
            
            // 读取CSV文件
            String line;
            List<Map<String, Object>> batch = new ArrayList<>(DEFAULT_BATCH_SIZE);
            
            // 跳过表头行
            reader.readLine();
            
            while ((line = reader.readLine()) != null) {
                String[] values = parseCsvLine(line);
                Map<String, Object> dataRow = new HashMap<>();
                
                // 转换数据格式
                for (Map.Entry<Integer, String> entry : columnMapping.entrySet()) {
                    Integer columnIndex = entry.getKey();
                    String fieldName = entry.getValue();
                    
                    if (columnIndex < values.length) {
                        dataRow.put(fieldName, values[columnIndex]);
                    } else {
                        dataRow.put(fieldName, "");
                    }
                }
                
                batch.add(dataRow);
                totalRows.incrementAndGet();
                
                // 达到批处理大小，进行处理
                if (batch.size() >= DEFAULT_BATCH_SIZE) {
                    batchConsumer.accept(new ArrayList<>(batch));
                    batch.clear();
                }
            }
            
            // 处理剩余数据
            if (!batch.isEmpty()) {
                batchConsumer.accept(batch);
            }
            
            log.info("CSV文件解析完成，总行数：{}", totalRows.get());
            return totalRows.get();
        } catch (Exception e) {
            log.error("解析CSV文件失败", e);
            throw new RuntimeException("解析CSV文件失败: " + e.getMessage(), e);
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
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
            
            // 写入表头
            List<String> headers = new ArrayList<>();
            for (int i = 0; i < headerMap.size(); i++) {
                headers.add(headerMap.getOrDefault(i, "列" + i));
            }
            writer.write(toCsvLine(headers.toArray(new String[0])));
            writer.newLine();
            
            // 写入数据
            int totalRows = 0;
            List<Map<String, Object>> dataRows;
            while (!(dataRows = dataProvider.provide(DEFAULT_BATCH_SIZE)).isEmpty()) {
                for (Map<String, Object> dataRow : dataRows) {
                    String[] values = new String[headerMap.size()];
                    Arrays.fill(values, "");
                    
                    // 转换数据格式
                    for (Map.Entry<String, Object> entry : dataRow.entrySet()) {
                        String fieldName = entry.getKey();
                        Object value = entry.getValue();
                        Integer columnIndex = fieldToColumnMap.get(fieldName);
                        
                        if (columnIndex != null && columnIndex < values.length) {
                            values[columnIndex] = value != null ? value.toString() : "";
                        }
                    }
                    
                    writer.write(toCsvLine(values));
                    writer.newLine();
                    totalRows++;
                }
            }
            
            writer.flush();
            log.info("CSV文件生成完成，总行数：{}", totalRows);
            
            // 返回输入流
            return new ByteArrayInputStream(outputStream.toByteArray());
        } catch (Exception e) {
            log.error("生成CSV文件失败", e);
            throw new RuntimeException("生成CSV文件失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解析CSV行
     *
     * @param line CSV行
     * @return 字段值数组
     */
    private String[] parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            
            if (c == QUOTE) {
                // 引号内的引号需要转义
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == QUOTE) {
                    sb.append(QUOTE);
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == SEPARATOR && !inQuotes) {
                result.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        
        result.add(sb.toString());
        return result.toArray(new String[0]);
    }

    /**
     * 转换为CSV行
     *
     * @param values 字段值数组
     * @return CSV行
     */
    private String toCsvLine(String[] values) {
        StringBuilder sb = new StringBuilder();
        
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                sb.append(SEPARATOR);
            }
            
            String value = values[i];
            if (value == null) {
                value = "";
            }
            
            // 如果值包含分隔符、引号或换行符，需要用引号包围
            if (value.contains(String.valueOf(SEPARATOR)) || value.contains(String.valueOf(QUOTE)) || 
                    value.contains("\n") || value.contains("\r")) {
                sb.append(QUOTE);
                // 将值中的引号替换为两个引号
                sb.append(value.replace(String.valueOf(QUOTE), String.valueOf(QUOTE) + QUOTE));
                sb.append(QUOTE);
            } else {
                sb.append(value);
            }
        }
        
        return sb.toString();
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