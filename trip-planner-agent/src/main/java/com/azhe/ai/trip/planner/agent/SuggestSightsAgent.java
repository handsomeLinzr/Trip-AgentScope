package com.azhe.ai.trip.planner.agent;

import com.azhe.ai.commons.hook.PrintHook;
import com.azhe.ai.commons.mcp.McpBuilder;
import com.azhe.ai.commons.utils.AgentBuilderUtils;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.skill.AgentSkill;
import io.agentscope.core.skill.SkillBox;
import io.agentscope.core.skill.util.JarSkillRepositoryAdapter;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.core.tool.mcp.McpClientWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

/**
 * 旅行规划智能体
 * @author linzherong
 * @date 2026/7/30 15:16
 */
@Slf4j
//@Component
public class SuggestSightsAgent {

    /**
     * 创建绑定景点推荐技能和美团 MCP 工具的智能体。
     *
     * @param agentBuilderUtils 智能体构建工具
     */
//    @Bean("suggestAgent")
    public ReActAgent suggestAgent(AgentBuilderUtils agentBuilderUtils) {
        try (JarSkillRepositoryAdapter skillRepositoryAdapter = new JarSkillRepositoryAdapter("skills")) {
            // 一次读取 resources/skills 下的全部技能，打包为 JAR 后同样可用。
            List<AgentSkill> skills = skillRepositoryAdapter.getAllSkills();
            Toolkit toolkit = new Toolkit();
            SkillBox skillBox = new SkillBox(toolkit);
            log.info("======================美团 MCP==========================");
            McpClientWrapper meituanMCP = McpBuilder.getInstance(
                    "https://mcp.api-inference.modelscope.net/4ae9ecbbfcfa4c/sse", "meituan-mcp");

            registerSkills(skillBox, skills, meituanMCP);

            return agentBuilderUtils.getReActAgentBuilder(
                    "suggestAgent",
                            "擅长进行旅游规划",
                            "qwen3-32b")
                    .skillBox(skillBox)
                    .toolkit(toolkit)
                    .hook(new PrintHook())
                    //你是一个专业的旅游规划专家，根据用户的提问，进行旅游行程的规划。
                    //                            处理旅行规划请求前，先从 Available Skills 中找到 Suggest-Sights，调用
                    //                            load_skill_through_path 加载其 SKILL.md；随后严格遵循该技能中的工作流程和回答原则。
                    //                            需要实时景点、餐饮或住宿信息时，调用加载该技能后启用的 MCP 工具。
                    .sysPrompt(
                            """
                            你是一个专业的旅游规划专家，根据用户的提问，进行旅游行程的规划，
                            优先加载合适的 SKILL.md，随后严格遵循该技能中的工作流程和回答原则。
                            如果没有合适的 SKILL.md，再根据你的理解进行回答。
                            你的输出规划，必须包含旅程、餐厅、住宿、景点。
                            """
                    )
                    .build();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 注册全部技能，并将美团 MCP 工具绑定到景点推荐技能。
     *
     * @param skillBox 技能容器
     * @param skills 待注册的技能列表
     * @param meituanMCP 美团 MCP 客户端
     */
    private void registerSkills(SkillBox skillBox, List<AgentSkill> skills, McpClientWrapper meituanMCP) {
        for (AgentSkill skill : skills) {
            // 景点推荐技能加载后才激活美团 MCP 工具，其他技能仅注册对应的技能文档。
            if ("Suggest-Sights".equals(skill.getName())) {
                skillBox.registration()
                        .skill(skill)
                        .mcpClient(meituanMCP)
                        .apply();
                continue;
            }
            skillBox.registration()
                    .skill(skill)
                    .apply();
        }
    }

}
