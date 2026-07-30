package com.azhe.ai.trip.planner.agent;

import com.azhe.ai.commons.hook.PrintHook;
import com.azhe.ai.commons.utils.AgentBuilderUtils;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.Agent;
import io.agentscope.core.skill.AgentSkill;
import io.agentscope.core.skill.SkillBox;
import io.agentscope.core.skill.SkillHook;
import io.agentscope.core.skill.util.JarSkillRepositoryAdapter;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.core.tool.subagent.SubAgentConfig;
import io.agentscope.core.tool.subagent.SubAgentProvider;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.List;

/**
 * 路程规划智能体
 * @author linzherong
 * @date 2026/7/28 12:07
 */
@Configuration
public class TripPlannerAgentConfig {

//    private ReActAgent agent;
//
//    private ReActAgent.Builder agentBuilder;

    @Resource
    private AgentBuilderUtils agentBuilderUtils;

    /*
    public TripPlannerAgent() {
        agentBuilder = AgentBuilderUtils.getReActAgentBuilder(
                        "TripPlannerAgent",
                        "擅长对旅程行程景点进行规划",
                        "deepseek-v4")
                .sysPrompt(
                        """
                        你是一个专业的旅游旅程规划师，根据用户问题进行旅程规划。
                        """
                );
        agent = agentBuilder.build();

        //=========== 手动写入注册中心，项目不用种方式 START ====

//        //行程规划Agent 智能体卡片
//        ConfigurableAgentCard agentCard =  new ConfigurableAgentCard.Builder()
//                .name("TripPlannerAgent")
//                .description("行程规划Agent")
//                .build();
//
//        //将智能体卡片写入到AgentScope自带的注册中心
//        AgentScopeA2aServer.builder(builder)
//                .agentCard(agentCard)
//                .deploymentProperties(
//                       new DeploymentProperties(
//                               "localhost",
//                               8080)
//                )
//                .build();

        //还需要AgentScopeA2aServer启动

        // 创建 A2A Server
        AgentScopeA2aServer server = AgentScopeA2aServer.builder(agentBuilder)
                .deploymentProperties(
                        new DeploymentProperties("localhost", 8848)
                )
                .build();

        // Agent 卡片
        ConfigurableAgentCard agentCard = new ConfigurableAgentCard.Builder()
                .name("TripPlannerAgent")
                .description("行程规划智能体")
                .version("1.0.0")
                .build();

        // 注册
        AgentScopeA2aServer.builder(agentBuilder)
                .agentCard(agentCard)
                .build();

        //======== 手动写入注册中心，项目不用种方式 END ====

    }
    */
    @Bean
    public ReActAgent tripPlannerAgent() throws IOException {

        // 注册景点推荐子智能体，并提交注册以生成可供父智能体调用的工具。
        Toolkit toolkit = new Toolkit();
        toolkit.registration()
                .subAgent(() -> new SuggestSightsAgent().suggestAgent(agentBuilderUtils),
                        SubAgentConfig.builder()
                                .toolName("suggest_sights")
                            .description("景点推荐专家：根据目的地、出行日期、同行人群和偏好，推荐景点并说明游玩建议。")
                            .build())
                .apply();

        return agentBuilderUtils.getReActAgentBuilder(
                "tripPlannerAgent",
                "旅游行程规划智能体",
                        "qwen3-32b")
                .sysPrompt(
                        """
                       你是一个专业的旅游旅程规划师，根据用户问题进行旅程规划，包括景点、美食、住宿、路线等。
                       当用户需要景点推荐或景点游玩建议时，优先调用 suggest_sights 子智能体。
                       """
                )
                .toolkit(toolkit)
                .hook(new PrintHook())
                .build();
    }


}
