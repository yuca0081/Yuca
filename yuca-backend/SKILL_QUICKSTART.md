# Yuca AI 技能系统 - 快速入门

## 🚀 5分钟上手

### 1. 查看所有可用技能

```bash
curl http://localhost:8500/api/skills/list
```

### 2. 执行一个技能

```bash
curl -X POST http://localhost:8500/api/skills/debug/execute \
  -H "Content-Type: application/json" \
  -d '{
    "args": "登录功能有bug",
    "sessionId": "session-123",
    "projectPath": "/path/to/project"
  }'
```

### 3. 创建你的第一个技能

创建文件 `.claude/skills/my-first-skill/SKILL.md`:

```markdown
---
name: 我的第一个技能
description: 帮助我写代码
version: 1.0.0
---

# 代码生成助手

你是一个专业的代码生成助手。根据用户的需求，生成高质量的代码。

请确保生成的代码：
- 遵循最佳实践
- 包含必要的注释
- 处理异常情况
```

重启应用后，你的新技能就会自动加载！

## 📖 核心概念

### 技能 (Skill)

技能是一个**可复用的提示词模板**，包含：
- **元数据**: 名称、描述、允许的工具等
- **提示词内容**: 具体的指令和示例
- **执行逻辑**: 如何生成响应

### 技能来源

1. **Bundled (内置)**: 编译在代码中，性能最好
2. **File (文件)**: 从 Markdown 文件加载，易于修改
3. **Conditional (条件)**: 基于路径模式激活

### 工具权限限制

技能只能使用白名单中指定的工具，例如：
```yaml
allowed_tools: Read, Grep, Glob
```

这确保了技能不会执行危险操作。

## 🎯 常见使用场景

### 1. 代码审查

```bash
curl -X POST http://localhost:8500/api/skills/code-review/execute \
  -H "Content-Type: application/json" \
  -d '{
    "args": "src/main/java/org/yuca/user/UserController.java",
    "sessionId": "session-123",
    "projectPath": "/path/to/project"
  }'
```

### 2. 调试问题

```bash
curl -X POST http://localhost:8500/api/skills/debug/execute \
  -H "Content-Type: application/json" \
  -d '{
    "args": "应用启动报错：NullPointerException",
    "sessionId": "session-123",
    "projectPath": "/path/to/project"
  }'
```

### 3. 批量执行

```bash
curl -X POST http://localhost:8500/api/skills/batch-execute \
  -H "Content-Type: application/json" \
  -d '{
    "skillArgs": {
      "code-review": "UserController.java",
      "debug": "检查错误日志"
    },
    "sessionId": "session-123",
    "projectPath": "/path/to/project"
  }'
```

## 🛠️ 创建自定义技能

### 方式 1: Markdown 文件 (推荐)

1. 创建技能目录：
```bash
mkdir -p .claude/skills/my-skill
```

2. 创建 `SKILL.md`:
```markdown
---
name: my-skill
description: 我的技能描述
version: 1.0.0
allowed_tools: Read, Grep
---

# 技能提示词

你是一个专业的助手，请帮助用户...
```

3. 重启应用，技能自动加载

### 方式 2: Java 代码

创建一个 Spring Bean：

```java
@Component
public class MyCustomSkill {

    private final SkillRegistry skillRegistry;

    @PostConstruct
    public void register() {
        SkillDefinition skill = new SkillDefinition() {
            @Override
            public SkillMetadata getMetadata() {
                return SkillMetadata.builder()
                        .name("my-skill")
                        .description("我的技能")
                        .source(SkillMetadata.SkillSource.BUNDLED)
                        .build();
            }

            @Override
            public List<ChatMessage> generatePrompt(String args, SkillExecutionContext context) {
                return List.of(new SystemMessage("你的提示词..."));
            }
        };

        skillRegistry.register(skill);
    }
}
```

## 🔧 高级功能

### 条件激活技能

只在特定文件被操作时激活：

```yaml
---
name: Frontend Rules
paths: src/frontend/**,src/components/**
---
```

### 钩子 (Hooks)

在技能执行前后添加自定义逻辑：

```java
@Component
public class LoggingHook implements SkillHook {

    @Override
    public void beforeExecution(SkillDefinition skill, SkillHookContext context) {
        log.info("开始执行: {}", skill.getMetadata().getName());
    }
}
```

### 与 LangChain4j 集成

```java
@Service
public class AgenticService {

    private final SkillExecutor skillExecutor;
    private final UserAiService aiService;

    public String chatWithSkill(String skillName, String query, String sessionId) {
        // 1. 执行技能获取提示词
        var context = new SkillExecutor.DefaultSkillExecutionContext(sessionId, ".");
        var result = skillExecutor.execute(skillName, query, context);

        // 2. 将提示词传递给 AI Service
        return aiService.chat(result.messages(), sessionId);
    }
}
```

## 📊 监控和调试

### 查看技能统计

```bash
curl http://localhost:8500/api/skills/statistics
```

返回：
```json
{
  "totalSkills": 10,
  "userInvocableSkills": 8,
  "conditionalSkills": 2,
  "skillsBySource": {
    "BUNDLED": 3,
    "USER": 2,
    "PROJECT": 5
  }
}
```

### 启用调试日志

在 `application.yml` 中配置：

```yaml
logging:
  level:
    org.yuca.ai.skill: DEBUG
```

## 🎓 最佳实践

1. **命名规范**: 使用 kebab-case (如 `code-review`)
2. **提示词设计**: 提供清晰的指令和期望输出格式
3. **工具限制**: 只授予必要的工具权限
4. **版本管理**: 使用版本号追踪变更
5. **文档完善**: 提供清晰的 `whenToUse` 说明

## 🔗 相关资源

- [完整文档](./SKILL_SYSTEM_README.md)
- [API 参考](./docs/api/skills.md)
- [示例技能](./.claude/skills/)

## 💡 提示

- 技能文件修改后需要重启应用才能生效
- 内置技能的性能优于文件技能
- 条件技能可以提供更精细的上下文控制
- 钩子系统可以用来添加日志、监控等横切关注点

---

有问题？查看 [完整文档](./SKILL_SYSTEM_README.md) 或提交 Issue！
