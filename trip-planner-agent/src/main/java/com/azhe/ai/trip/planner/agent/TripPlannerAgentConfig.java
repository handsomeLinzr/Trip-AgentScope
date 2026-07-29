package com.azhe.ai.trip.planner.agent;

import com.azhe.ai.commons.utils.AgentBuilderUtils;
import io.agentscope.core.ReActAgent;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
    public ReActAgent tripPlannerAgent() {
        return agentBuilderUtils.getReActAgentBuilder(
                "tripPlannerAgent",
                "旅游行程规划智能体",
                        "deepseek-v4-pro")
                .sysPrompt(
                        """
                       你是一个专业的旅游旅程规划师，根据用户问题进行旅程规划。
                       """
                )
                .build();
    }


}
