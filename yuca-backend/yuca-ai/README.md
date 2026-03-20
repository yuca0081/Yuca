# QwenChatClient 使用指南

## 架构设计

QwenChatClient 使用 **Builder 模式**创建，支持：
- ✅ 非单例，每个模块可创建独立实例
- ✅ 动态配置默认 tools
- ✅ 简洁易用的 API

## 核心组件

### 1. QwenChatClient（客户端）
使用 Builder 模式创建的 AI 客户端。

### 2. QwenChatClientFactory（工厂）
单例工厂，用于创建配置好的 QwenChatClient 实例。

## 使用方式

### 方式1：使用工厂创建基础客户端

```java
@Autowired
private QwenChatClientFactory factory;

// 创建基础客户端
QwenChatClient client = factory.create();

// 使用客户端
QwenChatRequest request = QwenChatRequest.builder()
    .messages(List.of(
        QwenChatRequest.QwenMessage.builder()
            .role("user")
            .content("你好")
            .build()
    ))
    .build();

ChatResponse response = client.chat(request);
```

### 方式2：创建带默认 Tools 的客户端

```java
@Autowired
private QwenChatClientFactory factory;

// 定义工具
List<AITool> tools = List.of(
    AITool.builder()
        .name("get_weather")
        .description("获取天气信息")
        .parameters(Map.of(
            "type", "object",
            "properties", Map.of(
                "city", Map.of(
                    "type", "string",
                    "description", "城市名称"
                )
            ),
            "required", List.of("city")
        ))
        .build()
);

// 创建带工具的客户端
QwenChatClient client = factory.createWithTools(tools);

// 请求会自动附加默认工具
QwenChatRequest request = QwenChatRequest.builder()
    .messages(List.of(...))
    .build(); // 无需手动设置 tools
```

### 方式3：使用 Builder 自定义配置

```java
@Autowired
private QwenChatClientFactory factory;

// 使用工厂的 builder 继续配置
QwenChatClient client = factory.builder()
    .defaultTools(tool1, tool2, tool3)
    .build();
```

### 方式4：完全自定义（不使用工厂）

```java
@Autowired
private QwenConfig config;
@Autowired
private RestTemplate qwenRestTemplate;
@Autowired
private ObjectMapper qwenObjectMapper;

QwenChatClient client = QwenChatClient.builder()
    .config(config)
    .restTemplate(qwenRestTemplate)
    .objectMapper(qwenObjectMapper)
    .defaultTools(tool1, tool2)
    .build();
```

## 模块隔离示例

```java
// 模块A：用户助手
@Service
public class UserAssistantService {
    private final QwenChatClient client;

    @Autowired
    public UserAssistantService(QwenChatClientFactory factory,
                                UserProfileTool userProfileTool) {
        this.client = factory.createWithTools(List.of(userProfileTool));
    }
}

// 模块B：代码助手
@Service
public class CodeAssistantService {
    private final QwenChatClient client;

    @Autowired
    public CodeAssistantService(QwenChatClientFactory factory,
                                CodeExecutionTool codeExecutionTool) {
        this.client = factory.createWithTools(List.of(codeExecutionTool));
    }
}
```

## 流式调用

```java
QwenChatRequest request = QwenChatRequest.builder()
    .messages(List.of(
        QwenChatRequest.QwenMessage.builder()
            .role("user")
            .content("写一首诗")
            .build()
    ))
    .build();

StringBuilder fullContent = new StringBuilder();
ChatStreamResponse finalResponse = client.chatStream(request, token -> {
    // 处理每个 token
    fullContent.append(token.getContent());
    System.out.print(token.getContent());
});

System.out.println("\n完整内容: " + fullContent.toString());
```

## 配置

在 `application.yml` 中配置：

```yaml
qwen:
  api-key: ${QWEN_API_KEY}
  base-url: https://dashscope.aliyuncs.com/compatible-mode/v1
  model: qwen3.5-flash
  max-tokens: 2000
  temperature: 0.7
```

## 迁移指南

### 从单例迁移到 Builder 模式

**之前（单例）：**
```java
@Autowired
private QwenChatClient qwenChatClient; // 全局单例
```

**现在（Builder）：**
```java
@Autowired
private QwenChatClientFactory factory;

private final QwenChatClient client;

@PostConstruct
public void init() {
    this.client = factory.create();
}
```

## 优势总结

| 特性 | 单例模式 | Builder 模式 |
|------|----------|--------------|
| 模块隔离 | ❌ 全局共享 | ✅ 独立配置 |
| 工具管理 | ❌ 手动传参 | ✅ 默认工具 |
| 可测试性 | ❌ 难以 Mock | ✅ 易于测试 |
| 扩展性 | ❌ 修改类 | ✅ Builder 配置 |
