# AI 模块待办清单（对标简历项目）

> 本文档对比简历"智慧工会AI智能服务"项目（2025.08-2026.05）描述的能力与当前 `yuca-ai` + `yuca-assistant` 模块已实现功能，列出尚未实现或实现不完整的功能点，按优先级分类。
>
> 对标基准：简历中描述的 9 大 AI 工作项 + 职业技能中的 Agent/RAG/MCP 能力

## 对标总览

| 简历功能 | 当前项目状态 | 优先级 |
|---|---|---|
| Agent 架构（增强器链 + 工厂 + Builder + 洋葱模型） | ✅ 已实现 | — |
| Tool Calling 多轮调用 | ✅ 已实现 | — |
| 工具错误重试（3 次压缩回塞） | ✅ 已实现 | — |
| Skill 系统（Markdown 模板 + 自动发现） | ✅ 已实现 | — |
| Plan 模式（探索→设计→审批→执行） | ❌ 未实现 | P0 |
| ReAct 框架（思考-行动-观察循环） | ⊘ 已被 native tool calling 覆盖（现代形态等价） | ~~P0~~ |
| MCP 协议工具集成 | ❌ 未实现 | P0 |
| 语义缓存（向量相似度命中历史答案） | ❌ 未实现 | P0 |
| 混合检索：pgvector + ES BM25 + 元数据过滤 | ⚠️ 用 PG `ts_query` 替代 ES；无元数据过滤 | P1 |
| Prompt 工程（Few-shot / CoT / 角色 / A/B 测试） | ⚠️ 仅硬编码 system prompt + skill 列表拼接 | P1 |
| 动态上下文窗口（摘要压缩替代固定截断） | ✅ 已实现（SummaryEnhancer） | — |
| 意图识别路由 | ✅ 已实现（IntentRecognitionEnhancer） | — |
| 长期记忆（跨会话用户偏好） | ❌ 未实现 | P1 |
| 护栏链（与增强器链对称的 after 链） | ❌ 未实现 | P1 |
| PII 敏感信息脱敏（身份证/手机号/银行卡） | ❌ 未实现 | P1 |
| RAG 答案溯源校验防幻觉 | ❌ 未实现 | P1 |
| OTEL 全链路追踪（决策路径树） | ❌ 未实现 | P2 |
| Agent 评测体系（黄金评测集 + LLM-as-Judge） | ❌ 未实现 | P2 |
| CI 回归门禁（分数下降自动拦截） | ❌ 未实现 | P2 |
| 分层限流（固定窗口 + 漏桶 + Redis ZSet） | ❌ 未实现 | P2 |
| SSE 快慢通道线程池隔离 | ❌ 未实现 | P2 |
| AiTestController 硬编码 API Key | 🟡 代码已修复（待吊销旧 Key + 清理 git 历史） | P0 |

---

## P0 - 核心架构缺失（简历主线功能，面试硬伤）

### 1. Plan 模式任务规划

**现状**：`Agent.execute` 只有一个工具调用循环——模型直接产出 AiMessage，若含 `toolExecutionRequests` 则执行工具，结果回塞后进入下一轮。没有"先探索→出方案→等用户审批→再执行"的阶段分离。

**简历原话**：
> 针对**复杂任务**借鉴 Claude Code 实现 Plan 模式——"探索→设计→审批→执行"四阶段分离工作流，探索阶段仅配置读工具防止写操作，生成结构化执行计划返回用户审批，确认后切换执行模式动态注入写工具

**目标**：

- 新增 `AgentMode` 枚举：`EXPLORE / PLAN / EXECUTE`
- `EXPLORE` 模式：只注入读类工具（如 `listSkills`、`retrieve`），禁止写类工具（如 `executeSkill` 产生副作用时、未来文件写入工具）
- `PLAN` 模式：要求 LLM 输出结构化 JSON 计划（步骤数组），SSE 推送给前端等待用户审批
- `EXECUTE` 模式：用户确认后切换，动态注入写工具，按计划逐步执行
- 新增 REST 端点 `POST /api/assistant/sessions/{id}/approve` 接收审批结果

**涉及文件**：
- 新建 `agent/AgentMode.java`（枚举）
- 新建 `agent/enhancer/PlanEnhancer.java`（PLAN 模式下拦截工具调用、产出结构化计划）
- 修改 `agent/Agent.java`：`execute` 分支不同模式；`EXPLORE` 过滤 toolSpecifications
- 修改 `agent/AgentFactory.java`：新增 `planAgent(ChatContext, AgentMode)` 工厂方法，按模式注入不同工具集
- 修改 `AssistantController/Service`：增加审批端点，SSE 推送计划

**关键决策**：
- 探索 vs 执行的工具白名单写在 `SkillDefinition.allowedTools` 里（字段已存在但未使用）
- 计划格式建议：`{ steps: [{ tool, args, rationale }], risk_level }`，便于前端渲染
- 审批态需要持久化到 `assistant_session` 表，避免 SSE 断线后丢上下文

**范围说明**：
- 不含计划修正/再审批循环（用户拒绝后重新规划）
- 不含计划自动回滚（执行失败时 undo）
- 不含多步计划的中间审批（每步都问）

**遗留风险**：
- 审批等待期间 SseEmitter 可能超时（当前 300s），需要心跳保活
- 前端需配合实现计划渲染 UI

---

### 2. ReAct 框架 ⊘ 取消（2026-07-22，已被 native tool calling 覆盖）

**取消原因**：重新精读简历后修正解读。

简历原文：
> Agent 基础运行基于 ReAct 框架的"思考-行动-观察"循环

原始 ReAct 论文（Yao 2022）是**前 native tool calling 时代**的产物——那时 LLM 没有 function calling，必须用 prompt 强制模型输出 `Thought: ... Action: ... Observation: ...` 文本格式再正则解析。2024 年后主流模型（Qwen / DeepSeek / Claude）都支持 native tool calling，"Thought"发生在模型内部，"Action"走结构化 `toolExecutionRequests`，"Observation"走结果回塞——**这就是现代形态的 ReAct，功能等价**。

当前 `Agent.execute` 的"模型直答 + 工具循环"就是 ReAct 的现代实现：

```
chatModel.chat → 含 toolRequests → 执行 → 结果回塞 → 下一轮
      ↑                ↑              ↑
    Thought         Action        Observation
  （模型内部）    （tool call）  （tool result）
```

**两项有价值的子任务拆分并入其他条目**：
- **Thought 显式采集（`reasoning_content` 字段）** → 并入 P2-#10 OTEL，作为 trace span 的属性记录。当前 `AssistantMessage.thinkingContent` 字段已预留但未采集，需要 QwenStreamingChatModel 解析 `reasoning_content` 字段时落库。
- **工具结果回塞规范化**（用 `ToolExecutionResultMessage` 替代当前 UserMessage 包装）→ 并入 P0-#3 MCP 改造，顺手统一消息类型。当前 `Agent.java:92` 把工具结果硬编码成 UserMessage 是为兼容 Qwen/DashScope 的历史妥协。

**面试讲法建议**：被问到 ReAct 时直接说"我们用的是 native function calling 形态，Thought 在模型内部完成，通过 `reasoning_content` 字段做可观测性采集，不强制文本格式 prompt"。这样比硬上文本格式 ReAct 显得更理解原理。

---

### 3. MCP 协议工具集成

**现状**：`ToolExtractor` 基于 `@Tool` 注解反射提取本地 Bean 的方法。无法接入外部 MCP server（如官方 filesystem / fetch / git server）。简历"职业技能"和"核心工作7"都明确提及 MCP。

**目标**：
- 新建 `tool/mcp/McpClient.java`：实现 MCP 协议（JSON-RPC over stdio / SSE）
- 新建 `tool/mcp/McpToolAdapter.java`：把 MCP server 暴露的 tools 适配为 `ToolSpecification` + `ToolExecutor`
- 启动时根据配置拉起 MCP server 子进程，注册到 `ToolExtractor`
- 配置项：`yuca.ai.mcp.servers[].name / command / args / env`

**涉及文件**：
- 新建 `tool/mcp/McpClient.java`、`McpToolAdapter.java`、`McpServerConfig.java`
- 修改 `tool/ToolExtractor.java`：构造时合并本地 tools + MCP tools
- 修改 `config/AiProperties.java`：加 `McpConfig` 嵌套类
- 修改 `application.yml`：加 `yuca.ai.mcp.servers` 配置示例
- 新增 `pom.xml` 依赖：`io.modelcontextprotocol:mcp` 官方 SDK（可选，或自实现）
- **顺手修复**：修改 `agent/Agent.java:92` 把工具结果从 UserMessage 包装改为 `ToolExecutionResultMessage`，统一消息类型体系（原硬编码是为了兼容 Qwen，引入 MCP 后多源工具结果更不能走 UserMessage 文本拼接）

**关键决策**：
- 传输方式：**stdio 优先**（本地子进程，低延迟，调试方便），SSE 留作未来
- 生命周期：随 JVM 启动子进程，ShutdownHook 关闭
- 权限：MCP server 的工具同样受 Plan 模式的 `allowedTools` 白名单约束

**范围说明**：
- 不含 MCP 资源（Resources）/提示（Prompts）支持，仅 Tools
- 不含动态发现远程 MCP server（mDNS 等）
- 不含 MCP server 鉴权（OAuth）

**遗留风险**：
- 子进程崩溃需重启机制
- 多 Agent 并发调用同一 MCP server 需要线程安全

---

### 4. 语义缓存

**现状**：每次用户问询都走完整的 embedding → 检索 → rerank → LLM 调用链。高频常见问题（如"你好"、"密码怎么改"）每次都消耗 LLM token。

**简历原话**：
> 构建语义相似度缓存系统，对用户问题向量化并与缓存问题计算余弦相似度，命中缓存直接返回历史答案，减少 AI 调用次数

**目标**：
- 新建表 `ai_semantic_cache`：`id / kb_id / question / question_embedding(vector1024) / answer / hit_count / created_at / expired_at / deleted`
- 新建 `cache/SemanticCacheService.java`：
  - `lookup(query, kbId, threshold=0.95)`：embed query → HNSW 召回 top1 → 阈值判断 → 返回缓存答案
  - `put(query, answer, kbId, ttl)`：写入缓存
- 新建 `agent/enhancer/SemanticCacheEnhancer.java`（order=-3，比 Summary 还早）：
  - `before`：命中时把 answer 塞进 context 跳过 LLM 调用
  - `after`：未命中且本轮成功生成答案时异步写入缓存
- 修改 `Agent.execute`：检查 context 的缓存命中标记，跳过 chatModel.chat

**涉及文件**：
- 新建 DDL `yuca-app/src/main/resources/table/ai/ai_semantic_cache.sql`
- 新建 `entity/AiSemanticCache.java` + `mapper/AiSemanticCacheMapper.java`
- 新建 `cache/SemanticCacheService.java`
- 新建 `agent/enhancer/SemanticCacheEnhancer.java`
- 修改 `agent/AgentFactory.java`：`defaultAgent` 链中加入 SemanticCacheEnhancer
- 修改 `agent/Agent.java`：识别 cache hit 标记，短路 LLM 调用

**关键决策**：
- 阈值：0.95（极高相似度才命中，宁可少命中也不答错）
- TTL：7 天（业务 FAQ 较稳定；Prompt 变更后旧缓存通过 `kb_id` 维度批量失效）
- 失效策略：文档重建（`KnowledgeDocService` 分支 C）时按 `kb_id` 清空该 KB 的缓存
- 命中后是否仍走 RAG 检索：不走（缓存的是完整答案）

**范围说明**：
- 不含负向缓存（检索结果为空时也缓存"无答案"）
- 不含缓存预热
- 不含管理后台手动清缓存接口

**遗留风险**：
- 缓存雪崩：大批量同义问题同时失效——概率低
- 缓存穿透：刻意构造低相似度 query 绕过——本场景不敏感

---

### 5. AiTestController 硬编码 API Key 🟡 安全修复（代码已完成，待用户吊销旧 Key）

**现状（2026-07-22 代码修复完成）**：
- ✅ `AiTestController.java:43` 硬编码 Key 已移除，改用 `dashscope.getApiKey()`
- ✅ `application.yml` 中 dashscope / deepseek 两个 api-key 改为 `${ENV:}` 占位
- ✅ `application-local.yml` 承载明文 Key（已通过 `git rm --cached` 移出索引，并加入 `.gitignore`）
- ✅ `application.yml` 顶部加入 `spring.profiles.active: ${SPRING_PROFILES_ACTIVE:local}`，本地默认激活 local profile 自动覆盖 Key

**代码变更**：
- 修改 `controller/AiTestController.java`（删硬编码）
- 修改 `yuca-app/src/main/resources/application.yml`（key 占位 + profile 默认 local）
- 修改 `yuca-app/src/main/resources/application-local.yml`（加入 Key 明文）
- 修改 `.gitignore`（新增 `**/application-local.yml` 规则；`application-prod.yml` 保留入仓，因为只含服务器本机地址无敏感信息）
- `git rm --cached application-local.yml`（本地文件保留）

**🔴 剩余用户侧操作（不在代码改动范围）**：
1. **立即吊销旧 Key**：登录 dashscope 控制台吊销 `sk-4632c16e41c64e738d3b4147aa58581f`；登录 deepseek 控制台吊销 `sk-46aaad92e53e4cfb8a165e99daaf17bc`
2. **生成新 Key** 并写入 `application-local.yml`
3. **可选但推荐**（仓库公开前必须做）：用 `git filter-repo` 或 BFG 清理 git 历史 commit 中的 Key
   ```bash
   # 用 BFG 清理（比 filter-repo 快）
   bfg --replace-text passwords.txt
   git reflog expire --expire=now --all && git gc --prune=now --aggressive
   git push --force  # ⚠️ 协作者需重新克隆
   ```

**遗留风险**：
- git 历史中的旧 Key 在吊销前仍可用，吊销动作不可拖延
- 若协作者已克隆旧版本，清理历史后需通知重新克隆

---

## P1 - 精度与治理提升

### 6. 混合检索补全（ES + IK 分词 + 元数据过滤）

**现状**：`KnowledgeRetrievalService` 的关键词路用的是 PostgreSQL `websearch_to_tsquery` + zhparser 分词。简历明确写"ES + IK 分词"。同时检索只支持按 `kbId` 过滤，无元数据筛选。

**目标**：
- 引入 ElasticSearch 客户端（`co.elastic.clients:elasticsearch-java`）
- `knowledge_chunk` 同步双写到 ES（`KnowledgeDocService.saveChapterNodes` 时并行写入）
- BM25 检索从 PG 改走 ES，支持 IK 分词、boost、phrase query
- 检索接口增加元数据过滤参数：`?source=official&tag=政策&dateFrom=...`

**涉及文件**：
- 新增 `yuca-infrastructure/es/EsClient.java` + `EsProperties.java`
- 新建 `knowledge/search/EsSearchService.java`：封装 BM25 查询
- 修改 `KnowledgeRetrievalService.retrieve`：BM25 路改为调 EsSearchService
- 修改 `KnowledgeDocService`：写入 chunk 时同步写 ES（可选异步队列）
- 修改 `dto/request/SemanticSearchRequest.java`：加元数据过滤字段
- DDL `knowledge_chunk.sql`：加 `tags VARCHAR[]`、`metadata JSONB`（已预留未用）

**关键决策**：
- 双写 vs 只写 ES：**双写**（PG 为事实源，ES 为检索副本，崩溃可重建）
- IK 分词模式：`ik_max_word`（写入时细粒度）/ `ik_smart`（查询时粗粒度）
- ES 部署：单节点起步（本场景数据量 < 100 万）

**范围说明**：
- 不含 ES 高可用集群
- 不含 reindex 任务（数据量小，全量重建即可）

---

### 7. Prompt 工程体系化

**现状**：`AgentFactory.buildSystemPrompt()` 是硬编码字符串 + skill 列表拼接。没有 Few-shot 示例库、思维链提示、角色模板、A/B 测试机制。

**目标**：
- 新建 `prompt/PromptTemplateRegistry.java`：按场景（审批/咨询/创作/总结）加载 YAML 模板
- 模板支持变量插值、Few-shot 示例块、CoT 引导语
- 新建 `prompt/PromptVariantService.java`：A/B 测试——按 sessionId hash 分桶，记录每桶 token 消耗与用户反馈
- 修改 `SystemPromptEnhancer`：从 registry 按 `context.intent` 拉模板

**涉及文件**：
- 新建 `prompt/PromptTemplateRegistry.java`、`PromptVariantService.java`
- 新建 `resources/prompts/{approval,qa,creation,summary}.yml`
- 修改 `agent/enhancer/SystemPromptEnhancer.java`
- 新建表 `ai_prompt_variant`：`id / scenario / variant / template_hash / hit_count / avg_tokens / feedback_score`

**关键决策**：
- 模板格式：YAML（和 Skill 一致），frontmatter 描述场景、正文含 Few-shot
- A/B 分桶：`Math.abs(sessionId.hashCode()) % variantCount`，稳定可复现
- 默认不启用对抗测试（10% 对抗样本），避免拖慢迭代

---

### 8. 长期记忆（跨会话用户偏好）

**现状**：`ai_chat_history` 按 session 隔离，用户跨会话的偏好（如"总是用中文回答"、"偏好简洁"、"正在做 XX 项目"）无法保留。

**目标**：
- 新建表 `ai_user_memory`：`id / user_id / memory_type(preference/fact/project) / content / source_session_id / confidence / created_at / deleted`
- 新建 `memory/LongTermMemoryService.java`：
  - `extract(userId, sessionId)`：LLM 从会话中抽取用户偏好/事实
  - `recall(userId, topK)`：按 user_id 查询，可选用 embedding 相似度排序
  - `decay()`：定期任务，低 confidence + 长期未命中的记忆降权
- 新建 `agent/enhancer/LongTermMemoryEnhancer.java`（order=-1.5）：before 注入用户记忆到 system prompt，after 触发异步抽取

**涉及文件**：
- 新建 DDL `yuca-app/src/main/resources/table/ai/ai_user_memory.sql`
- 新建 `entity/AiUserMemory.java` + `mapper/AiUserMemoryMapper.java`
- 新建 `memory/LongTermMemoryService.java` + `agent/enhancer/LongTermMemoryEnhancer.java`
- 修改 `AgentFactory.defaultAgent`：注入 LongTermMemoryEnhancer

**关键决策**：
- 抽取时机：每 N 轮（N=10）触发一次，而非每轮
- 抽取模型：`qwen-turbo`（便宜快）
- 隐私：记忆只对本人可见，跨用户严格隔离

---

### 9. 护栏链 + PII 脱敏

**现状**：增强器链只做"输入侧增强"（before）和"副作用"（after），没有输出侧治理。身份证、手机号、银行卡号等 PII 直接原样返回。简历"核心工作7"明确提及双侧治理。

**目标**：
- 新建 `guardrail/Guardrail.java` 接口：`String checkOutput(ChatResponse, ChatContext)`，返回脱敏后的 response 或抛 `BusinessException`
- 实现 3 个 Guardrail：
  - `PiiMaskGuardrail`：正则识别身份证（18 位）、手机号（11 位）、银行卡（16-19 位），替换为 `***`
  - `HallucinationGuardrail`：RAG 场景下校验答案中的关键事实能否在 retrieved chunks 中找到（简单 string contains，可选 LLM 复核）
  - `ForbiddenContentGuardrail`：关键词黑名单 + 可选调云厂商内容安全 API
- 修改 `Agent.execute`：before 链后增加 `inputGuardrails`，after 链前增加 `outputGuardrails`（对称结构）
- 新建 `agent/enhancer/GuardrailChain.java`：装配 guardrail 链，order 与 enhancer 对称

**涉及文件**：
- 新建 `guardrail/Guardrail.java`、`PiiMaskGuardrail.java`、`HallucinationGuardrail.java`、`ForbiddenContentGuardrail.java`
- 修改 `agent/Agent.java`：execute 增加 input/output guardrail 节点
- 修改 `AgentFactory.defaultAgent`：注册默认 guardrail 链
- 配置项：`yuca.ai.guardrail.pii.enabled / hallucination.enabled / forbidden.enabled`

**关键决策**：
- PII 阈值：严格模式（正则匹配即脱敏），可配置白名单（如测试环境不脱敏）
- 防幻觉：首次只做 chunk contains 校验（召回率优先，精确率随迭代调），可选 LLM 复核阈值 < 0.5 的样本
- 拦截动作：日志记录 + 替换输出（不直接拒绝，避免用户体验差）

**范围说明**：
- 不含 bias/toxicity 分类模型（需外部 API，成本高）
- 不含对抗提示词检测（prompt injection defense）

---

## P2 - 工程化与运维

### 10. OTEL 全链路追踪 + Thought 采集

**现状**：仅有 `log.info/debug` 散落各处，无法回答"这次回答慢在哪一步"、"这次幻觉是检索还是 LLM 的问题"。另外模型的 `reasoning_content`（思考链 / Thought）字段从未被采集，`AssistantMessage.thinkingContent` 永远是 null——调试和评测都缺这块上下文。

**目标**：
- 引入 `io.opentelemetry:opentelemetry-sdk` + `opentelemetry-spring-boot-starter`
- 把 `Agent.execute` 的每个节点（enhancer before / LLM call / tool exec / enhancer after / guardrail）包装为 span
- 每个用户请求生成 `traceId`，SSE 发送到前端，前端展示时关联
- Dashboard：Grafana / Tempo 展示决策路径树
- **Thought 采集**（从 P0-#2 并入）：修改 `QwenStreamingChatModel` / `QwenChatModel` 解析 `reasoning_content` 字段，写入 `AiMessage.thinkingContent` → Agent 每轮 LLM span 记录 Thought 属性 → SSE 可选推送给前端展示思考过程（类似 DeepSeek）

**涉及文件**：
- 新增 `pom.xml` 依赖（`yuca-ai` 模块）
- 新建 `trace/AgentTracer.java`：封装 span 创建/属性记录
- 修改 `agent/Agent.java`：每个关键步骤包 `tracer.span("...", () -> ...)`，每轮 LLM 调用 span 记录 `thought` 属性
- 修改各 Enhancer：before/after 内记录属性（hit/miss、tokens、score）
- 修改 `core/provider/qwen/QwenChatModel.java` + `QwenStreamingChatModel.java`：解析 `reasoning_content` 填入 `AiMessage.thinkingContent`
- 修改 `AssistantService`：流式回调中透传 thought 到 SSE
- 修改 `dto/sse/SseEvent.java`：新增 `SseThoughtEvent`
- 配置 `OTEL_EXPORTER_OTLP_ENDPOINT` 环境变量

**关键决策**：
- 采样率：默认 100%（流量不大），未来切 tail-based sampling
- Span 粒度：enhancer 一级、tool 一级，不下钻到 prompt 内容（避免泄漏）
- Thought 入库：建议入 `ai_chat_history` 新增 `message_type=THOUGHT`（审计/评测/调试都需要，不限于 span 生命周期）
- Thought 前端展示：默认关闭（隐私 + token 成本），`yuca.ai.thought.expose-to-user=true` 才推送 SSE

---

### 11. Agent 评测体系 + CI 回归门禁

**现状**：所有改动靠手动点击 `/api/ai/test/chat` 验证。Prompt 改动影响无法量化。

**目标**：
- 新建 `yuca-ai-eval` 测试 sourceSet（或独立模块）：
  - 黄金评测集：`resources/eval/golden.jsonl`（200+ 用例：70% 正样本 / 20% 边界 / 10% 对抗）
  - `EvalRunner.java`：批量跑用例 → 调 `LLM-as-Judge` 评分（思维链 + 分数）
  - RAG 专用指标：忠实度（faithfulness）、相关性（relevance）、幻觉率
- CI 集成：GitHub Actions / Gitea Actions 在 PR 触发时跑 eval，对比基线分数
- 分数下降 ≥ 5% 自动拦截（CI 失败）

**涉及文件**：
- 新建 `eval/EvalRunner.java`、`LlmAsJudge.java`、`EvalMetric.java`
- 新建 `resources/eval/golden.jsonl`
- 新建 `.github/workflows/eval.yml`
- 新建 `eval/baseline.json`（上次通过的基线分数）

**关键决策**：
- Judge 模型：用比生成模型更强的（如 qwen-max 评 qwen-turbo 生成）
- 评测范围：仅 defaultAgent + RAG 路径，simpleAgent 不评
- 对抗样本来源：手工 + LLM 生成（DGF / red-teaming）

---

### 12. 分层限流体系

**现状**：无任何限流。单个用户可以无限调 chat 接口刷爆 LLM 配额。

**目标**：
- 三层串联限流：
  1. **固定窗口**（用户维度）：Redis `INCR user:{uid}:window:{minute}`，超 N 拒绝
  2. **漏桶**（全局维度）：Guava `RateLimiter` 控制放行节奏（如 50 QPS）
  3. **Redis Sorted Set**（缓冲层）：过载请求按时间戳入 ZSet，工作线程按时间戳拉取，超时 5s 降级返回兜底回答
- 新建 `ratelimit/RateLimiterChain.java`：串起三层
- 修改 `AssistantService.processChat`：入口处先过 RateLimiterChain

**涉及文件**：
- 新建 `ratelimit/FixedWindowLimiter.java`、`LeakyBucketLimiter.java`、`RedisZsetBuffer.java`、`RateLimiterChain.java`
- 修改 `AssistantService.processChat`、`AssistantController` 入口
- 配置项：`yuca.ai.ratelimit.user-qpm / global-qps / buffer-timeout`

---

### 13. SSE 快慢通道线程池隔离

**现状**：`AsyncConfig` 配置了单个线程池，慢请求（长文档 RAG、复杂工具链）会占用线程导致快请求被拖累（"传染"效应）。

**目标**：
- 拆分两个线程池：
  - `fastExecutor`：核心 10 / 最大 50，处理简单 chat（无 RAG / 单轮）
  - `slowExecutor`：核心 2 / 最大 10，处理 RAG / 多轮工具调用
- 路由策略：`AssistantService` 根据 `request.useRag / request.enableTools` 选择 executor
- 队列独立，慢队列满时直接拒绝（快通道不受影响）

**涉及文件**：
- 修改 `config/AsyncConfig.java`：拆分两个 Executor Bean
- 修改 `AssistantController.processChat`：`@Async("fastExecutor")` 或动态选择
- 新增配置：`yuca.async.fast.* / yuca.async.slow.*`

---

## 待办优先级建议

**P0 必做（面试硬伤）**：
- **#5 AiTestController 硬编码 Key**（🟡 代码已完成；用户待吊销旧 Key + 可选清理 git 历史）
- **#1 Plan 模式**（2-3 天，简历"核心工作2"整段描述）
- **#4 语义缓存**（2 天，简历"核心工作5"整段描述）
- **#3 MCP 集成**（2-3 天，简历"核心工作7"+"职业技能"双重提及；顺手合并 #2 工具结果回塞规范化）
- ~~**#2 ReAct**~~（取消：native tool calling 已等价实现，子任务拆分并入 #3 和 #10）

**P1 中期（1-2 周，支撑简历亮点）**：
- #6 ES + IK + 元数据过滤（简历核心工作 3）
- #7 Prompt 工程（简历核心工作 4）
- #8 长期记忆（简历核心工作 4 后半段）
- #9 护栏 + PII（简历核心工作 7）

**P2 长期（工程化，加分项）**：
- #10 OTEL、#11 评测体系、#12 限流、#13 SSE 线程池——简历最后两个"核心工作"。

---

## 实施约束

- 所有 `Agent.execute` 的改动必须保持 `Agent` 类的接口不变，下游 `AssistantService` / `AiClient` 无感
- 新增 LLM 调用（长期记忆抽取、语义缓存写、评测 Judge）一律走 `AgentFactory.simpleAgent(modelName)`，不直接 new `QwenChatModel`
- 新增表统一放 `yuca-app/src/main/resources/table/ai/` 子目录（按 CLAUDE.md 约定）
- 新增增强器严格按 `order()` 排序，正反序执行（洋葱模型），不得破坏现有顺序（Summary=-2 / Intent=-1 / History=0 / Rag=1）
- 所有 PII / Key / 敏感配置走 `application-local.yml`（已加入 `.gitignore`），禁止入 git
- 涉及外部服务（ES / OTEL Collector / MCP server）的改动需在 `application.yml` 留开关，默认关闭，本地起服务时手动开
