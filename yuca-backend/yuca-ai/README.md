# yuca-ai 模块

AI 能力模块，集成 LangChain4j 框架，提供对话、流式输出、记忆存储和 Skill 系统等功能。

## 技术栈

- **LangChain4j** — AI 应用框架（AiServices、ChatModel、Tool、RAG）
- **Qwen / DashScope** — 默认大语言模型（通义千问）
- **PgVector** — 向量存储（RAG 检索增强）
- **Reactor** — 响应式流式输出

## 模块结构

```
yuca-ai/src/main/java/org/yuca/ai/
├── AiClient.java                  # AI 客户端统一入口
├── config/
│   ├── ChatMemoryConfig.java      # ChatMemory Provider 配置
│   ├── DashscopeProperties.java   # DashScope 配置属性
│   └── OpenAiProperties.java      # OpenAI 配置属性
├── controller/
│   ├── AiController.java          # AI 对话接口（chat / stream / memory）
│   └── SkillController.java       # Skill 查询与执行接口
├── entity/
│   └── Conversation.java          # 对话记录实体
├── handler/
│   └── JsonbTypeHandler.java      # PostgreSQL JSONB 类型处理器
├── mapper/
│   └── ConversationMapper.java    # 对话记录 Mapper
├── memory/
│   └── PostgresChatMemoryStore.java  # 基于 PostgreSQL 的对话记忆存储
├── service/
│   ├── AiServiceFactory.java      # AI 服务工厂（条件装配）
│   ├── Assistant.java             # 通用 AI 对话接口
│   ├── MemoryChatService.java     # 带记忆的对话服务接口
│   └── NoteAssistant.java         # 笔记助手接口
├── skill/
│   ├── SkillDefinition.java       # Skill 元数据定义
│   ├── SkillLoader.java           # Skill 文件加载器（启动扫描）
│   ├── SkillRegistry.java         # Skill 注册表
│   └── SkillService.java          # Skill 执行服务（模板展开）
└── tool/
    └── Calculator.java            # 示例 Tool（计算器）

yuca-ai/src/main/resources/
├── db/migration/
│   └── V1__create_conversation_tables.sql
├── mapper/
│   └── ConversationMapper.xml
└── skills/
    ├── code-review/SKILL.md       # 代码审查 Skill
    └── commit/SKILL.md            # 生成 commit message Skill
```

## 功能模块

### 1. AI 对话

通过 LangChain4j AiServices 构建 AI 对话服务，支持普通对话、流式输出和带记忆的对话。

| 接口 | 方法 | 说明 |
|------|------|------|
| `GET /ai/chat` | chat | 普通对话（无状态） |
| `GET /ai/streamChat` | streamChat | 流式对话（SSE） |
| `GET /ai/chatWithMemory` | chatWithMemory | 带记忆的对话（PostgreSQL 存储） |

**带记忆对话参数：**
- `message` — 用户消息
- `sessionId` — 会话 ID（默认 `default`），用于隔离不同对话

### 2. Chat Memory

对话历史持久化到 PostgreSQL，支持多会话隔离。

- **PostgresChatMemoryStore** — 实现 `ChatMemoryStore` 接口，将消息存入 `ai_conversation` 表
- **ChatMemoryConfig** — 配置 `ChatMemoryProvider`，每个 sessionId 保留最近 50 条消息

**建表 SQL：**
```sql
CREATE TABLE ai_conversation (
    id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(255) NOT NULL,
    message_type VARCHAR(50) NOT NULL,  -- USER / AI
    content TEXT NOT NULL,
    tool_calls JSONB,
    created_at TIMESTAMP DEFAULT NOW(),
    deleted INTEGER DEFAULT 0
);
```

### 3. Tool Calling

通过 LangChain4j `@Tool` 注解定义工具，AI 可在对话中自动调用。

当前内置工具：
- **Calculator** — 字符串长度、加法、乘法、开方

在 `AiServices.builder()` 中通过 `.tools(new Calculator())` 注册。

### 4. Skill 系统

Skill 是结构化的 prompt 模板，支持元数据定义和参数替换。

**核心流程：** 加载 SKILL.md → 注册到 SkillRegistry → API 查询/执行 → 模板变量替换 → 返回展开后的 prompt

#### API 接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `GET /ai/skills/list` | list | 获取所有 Skill 列表 |
| `GET /ai/skills/{name}` | detail | 获取 Skill 详情 |
| `POST /ai/skills/{name}/execute` | execute | 执行 Skill，返回展开后的 prompt |

#### SKILL.md 文件格式

每个 Skill 由 `resources/skills/<name>/SKILL.md` 定义：

```markdown
---
name: commit
description: 生成规范的 git commit message
when_to_use: 当用户要求提交代码时
arguments:
  - message
argument-hint: "<message> 可选的提交信息提示"
---

prompt 正文，支持 $ARGUMENTS 和 $message 变量替换。
```

**Frontmatter 字段：**
- `name` — Skill 名称（省略则取目录名）
- `description` — 功能描述
- `when_to_use` — 使用时机
- `arguments` — 命名参数列表
- `argument-hint` — 参数提示文本
- `allowed-tools` — 允许使用的工具列表

**变量替换：**
- `$ARGUMENTS` — 替换为用户传入的原始参数
- `$paramName` — 替换为对应的命名参数值（按空格分割，按序匹配）

#### 新增 Skill

在 `resources/skills/` 下创建目录并添加 `SKILL.md` 即可，重启后自动加载。

## 配置

DashScope（通义千问）相关配置在 `application.yml` 中：

```yaml
langchain4j:
  dashscope:
    chat-model:
      api-key: your-api-key
      model-name: qwen3.5-flash
```
