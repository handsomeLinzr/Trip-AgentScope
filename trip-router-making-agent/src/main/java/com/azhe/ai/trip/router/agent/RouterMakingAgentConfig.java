package com.azhe.ai.trip.router.agent;

import com.azhe.ai.commons.configuration.EnvConfiguration;
import com.azhe.ai.commons.hook.PrintHook;
import com.azhe.ai.commons.utils.AgentBuilderUtils;
import com.azhe.ai.trip.router.mcp.GaodeMapMCP;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.plan.PlanNotebook;
import io.agentscope.core.tool.Toolkit;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author linzherong
 * @date 2026/7/29 01:33
 */
@Configuration
@Slf4j
public class RouterMakingAgentConfig {

    @Resource
    private AgentBuilderUtils agentBuilderUtils;


    @Bean
    public ReActAgent routerMakingAgent(EnvConfiguration env) {

        GaodeMapMCP gaodeMcp = new GaodeMapMCP();
        Toolkit toolkit = new Toolkit();
        log.info("==================高德地图mcp工具注册中=======================");
        toolkit.registerMcpClient(gaodeMcp.getInstance(env.getGaodeMcpUrl())).block();
        log.info("==================高德地图mcp工具注册完成=======================");

        return agentBuilderUtils.getReActAgentBuilder(
                "routerMakingAgent",
                "擅长处理自驾游路线制定",
                        "deepseek-v4-pro")
                .toolkit(toolkit)
                .hook(new PrintHook())
                .build();
    }

}
