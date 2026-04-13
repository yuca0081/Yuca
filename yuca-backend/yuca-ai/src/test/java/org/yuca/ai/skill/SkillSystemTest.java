package org.yuca.ai.skill;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.yuca.ai.skill.core.SkillDefinition;
import org.yuca.ai.skill.core.SkillExecutor;
import org.yuca.ai.skill.core.SkillRegistry;

import java.util.List;
import java.util.Map;

/**
 * 技能系统测试
 */
@SpringBootTest
public class SkillSystemTest {

    @Autowired
    private SkillRegistry skillRegistry;

    @Autowired
    private SkillExecutor skillExecutor;

    @Test
    public void testListAllSkills() {
        // 获取所有技能
        List<SkillDefinition> skills = skillRegistry.getAllSkills().stream().toList();

        System.out.println("=== 所有技能 ===");
        skills.forEach(skill -> {
            var metadata = skill.getMetadata();
            System.out.printf("- %s: %s (来源: %s, 可调用: %s)%n",
                    metadata.getName(),
                    metadata.getDescription(),
                    metadata.getSource(),
                    metadata.isUserInvocable()
            );
        });
    }

    @Test
    public void testGetUserInvocableSkills() {
        // 获取用户可调用的技能
        List<SkillDefinition> userSkills = skillRegistry.getUserInvocableSkills();

        System.out.println("=== 用户可调用技能 ===");
        userSkills.forEach(skill -> {
            var metadata = skill.getMetadata();
            System.out.printf("- %s: %s%n",
                    metadata.getName(),
                    metadata.getDescription()
            );
        });
    }

    @Test
    public void testExecuteSkill() {
        // 执行调试技能
        var context = new SkillExecutor.DefaultSkillExecutionContext(
                "test-session-123",
                System.getProperty("user.dir")
        );

        var result = skillExecutor.execute(
                "debug",
                "用户报告登录功能异常",
                context
        );

        System.out.println("=== 技能执行结果 ===");
        System.out.println("成功: " + result.success());
        System.out.println("消息数量: " + result.messages().size());
        System.out.println("允许工具: " + result.allowedTools());

        if (!result.success()) {
            System.out.println("错误: " + result.errorMessage());
        }
    }

    @Test
    public void testBatchExecuteSkills() {
        // 批量执行多个技能
        var context = new SkillExecutor.DefaultSkillExecutionContext(
                "test-session-456",
                System.getProperty("user.dir")
        );

        Map<String, String> skillArgs = Map.of(
                "debug", "检查日志错误",
                "code-review", "审查 UserController"
        );

        var results = skillExecutor.executeBatch(skillArgs, context);

        System.out.println("=== 批量执行结果 ===");
        results.forEach((skillName, result) -> {
            System.out.printf("%s: %s (消息: %d)%n",
                    skillName,
                    result.success() ? "成功" : "失败",
                    result.messages().size()
            );
        });
    }

    @Test
    public void testSkillStatistics() {
        // 获取技能统计信息
        var stats = skillRegistry.getStatistics();

        System.out.println("=== 技能统计 ===");
        stats.forEach((key, value) -> System.out.println(key + ": " + value));
    }

    @Test
    public void testGetSkillDetail() {
        // 获取特定技能的详情
        skillRegistry.getSkill("debug").ifPresent(skill -> {
            var metadata = skill.getMetadata();
            System.out.println("=== 技能详情: " + metadata.getName() + " ===");
            System.out.println("描述: " + metadata.getDescription());
            System.out.println("使用场景: " + metadata.getWhenToUse());
            System.out.println("参数提示: " + metadata.getArgumentHint());
            System.out.println("允许工具: " + metadata.getAllowedTools());
            System.out.println("版本: " + metadata.getVersion());
        });
    }
}
