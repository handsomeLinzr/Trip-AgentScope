package com.azhe.ai.trip.manager.tools;

import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.ai.AiFactory;
import com.alibaba.nacos.api.ai.AiService;
import com.alibaba.nacos.api.exception.NacosException;
import com.azhe.ai.commons.utils.MsgUtils;
import com.azhe.ai.commons.utils.NacosUtils;
import com.azhe.ai.commons.utils.ResponseUtils;
import io.agentscope.core.a2a.agent.A2aAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.nacos.a2a.discovery.NacosAgentCardResolver;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Properties;

/**
 * @author linzherong
 * @date 2026/7/28 16:10
 */
@Slf4j
@Service
public class RemoteAgentTool {

    private A2aAgent tripPannerAgent;
    private final Object tripPannerAgentLock = new Object();
    private A2aAgent routerMakingAgent;
    private final Object routerMakingAgentLock = new Object();

    @Resource
    private NacosUtils nacosUtils;

    @Tool(description = "旅程规划专家，擅长根据起点和终点制定旅游行程的Agent")
    public String callTripPannerAgent() throws NacosException {

        log.info("============");
        log.info("工具方法：路线制定智能体...正在调用中");
        log.info("============");

        if (tripPannerAgent == null) {
            synchronized (tripPannerAgentLock) {
                if (tripPannerAgent == null) {
                    // 创建 A2A Agent
                    tripPannerAgent = A2aAgent.builder()
                            .name("tripPlannerAgent")
                            .agentCardResolver(nacosUtils.getNacosCard())
                            .build();
                }
            }
        }

        // 创建 A2A Agent
        A2aAgent agent = A2aAgent.builder()
                .name("tripPlannerAgent")
                .agentCardResolver(nacosUtils.getNacosCard())
                .build();

        // 调用远程 Agent
        Msg response = agent.call(Msg.builder().role(MsgRole.USER).content(List.of(TextBlock.builder().text("你好").build())).build()).block();
        return null;
    }


    @Tool(description = "路线制定专家，擅长制定最优路线规划的Agent")
    public String callRouterMakingAgent(
            @ToolParam(name = "prompt", description = "路线的起点和终点") String prompt) throws NacosException {

        log.info("======callRouterMakingAgent 提示词：========》》{}", prompt);

        String newPrompt = "请根据给定的起点和终点：**"+prompt+"**，制定对应的路线规划";

        if (routerMakingAgent == null) {
            synchronized (routerMakingAgentLock) {
                if (routerMakingAgent == null) {
                    routerMakingAgent = A2aAgent.builder()
                            .name("routerMakingAgent")
                            .agentCardResolver(nacosUtils.getNacosCard())
                            .build();
                }
            }
        }

        // 调用
        return ResponseUtils.call(routerMakingAgent, newPrompt);
    }


}
