# File Tool Service

文件处理服务，提供文件上传、下载、解析等功能。

## 项目结构

```
file-tool-service/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── example/
│   │   │           └── filetool/
│   │   │               ├── config/           # 配置类
│   │   │               ├── controller/       # 控制器
│   │   │               ├── entity/          # 实体类
│   │   │               ├── parser/          # 文件解析器
│   │   │               │   ├── impl/        # 解析器实现
│   │   │               │   └── FileParser.java
│   │   │               ├── service/         # 服务层
│   │   │               │   ├── impl/        # 服务实现
│   │   │               │   └── FileTaskService.java
│   │   │               └── util/            # 工具类
│   │   └── resources/
│   │       ├── application.yml             # 应用配置
│   │       └── db/
│   │           └── migration/              # 数据库迁移脚本
│   └── test/                              # 测试代码
└── pom.xml                                # 项目依赖配置
```

## 主要功能

### 1. 文件上传
- 支持大文件分片上传
- 支持断点续传
- 支持文件秒传（基于MD5校验）
- 支持文件类型校验
- 支持文件大小限制

### 2. 文件下载
- 支持大文件分片下载
- 支持断点续传
- 支持文件类型转换（Excel、CSV）
- 支持自定义字段映射
- 支持中文表头

### 3. 文件解析
- 支持Excel文件解析
- 支持CSV文件解析
- 支持自定义字段映射
- 支持批量数据处理
- 支持数据格式转换

### 4. 任务管理
- 支持异步任务处理
- 支持任务状态查询
- 支持任务进度跟踪
- 支持任务取消
- 支持任务重试

## 技术栈

- Spring Boot 2.7.0
- Spring Web
- Spring Data JPA
- MySQL 8.0
- Flyway（数据库迁移）
- EasyExcel（Excel处理）
- Lombok
- SLF4J + Logback

## 快速开始

### 1. 环境要求
- JDK 1.8+
- Maven 3.6+
- MySQL 8.0+

### 2. 配置数据库
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/file_tool?useSSL=false&serverTimezone=UTC
    username: your_username
    password: your_password
```

### 3. 构建项目
```bash
mvn clean package
```

### 4. 运行项目
```bash
java -jar target/file-tool-service-1.0.0.jar
```

## API文档

### 1. 文件上传
```http
POST /api/v1/upload
Content-Type: multipart/form-data

file: 文件
chunkNumber: 分片序号
totalChunks: 总分片数
identifier: 文件唯一标识
```

### 2. 文件下载
```http
POST /api/v1/download
Content-Type: application/json

{
    "taskId": "任务ID",
    "fileType": "文件类型(EXCEL/CSV)",
    "fieldMapping": "字段映射配置"
}
```

### 3. 任务状态查询
```http
GET /api/v1/tasks/{taskId}
```

## 字段映射配置

字段映射配置使用JSON格式，支持字段名和中文标签的映射：

```json
{
    "0": {
        "field": "username",
        "label": "用户名"
    },
    "1": {
        "field": "age",
        "label": "年龄"
    }
}
```

## 注意事项

1. 文件上传
   - 默认支持的文件类型：xlsx, xls, csv
   - 默认最大文件大小：100MB
   - 分片大小：1MB

2. 文件下载
   - 支持Excel和CSV格式
   - 支持自定义字段映射
   - 支持中文表头

3. 性能优化
   - 使用分片上传/下载处理大文件
   - 使用异步任务处理耗时操作
   - 使用批量处理提高性能

## 开发指南

### 1. 添加新的文件解析器
1. 实现 `FileParser` 接口
2. 在 `FileParserFactory` 中注册新的解析器
3. 在 `FileTaskServiceImpl` 中使用新的解析器

### 2. 添加新的文件类型支持
1. 在 `FileType` 枚举中添加新的文件类型
2. 实现对应的文件解析器
3. 更新文件类型校验逻辑

## 测试

### 1. 单元测试
```bash
mvn test
```

### 2. 集成测试
```bash
mvn verify
```

## 贡献指南

1. Fork 项目
2. 创建特性分支
3. 提交更改
4. 推送到分支
5. 创建 Pull Request

## 许可证

MIT License 