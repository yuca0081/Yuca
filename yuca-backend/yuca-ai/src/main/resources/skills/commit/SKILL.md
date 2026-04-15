---
name: commit
description: 分析代码变更并生成规范的 git commit message
when_to_use: 当用户要求提交代码或生成 commit message 时
arguments:
  - message
argument-hint: "<message> 可选的提交信息提示"
---

请根据当前的代码变更生成一个规范的 git commit message。

$ARGUMENTS

## 要求

1. 分析代码变更内容
2. 生成符合 Conventional Commits 规范的提交信息
3. 格式：`type(scope): description`
4. type 包括：feat, fix, refactor, docs, style, test, chore
5. 描述使用中文，简洁明了地说明变更内容

## 输出格式

```
<type>(<scope>): <简要描述>

<详细说明（可选）>
```
