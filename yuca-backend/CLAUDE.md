# CLAUDE.md — yuca-backend

本文件为 Claude Code (claude.ai/code) 在本仓库中工作时提供指导。

## 多模块架构

本项目为 **Maven 多模块项目**（父 `pom.xml` 的 packaging 为 `pom`）。`yuca-app` 模块是 Spring Boot 启动入口，聚合所有其他模块。

### 模块依赖链

```
yuca-common（无依赖）
  └── yuca-infrastructure（依赖 common）
        ├── yuca-ai（依赖 infrastructure）
        │     └── yuca-assistant（依赖 ai + infrastructure）
        ├── yuca-user（依赖 infrastructure）
        ├── yuca-knowledge（依赖 infrastructure）
        ├── yuca-note（依赖 infrastructure）
        └── yuca-diet（依赖 infrastructure）
              └── yuca-app（聚合模块，依赖上述所有模块）
```

### 各模块职责

| 模块 | 职责 | 核心类 |
|------|------|--------|
| `yuca-common` | 公共注解、常量、异常、统一响应 `Result<T>` | `@RequireLogin`、`@SkipAuth`、`BusinessException`、`ErrorCode` |
| `yuca-infrastructure` | 数据库、Redis、JWT、MinIO、Swagger、全局异常处理 | `JwtAuthenticationFilter`、`JwtTokenProvider`、`GlobalExceptionHandler`、`MinioFileStorageService` |
| `yuca-ai` | AI 客户端、Agent 框架、Skill/Tool 系统 | `AiClient`、`AgentFactory`、`AgentBuilder`、`SkillRegistry`、`PostgresChatHistoryStore` |
| `yuca-user` | 用户认证与管理 | `UserController`、`UserService`、`TokenService` |
| `yuca-knowledge` | 知识库、文档处理、向量检索 | `KnowledgeBaseController`、`KnowledgeDocService`（pgvector 1024 维嵌入） |
| `yuca-note` | 笔记本、文件夹、文档、标签 | `NoteBookController`、`NoteItemController`、`NoteTagController` |
| `yuca-assistant` | AI 聊天会话 + SSE 流式传输 | `AssistantController`、`AssistantService`、`SseEvent` |
| `yuca-diet` | 饮食记录、目标、趋势分析 | `DietRecordController`、`DietGoalController`、`DietTrendController` |

## 构建命令

```bash
# 从项目根目录构建所有模块
./mvnw clean install

# 启动应用
./mvnw spring-boot:run -pl yuca-app

# 构建指定模块（及其依赖）
./mvnw clean install -pl yuca-note -am

# 运行测试
./mvnw test

# 运行指定测试
./mvnw test -Dtest=YucaApplicationTests

# 打包可执行 JAR
./mvnw package -pl yuca-app
```

## 包结构约定

每个模块使用 `org.yuca.<模块名>` 作为基础包，包含：
- `controller/` — REST 控制器
- `service/` — 业务逻辑（按需使用接口 + 实现）
- `mapper/` — MyBatis-Plus Mapper 接口
- `entity/` — 数据库实体（`@TableName`、`@TableId(type=IdType.AUTO)`、`@TableLogic`）
- `dto/request/`、`dto/response/`、`dto/internal/` — API 和模块间通信的 DTO
- `enums/` — 模块枚举
- `config/` — Spring `@Configuration` 配置类（仅在需要时添加）

## MyBatis-Plus 配置

- 驼峰映射已启用（`map-underscore-to-camel-case: true`）
- 逻辑删除：`deleted` 字段（1=已删除，0=正常）
- Mapper XML 位置：`classpath*:/mapper/**/*.xml`（目前无 XML 文件，使用 MyBatis-Plus 代码优先方式）
- Entity 扫描包：`org.yuca.yuca.user.entity`、`org.yuca.yuca.assistant.entity`

## 数据库

- **PostgreSQL** + **pgvector** 扩展（向量嵌入）
- DDL 脚本：`yuca-app/src/main/resources/table/`（按模块分子目录，每个表一个文件）
- Flyway 迁移：`yuca-ai/src/main/resources/db/migration/`（V1、V2 为 AI 表）
- 知识库分块使用 1024 维向量 + HNSW 索引进行相似性搜索

## 新增模块步骤

1. 创建 `yuca-<名称>/` 目录及标准 `pom.xml`（parent = `yuca-backend`）
2. 在父 `pom.xml` 中添加 `<module>yuca-<名称></module>`
3. 在父 `<dependencyManagement>` 中添加版本管理的依赖声明
4. 在 `yuca-app/pom.xml` 中添加依赖
5. 遵循 `org.yuca.<名称>/{controller,service,mapper,entity,dto,...}` 包结构
6. 在 `yuca-app/src/main/resources/table/<模块名>/` 中添加 DDL 脚本（每个表一个文件）
7. 在 `application.yml` 的 `springdoc.packages-to-scan` 中添加新 Controller 包路径
