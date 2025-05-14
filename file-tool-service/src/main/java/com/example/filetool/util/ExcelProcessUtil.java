package com.example.filetool.util;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.write.metadata.WriteSheet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Excel文件处理工具类
 * 基于EasyExcel实现高性能的Excel文件读写
 */
@Slf4j
@Component
public class ExcelProcessUtil {

    /**
     * 默认每批处理的数据量
     */
    private static final int DEFAULT_BATCH_SIZE = 1000;

    /**
     * 默认每个Sheet的行数
     */
    private static final int DEFAULT_ROWS_PER_SHEET = 100000;

    /**
     * 读取Excel文件（Map方式）
     *
     * @param inputStream Excel文件输入流
     * @param batchConsumer 批量数据处理函数
     * @param batchSize 每批处理的数据量
     * @return 处理的总行数
     */
    public int readExcel(InputStream inputStream, Consumer<List<Map<Integer, String>>> batchConsumer, int batchSize) {
        final AtomicInteger totalRows = new AtomicInteger(0);
        
        EasyExcel.read(inputStream).sheet().registerReadListener(new AnalysisEventListener<Map<Integer, String>>() {
            private final List<Map<Integer, String>> dataList = new ArrayList<>(batchSize);

            @Override
            public void invoke(Map<Integer, String> data, AnalysisContext context) {
                dataList.add(data);
                totalRows.incrementAndGet();
                
                // 达到批处理大小，进行处理
                if (dataList.size() >= batchSize) {
                    batchConsumer.accept(new ArrayList<>(dataList));
                    dataList.clear();
                }
            }

            @Override
            public void doAfterAllAnalysed(AnalysisContext context) {
                // 处理剩余数据
                if (!dataList.isEmpty()) {
                    batchConsumer.accept(new ArrayList<>(dataList));
                    dataList.clear();
                }
                log.info("Excel文件解析完成，总行数：{}", totalRows.get());
            }
        }).doRead();
        
        return totalRows.get();
    }

    /**
     * 读取Excel文件（Map方式），使用默认批处理大小
     *
     * @param inputStream Excel文件输入流
     * @param batchConsumer 批量数据处理函数
     * @return 处理的总行数
     */
    public int readExcel(InputStream inputStream, Consumer<List<Map<Integer, String>>> batchConsumer) {
        return readExcel(inputStream, batchConsumer, DEFAULT_BATCH_SIZE);
    }

    /**
     * 写入Excel文件（Map方式）
     *
     * @param outputStream Excel文件输出流
     * @param headerMap 表头映射（列索引 -> 表头名称）
     * @param dataProvider 数据提供函数，每次调用返回一批数据，返回空集合表示结束
     * @param batchSize 每批获取的数据量
     * @return 写入的总行数
     */
    public int writeExcel(OutputStream outputStream, Map<Integer, String> headerMap, 
                         DataProvider<Map<Integer, Object>> dataProvider, int batchSize) {
        int totalRows = 0;
        int sheetNo = 0;
        
        try (ExcelWriter excelWriter = EasyExcel.write(outputStream).build()) {
            // 准备表头
            List<List<String>> headList = new ArrayList<>();
            for (int i = 0; i < headerMap.size(); i++) {
                List<String> head = new ArrayList<>();
                head.add(headerMap.getOrDefault(i, "列" + i));
                headList.add(head);
            }
            
            WriteSheet writeSheet = null;
            int rowsInCurrentSheet = 0;
            List<Map<Integer, Object>> dataList;
            
            while (!(dataList = dataProvider.provide(batchSize)).isEmpty()) {
                // 如果当前Sheet已满或者是第一次，创建新的Sheet
                if (writeSheet == null || rowsInCurrentSheet >= DEFAULT_ROWS_PER_SHEET) {
                    writeSheet = EasyExcel.writerSheet(sheetNo++, "Sheet" + sheetNo)
                            .head(headList).build();
                    rowsInCurrentSheet = 0;
                }
                
                // 转换数据格式
                List<List<Object>> rows = new ArrayList<>();
                for (Map<Integer, Object> rowData : dataList) {
                    List<Object> row = new ArrayList<>();
                    for (int i = 0; i < headerMap.size(); i++) {
                        row.add(rowData.getOrDefault(i, ""));
                    }
                    rows.add(row);
                }
                
                // 写入数据
                excelWriter.write(rows, writeSheet);
                
                totalRows += dataList.size();
                rowsInCurrentSheet += dataList.size();
            }
            
            log.info("Excel文件写入完成，总行数：{}, Sheet数：{}", totalRows, sheetNo);
        }
        
        return totalRows;
    }

    /**
     * 写入Excel文件（Map方式），使用默认批处理大小
     *
     * @param outputStream Excel文件输出流
     * @param headerMap 表头映射（列索引 -> 表头名称）
     * @param dataProvider 数据提供函数
     * @return 写入的总行数
     */
    public int writeExcel(OutputStream outputStream, Map<Integer, String> headerMap, 
                         DataProvider<Map<Integer, Object>> dataProvider) {
        return writeExcel(outputStream, headerMap, dataProvider, DEFAULT_BATCH_SIZE);
    }

    /**
     * 数据提供者接口
     * @param <T> 数据类型
     */
    public interface DataProvider<T> {
        /**
         * 提供一批数据
         * @param batchSize 批量大小
         * @return 数据列表，返回空集合表示结束
         */
        List<T> provide(int batchSize);
    }
}