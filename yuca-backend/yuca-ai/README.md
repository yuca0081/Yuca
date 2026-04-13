# AI模块 - Chat Memory 功能

## ⚙️ 配置要求

### 1. 添加MapperScan（必须）

在主应用类 `YucaApplication.java` 中添加：

```java
@MapperScan({
    "org.yuca.user.mapper",
    "org.yuca.infrastructure.storage.mapper",
    "org.yuca.knowledge.mapper",
    "org.yuca.note.mapper",
    "org.yuca.assistant.mapper",
    "org.yuca.ai.mapper"  // ← 必须添加这一行
})
```

### 2. 创建数据库表

```sql
CREATE TABLE ai_conversation (
    id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(255) NOT NULL,
    message_type VARCHAR(50) NOT NULL,
    content TEXT NOT NULL,
    tool_calls JSONB,
    created_at TIMESTAMP DEFAULT NOW(),
    deleted INTEGER DEFAULT 0
);
```

### 3. 接口免鉴权（已配置）

在 `AiController` 上已添加 `@SkipAuth` 注解，所有AI接口无需Token即可访问：

```java
@RestController
@RequestMapping("/ai")
@SkipAuth  // 跳过JWT认证
public class AiController {
    // ...
}
```

## 🚀 快速使用

**无需Token，直接访问！**

```sql
CREATE TABLE ai_conversation (
    id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(255) NOT NULL,
    message_type VARCHAR(50) NOT NULL,
    content TEXT NOT NULL,
    tool_calls JSONB,
    created_at TIMESTAMP DEFAULT NOW(),
    deleted INTEGER DEFAULT 0
);
```

## 🚀 快速使用

### 1. 带Memory的聊天（自动保存到PostgreSQL）

```bash
curl "http://localhost:8500/ai/chatWithMemory?message=你好&sessionId=user123"
```

### 2. 普通聊天（无记忆）

```bash
curl "http://localhost:8500/ai/chat?message=你好"
```

### 3. 流式聊天

```bash
curl "http://localhost:8500/ai/streamChat?message=你好"
```

## 💡 代码示例

### 在Controller中使用

```java
@RestController
@RequiredArgsConstructor
public class MyController {
    
    private final ChatMemoryProvider chatMemoryProvider;
    
    @PostMapping("/chat")
    public String chat(@RequestParam String message,
                      @RequestParam String sessionId) {
        
        // 创建ChatModel
        ChatModel model = QwenChatModel.builder()
                .apiKey("your-api-key")
                .modelName("qwen3.5-flash")
                .build();
        
        // 获取ChatMemory（自动使用PostgreSQL存储）
        MessageWindowChatMemory memory =
                (MessageWindowChatMemory) chatMemoryProvider.get(sessionId);
        
        // 创建ChatService
        MemoryChatService chatService = AiServices.builder(MemoryChatService.class)
                .chatModel(model)
                .chatMemory(memory)
                .build();
        
        // 调用AI（自动保存到PostgreSQL）
        return chatService.chat(message);
    }
}
```

## 📁 核心文件

### ChatMemoryStore实现
- **PostgresChatMemoryStore.java** - 将对话保存到PostgreSQL

### 配置
- **ChatMemoryConfig.java** - ChatMemoryProvider配置

### Service接口
- **MemoryChatService.java** - AI聊天接口

### 数据层
- **Conversation.java** - 对话记录实体
- **ConversationMapper.java** - 数据访问接口
- **ConversationMapper.xml** - SQL映射

### Controller
- **AiController.java** - 所有AI接口（包含chatWithMemory）

## 🗄️ 数据库表

```sql
CREATE TABLE ai_conversation (
    id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(255) NOT NULL,
    message_type VARCHAR(50) NOT NULL,  -- 'USER' or 'AI'
    content TEXT NOT NULL,
    tool_calls JSONB,
    created_at TIMESTAMP DEFAULT NOW(),
    deleted INTEGER DEFAULT 0
);
```

## ✨ 功能特性

1. **自动保存** - 调用AI时自动保存用户消息和AI回复
2. **上下文记忆** - AI能记住之前的对话（最近50条）
3. **多会话隔离** - 每个sessionId有独立的对话历史
4. **持久化存储** - 所有对话保存在PostgreSQL中

## 🎯 API说明

### GET /ai/chatWithMemory
带记忆的聊天接口

**参数：**
- `message`: 用户消息
- `sessionId`: 会话ID（默认: "default"）

**响应：**
```json
{
  "success": true,
  "sessionId": "user123",
  "userMessage": "你好",
  "response": "你好！有什么我可以帮助你的吗？"
}
```

## 📝 核心流程

```
AiController.chatWithMemory()
        ↓
获取 ChatMemoryProvider.get(sessionId)
        ↓
创建 AiServices.builder()
        ↓
调用 chatService.chat(message)
        ↓
PostgresChatMemoryStore 自动保存到PostgreSQL
```

## 🔧 配置

### ChatMemoryConfig.java
```java
@Bean
public ChatMemoryProvider chatMemoryProvider(ChatMemoryStore chatMemoryStore) {
    return memoryId ->
            MessageWindowChatMemory.builder()
                    .id(memoryId)
                    .maxMessages(50)
                    .chatMemoryStore(chatMemoryStore)  // PostgreSQL存储
                    .build();
}
```

## 📚 总结

**核心组件：**
- PostgresChatMemoryStore - ChatMemoryStore实现
- ChatMemoryProvider - 提供ChatMemory实例
- AiController - 提供API接口

**使用方式：**
```java
// 1. 获取ChatMemory
MessageWindowChatMemory memory = 
    (MessageWindowChatMemory) chatMemoryProvider.get(sessionId);

// 2. 创建ChatService
MemoryChatService chatService = AiServices.builder(MemoryChatService.class)
        .chatModel(model)
        .chatMemory(memory)
        .build();

// 3. 调用（自动保存）
String response = chatService.chat(message);
```

就这么简单！🎉
