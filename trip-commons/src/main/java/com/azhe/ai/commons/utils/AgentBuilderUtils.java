package com.azhe.ai.commons.utils;

import com.alibaba.nacos.common.utils.StringUtils;
import com.azhe.ai.commons.configuration.EnvConfiguration;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.Event;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.DashScopeChatModel;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * @author linzherong
 * @date 2026/7/28 12:11
 */
@Configuration
public class AgentBuilderUtils {

    @Resource
    public EnvConfiguration env;

    /**
     * 智能体
     * @param name
     * @param description
     * @param model
     * @return
     */
    public ReActAgent.Builder getReActAgentBuilder(String name, String description, String model) {
        return ReActAgent.builder()
                .name(name)
                .description(description)
                .model(DashScopeChatModel.builder()
                        .apiKey(env.getApiKey())
                        .modelName(model)
                        .enableThinking(true)
                        .build()
                );
    }

    public ReActAgent.Builder getReActAgentBuilder(String name, String description) {
        return getReActAgentBuilder(name, description, "qwen3-max");
    }

}
