# CLAUDE.md

本文件为 Claude Code (claude.ai/code) 在本仓库中工作时提供指导。

## 项目概述

Yuca 是一个带 AI 能力的全栈个人主页应用：
- **后端**: Spring Boot 3.5.9 (Java 21, Maven 多模块) — REST API + SSE 流式传输
- **前端**: React 19 + TypeScript + Vite — Tailwind CSS + shadcn/ui 单页应用

后端采用多模块架构，使用 MyBatis-Plus ORM、PostgreSQL（pgvector 向量检索）、Redis 缓存、MinIO 文件存储，通过 Spring AI + LangChain4j 集成 AI 能力（Qwen/DeepSeek 模型）。

## 后端开发命令

所有命令在 `yuca-backend/` 目录下执行：

```bash
# 构建所有模块
./mvnw clean install

# 启动应用（端口 8500，上下文路径 /api）
./mvnw spring-boot:run -pl yuca-app

# 运行测试
./mvnw test

# 运行指定测试类
./mvnw test -Dtest=YucaApplicationTests

# 构建指定模块及其依赖
./mvnw clean install -pl yuca-note -am

# 打包
./mvnw package -pl yuca-app
```

应用运行后 API 文档地址：`http://localhost:8500/api/swagger-ui.html`

## 后端架构

### 多模块结构

后端是 **Maven 多模块项目**，依赖层级如下：

```
yuca-backend/
├── yuca-common/          # 公共模块：注解、常量、异常、Result 统一响应
│                          #   无 Spring 依赖（仅 Lombok + Validation + Jackson）
├── yuca-infrastructure/  # 依赖 yuca-common
│                          #   Redis、PostgreSQL/MyBatis-Plus、JWT 安全、MinIO、Swagger
│                          #   JwtAuthenticationFilter、JwtTokenProvider、GlobalExceptionHandler
│                          #   FileController（MinIO 文件上传下载）
├── yuca-ai/              # 依赖 yuca-infrastructure
│                          #   AI 客户端抽象、Agent/AgentFactory/AgentBuilder
│                          #   Skill 系统（SkillRegistry、SkillExecutor、SkillTool）
│                          #   聊天历史（PostgresChatHistoryStore）
│                          #   聊天增强器（SystemPrompt、History、Skill enhancers）
├── yuca-user/            # 依赖 yuca-infrastructure
│                          #   用户认证（登录/注册/刷新令牌）、TokenService
│                          #   UserController、UserService、UserApplicationService
├── yuca-knowledge/       # 依赖 yuca-infrastructure
│                          #   知识库 CRUD、文档上传处理
│                          #   pgvector 向量嵌入（1024 维，HNSW 索引）
│                          #   语义搜索、分块存储
├── yuca-note/            # 依赖 yuca-infrastructure
│                          #   笔记本、文件夹/文档（单表树形结构）、标签
│                          #   NoteBookController、NoteItemController、NoteTagController
├── yuca-assistant/       # 依赖 yuca-ai + yuca-infrastructure
│                          #   聊天会话 + SSE 流式传输
│                          #   AssistantController、AssistantService
│                          #   AsyncConfig（SSE 配置）、SseEvent DTO
├── yuca-diet/            # 依赖 yuca-infrastructure
│                          #   饮食记录追踪（记录、目标、趋势分析）
│                          #   DietRecordController、DietGoalController、DietTrendController
└── yuca-app/             # 聚合模块，依赖上述所有模块
                           #   包含 YucaApplication.java（启动类）
                           #   application.yml、application-ai.yml、DDL SQL 文件
```

### 核心设计模式

**模块包结构**：每个模块遵循 `org.yuca.<模块名>/{controller,service,mapper,entity,dto/{request,response,internal},enums,config}`

**认证**：基于 JWT，access token 24小时，refresh token 30天。`JwtAuthenticationFilter` 拦截请求；`@SkipAuth` / `@RequireLogin` 注解控制访问权限。

**统一响应**：所有 Controller 返回 `Result<T>`（`{ code, message, data, timestamp }`），`code: 200` 表示成功。

**AI 集成**：`AgentFactory` 通过 `AgentBuilder` 创建 Agent。Agent 使用增强器（SystemPrompt、History、Skill）和 Skill 系统实现工具调用。模型在 `application-ai.yml` 中配置（Dashscope/Qwen 和 DeepSeek）。

**SSE 流式传输**：Assistant 模块使用 Spring `SseEmitter` 实现实时聊天流式传输。Tomcat 配置长超时（300s）并禁用压缩以支持 SSE。

**数据库**：PostgreSQL + MyBatis-Plus。逻辑删除（`deleted` 字段）。pgvector 扩展用于 1024 维向量嵌入，HNSW 索引。项目所有表的 DDL 脚本位于 `yuca-app/src/main/resources/table/`（按模块分子目录，每个表一个文件）。

### 新增后端模块

1. 在 `yuca-backend/` 下创建 `yuca-<名称>/` 模块目录
2. 在父 `pom.xml` 中添加 `<module>`
3. 在 `<dependencyManagement>` 和 `yuca-app/pom.xml` 中添加 `<dependency>`
4. 遵循包结构：`org.yuca.<名称>/{controller,service,mapper,entity,dto,...}`
5. Entity 使用：`@TableName`、`@TableId(type = IdType.AUTO)`、`@TableLogic`
6. 在 `yuca-app/src/main/resources/table/<模块名>/` 添加 DDL 脚本（每个表一个文件）
7. 在 `application.yml` 的 `springdoc.packages-to-scan` 中添加新 Controller 包路径

## 前端开发命令

所有命令在 `yuca-react/` 目录下执行：

```bash
npm install          # 安装依赖
npm run dev          # 开发服务器（端口 5174，代理 /api 到 localhost:8500）
npm run build        # 生产构建（tsc -b && vite build）
npm run lint         # ESLint 检查
npm run preview      # 预览生产构建
```

## 前端架构

### 技术栈
- **React 19** + TypeScript
- **Vite 8** 构建工具，`@` 别名指向 `src/` 目录
- **Tailwind CSS 4**（通过 `@tailwindcss/vite` 插件）
- **shadcn/ui** 组件库（base-nova 风格，lucide 图标）
- **Zustand** 状态管理
- **React Router DOM 7**（`createBrowserRouter`）
- **Axios** 请求/响应拦截器

### 目录结构

```
yuca-react/src/
├── api/              # Axios 客户端（index.ts）+ 按功能的 API 模块
├── components/       # 可复用组件
│   └── ui/           # shadcn/ui 基础组件（button、input、dialog 等）
├── hooks/            # 自定义 React Hooks（useAutoSave）
├── layouts/          # MainLayout（Navbar + Outlet）、SidebarLayout
├── pages/            # 按功能划分的页面组件
│   ├── Home/         ├── Login/     ├── Blog/
│   ├── Notes/        ├── Wiki/      ├── Assistant/
│   ├── Diet/         ├── Profile/   ├── Tools/
│   └── ResetPassword/
├── stores/           # Zustand 状态管理（user.ts、diet.ts）
├── types/            # TypeScript 类型定义
├── router.tsx        # 路由配置
├── App.tsx           # 根组件（TooltipProvider + RouterProvider）
└── main.tsx          # 入口文件
```

### 核心设计模式

**API 客户端**（`src/api/index.ts`）：Axios 实例，baseURL 为 `/api`。请求拦截器从 localStorage 读取 token 添加 `Authorization: Bearer <token>`。响应拦截器在 `code: 200` 时自动提取 `data`；401 时清除 token 并跳转 `/login`。包含自定义错误码到中文提示的映射。

**路由**（`src/router.tsx`）：`createBrowserRouter`，大部分路由由 `MainLayout` 包裹。Login/ResetPassword 为独立页面。未匹配路由重定向到 `/`。

**状态管理**：Zustand store 持久化到 localStorage。`useUserStore` 管理 token 和用户信息，提供 `initAuth()`、`clearAuth()`、`fetchUserInfo()` 方法。

**布局**：`MainLayout` 渲染 `Navbar` + `Outlet`，内容居中（`max-w-7xl`）。背景色 `#FFFAF0`。

### 新增前端页面

1. 创建 `src/pages/<功能名>/index.tsx`
2. 在 `src/api/<功能名>.ts` 中创建 API 模块，使用 `request` 导入自 `./index`
3. 在 `src/types/` 中添加类型定义
4. 如需状态管理，在 `src/stores/` 中创建 Zustand store
5. 在 `src/router.tsx` 的 `MainLayout` children 中添加路由

## 配置信息

- 后端服务：端口 **8500**，上下文路径 `/api`
- 前端开发服务器：端口 **5174**，代理 `/api` 到 `http://localhost:8500`
- PostgreSQL：`47.94.247.12:5432/yuca`
- Redis：`47.94.247.12:6379`
- MinIO：`47.94.247.12:9000`，bucket `yuca`
- AI 模型：在 `application-ai.yml` 中配置（Spring profile `ai`）
