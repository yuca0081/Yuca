# Yuca AI 技能系统

基于 Claude Code 源码设计，完整的 Java 实现。

## 🎯 设计理念

借鉴 Claude Code 的 Skill 系统，核心设计理念：

1. **Prompt Engineering** - 技能本质是可复用的提示词模板
2. **Tool Restrictions** - 通过限制可用工具来控制技能行为
3. **Dynamic Discovery** - 运行时动态发现和加载技能
4. **Multi-layer Loading** - 支持多层级技能加载

## 📁 核心组件

### 1. 技能定义层

```java
public interface SkillDefinition {
    SkillMetadata getMetadata();
    List<ChatMessage> generatePrompt(String args, SkillExecutionContext context);
    boolean isEnabled();
    void beforeExecution(SkillExecutionContext context);
    void afterExecution(SkillExecutionContext context, String result);
}
```

### 2. 技能元数据

```java
@Data
@Builder
public class SkillMetadata {
    private String name;                  // 技能名称
    private String description;           // 描述
    private String whenToUse;             // 使用场景
    private String argumentHint;          // 参数提示
    private Set<String> allowedTools;     // 允许的工具
    private boolean userInvocable;        // 是否用户可调用
    private String version;               // 版本
    private String skillRoot;             // 技能根目录
    private SkillSource source;           // 来源
    private List<String> pathPatterns;    // 路径模式（条件激活）
}
```

### 3. 技能注册表

```java
@Component
public class SkillRegistry {
    // 注册技能
    public void register(SkillDefinition skill);

    // 获取技能
    public Optional<SkillDefinition> getSkill(String name);

    // 条件激活技能
    public List<SkillDefinition> activateConditionalSkills(
        List<String> filePaths, String projectPath
    );
}
```

### 4. 技能执行器

```java
@Service
public class SkillExecutor {
    public SkillExecutionResult execute(
        String skillName,
        String args,
        SkillExecutionContext context
    );

    public Map<String, SkillExecutionResult> executeBatch(
        Map<String, String> skillArgs,
        SkillExecutionContext context
    );
}
```

## 🚀 技能类型

### 1. 内置技能 (Bundled Skills)

编译到代码中的技能，在启动时自动注册：

```java
@Component
public class DebugSkill implements ApplicationListener<ContextRefreshedEvent> {
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        registerDebugSkill();
    }
}
```

**优点**:
- 性能最好
- 类型安全
- 可以执行复杂逻辑

**缺点**:
- 需要重新编译才能修改

### 2. 文件技能 (File-based Skills)

从 Markdown 文件加载的技能：

```
.claude/skills/
├── code-review/
│   └── SKILL.md
├── database/
│   └── SKILL.md
└── security/
    └── SKILL.md
```

**SKILL.md 格式**:

```markdown
---
name: 代码审查
description: 对代码进行全面审查
version: 1.0.0
when_to_use: 当代码需要审查时使用
allowed_tools: Read, Grep, Glob
---

# 技能内容

具体的提示词内容...
```

**优点**:
- 易于修改和维护
- 支持 Markdown 格式
- 可以版本控制

**缺点**:
- 需要解析和验证
- 性能略低于内置技能

### 3. 条件技能 (Conditional Skills)

基于路径模式激活的技能：

```yaml
---
name: Frontend Rules
paths: src/frontend/**,src/components/**
---
```

只在匹配的文件被操作时激活。

## 🔌 钩子系统

在技能执行的不同阶段插入自定义逻辑：

```java
public interface SkillHook {
    void beforeExecution(SkillDefinition skill, SkillHookContext context);
    void afterExecution(SkillDefinition skill, SkillHookContext context, Object result);
    void onError(SkillDefinition skill, SkillHookContext context, Exception exception);
}
```

**使用示例**:

```java
@Component
public class LoggingHook implements SkillHook {

    @Override
    public void beforeExecution(SkillDefinition skill, SkillHookContext context) {
        log.info("开始执行技能: {}", skill.getMetadata().getName());
    }

    @Override
    public void afterExecution(SkillDefinition skill, SkillHookContext context, Object result) {
        log.info("技能执行完成: {}", skill.getMetadata().getName());
    }
}
```

## 📡 REST API

### 获取技能列表

```http
GET /api/skills/list
```

### 获取技能详情

```http
GET /api/skills/{skillName}
```

### 执行技能

```http
POST /api/skills/{skillName}/execute
Content-Type: application/json

{
  "args": "参数内容",
  "sessionId": "session-123",
  "projectPath": "/path/to/project"
}
```

### 批量执行技能

```http
POST /api/skills/batch-execute
Content-Type: application/json

{
  "skillArgs": {
    "code-review": "审查 UserController.java",
    "security-check": "检查认证逻辑"
  },
  "sessionId": "session-123",
  "projectPath": "/path/to/project"
}
```

## 🛠️ 创建自定义技能

### 方式1: Java 内置技能

```java
@Component
public class MyCustomSkill implements ApplicationListener<ContextRefreshedEvent> {

    private final SkillRegistry skillRegistry;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        SkillDefinition skill = new SkillDefinition() {
            @Override
            public SkillMetadata getMetadata() {
                return SkillMetadata.builder()
                        .name("my-skill")
                        .description("我的自定义技能")
                        .build();
            }

            @Override
            public List<ChatMessage> generatePrompt(String args, SkillExecutionContext context) {
                return List.of(new SystemMessage("提示词内容..."));
            }
        };

        skillRegistry.register(skill);
    }
}
```

### 方式2: Markdown 文件技能

创建 `.claude/skills/my-skill/SKILL.md`:

```markdown
---
name: My Skill
description: 我的技能描述
version: 1.0.0
allowed_tools: Read, Grep
---

# 技能提示词

这是技能的具体内容...
```

## 🎨 与 LangChain4j 集成

### 1. 作为 AiService 的前置处理器

```java
@Service
public class AgenticService {

    private final SkillExecutor skillExecutor;
    private final UserAiService userAiService;

    public String processWithSkill(String skillName, String userQuery, String sessionId) {
        // 执行技能获取提示词
        var context = new SkillExecutor.DefaultSkillExecutionContext(sessionId, ".");
        var result = skillExecutor.execute(skillName, userQuery, context);

        // 将技能提示词传递给 AiService
        return userAiService.chat(result.messages(), sessionId);
    }
}
```

### 2. 作为独立的智能体能力

```java
@AiService
public interface AgentSkill {

    @SystemMessage("""
        你是一个智能助手，可以使用以下技能来帮助用户：
        - /debug: 调试问题
        - /code-review: 代码审查
        - /refactor: 代码重构

        根据用户需求，选择合适的技能。
        """)
    String assist(String userQuery);

    @Tool("执行调试技能")
    default String executeDebugSkill(String issue) {
        // 调用技能系统
        return null;
    }
}
```

## 🔐 安全特性

1. **工具权限限制**: 技能只能使用白名单中的工具
2. **沙箱执行**: 每个技能在独立的上下文中执行
3. **钩子验证**: 可以在执行前进行权限检查
4. **路径验证**: 文件技能有严格的路径验证

## 📊 监控和统计

```java
// 获取技能统计信息
GET /api/skills/statistics

// 返回示例
{
  "totalSkills": 10,
  "userInvocableSkills": 8,
  "conditionalSkills": 2,
  "activatedConditionalSkills": 1,
  "skillsBySource": {
    "BUNDLED": 3,
    "USER": 2,
    "PROJECT": 5
  }
}
```

## 🚀 最佳实践

1. **技能命名**: 使用 kebab-case (如 `code-review`)
2. **提示词设计**: 清晰的指令和期望输出格式
3. **工具限制**: 只授予必要的工具权限
4. **错误处理**: 在技能中处理异常情况
5. **版本管理**: 使用版本号追踪技能变更
6. **文档完善**: 提供清晰的 `whenToUse` 说明

## 🔧 扩展方向

1. **技能市场**: 支持分享和发现技能
2. **技能编排**: 支持多个技能的链式调用
3. **技能学习**: 根据使用反馈优化技能
4. **多模态支持**: 支持图片、音频等多模态技能
5. **分布式技能**: 支持远程技能调用

## 📚 参考资料

- Claude Code 源码: https://github.com/anthropics/claude-code
- LangChain4j 文档: https://docs.langchain4j.dev
- 技能系统设计: `src/skills/` 目录

---

**作者**: Yuca AI Team
**版本**: 1.0.0
**更新日期**: 2026-04-10
