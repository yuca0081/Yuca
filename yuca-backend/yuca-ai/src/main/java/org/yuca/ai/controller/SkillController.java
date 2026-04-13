package org.yuca.ai.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.yuca.ai.skill.core.SkillExecutor;
import org.yuca.ai.skill.core.SkillRegistry;
import org.yuca.common.response.Result;

import java.util.List;
import java.util.Map;

/**
 * 技能控制器
 * 提供技能调用的 REST API
 */
@RestController
@RequestMapping("/api/skills")
@RequiredArgsConstructor
public class SkillController {

    private final SkillRegistry skillRegistry;
    private final SkillExecutor skillExecutor;

    /**
     * 获取所有技能列表
     */
    @GetMapping("/list")
    public Result<List<SkillInfo>> listSkills() {
        var skills = skillRegistry.getAllSkills().stream()
                .map(skill -> new SkillInfo(
                        skill.getMetadata().getName(),
                        skill.getMetadata().getDescription(),
                        skill.getMetadata().isUserInvocable(),
                        skill.getMetadata().getSource()
                ))
                .toList();

        return Result.success(skills);
    }

    /**
     * 获取用户可调用的技能
     */
    @GetMapping("/user-invocable")
    public Result<List<SkillInfo>> getUserInvocableSkills() {
        var skills = skillRegistry.getUserInvocableSkills().stream()
                .map(skill -> new SkillInfo(
                        skill.getMetadata().getName(),
                        skill.getMetadata().getDescription(),
                        skill.getMetadata().isUserInvocable(),
                        skill.getMetadata().getSource()
                ))
                .toList();

        return Result.success(skills);
    }

    /**
     * 获取技能详情
     */
    @GetMapping("/{skillName}")
    public Result<SkillDetail> getSkillDetail(@PathVariable String skillName) {
        return skillRegistry.getSkill(skillName)
                .map(skill -> {
                    var metadata = skill.getMetadata();
                    SkillDetail detail = new SkillDetail(
                            metadata.getName(),
                            metadata.getDescription(),
                            metadata.getWhenToUse(),
                            metadata.getArgumentHint(),
                            metadata.getAllowedTools(),
                            metadata.isUserInvocable(),
                            metadata.getVersion(),
                            metadata.getSource()
                    );
                    return Result.success(detail);
                })
                .orElse(Result.<SkillDetail>failure("技能不存在: " + skillName));
    }

    /**
     * 执行技能
     */
    @PostMapping("/{skillName}/execute")
    public Result<SkillExecutionResult> executeSkill(
            @PathVariable String skillName,
            @RequestBody SkillExecutionRequest request) {

        try {
            // 创建执行上下文
            var context = new SkillExecutor.DefaultSkillExecutionContext(
                    request.getSessionId(),
                    request.getProjectPath()
            );

            // 执行技能
            var result = skillExecutor.execute(
                    skillName,
                    request.getArgs(),
                    context
            );

            return Result.success(new SkillExecutionResult(
                    result.success(),
                    result.errorMessage(),
                    result.messages().size(),
                    result.allowedTools().size()
            ));

        } catch (Exception e) {
            return Result.failure("执行失败: " + e.getMessage());
        }
    }

    /**
     * 批量执行技能
     */
    @PostMapping("/batch-execute")
    public Result<Map<String, SkillExecutionResult>> executeBatch(
            @RequestBody BatchExecutionRequest request) {

        try {
            var context = new SkillExecutor.DefaultSkillExecutionContext(
                    request.getSessionId(),
                    request.getProjectPath()
            );

            var results = skillExecutor.executeBatch(
                    request.getSkillArgs(),
                    context
            );

            // 转换结果格式
            Map<String, SkillExecutionResult> convertedResults = results.entrySet().stream()
                    .collect(java.util.stream.Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> new SkillExecutionResult(
                                    entry.getValue().success(),
                                    entry.getValue().errorMessage(),
                                    entry.getValue().messages().size(),
                                    entry.getValue().allowedTools().size()
                            )
                    ));

            return Result.success(convertedResults);

        } catch (Exception e) {
            return Result.failure("批量执行失败: " + e.getMessage());
        }
    }

    /**
     * 获取技能统计信息
     */
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getStatistics() {
        return Result.success(skillRegistry.getStatistics());
    }

    // DTO 类定义

    public record SkillInfo(
            String name,
            String description,
            boolean userInvocable,
            String source
    ) {}

    public record SkillDetail(
            String name,
            String description,
            String whenToUse,
            String argumentHint,
            java.util.Set<String> allowedTools,
            boolean userInvocable,
            String version,
            String source
    ) {}

    public record SkillExecutionRequest(
            String args,
            String sessionId,
            String projectPath
    ) {}

    public record BatchExecutionRequest(
            Map<String, String> skillArgs,
            String sessionId,
            String projectPath
    ) {}

    public record SkillExecutionResult(
            boolean success,
            String errorMessage,
            int messageCount,
            int allowedToolCount
    ) {}
}
