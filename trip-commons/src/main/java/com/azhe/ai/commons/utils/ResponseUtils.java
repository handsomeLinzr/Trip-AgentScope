package com.azhe.ai.commons.utils;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.AgentBase;
import io.agentscope.core.agent.Event;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * @author linzherong
 * @date 2026/7/29 13:24
 */
public class ResponseUtils {

    private ResponseUtils() {}

    /**
     * 同步调用返回结果
     * @param agent
     * @param prompt
     * @return
     */
    public static String call(AgentBase agent, String prompt) {
        Msg msg = agent.call(MsgUtils.buildText(prompt)).block();
        return msg.getTextContent();
    }

    /**
     * agent流式响应
     * @param agent
     * @param prompt
     */
    public static Flux<Event> responseAgentStream(ReActAgent agent, String prompt) {
        return agent.stream(
                Msg.builder()
                        .role(MsgRole.USER)
                        .content(
                                List.of(TextBlock.builder()
                                        .text(prompt)
                                        .build()
                                )
                        )
                        .build()
        );
    }

    /**
     * agent流式响应
     * @param agent
     * @param msgs
     */
    public static Flux<Event> responseAgentStream(ReActAgent agent, List<Msg> msgs) {
        return agent.stream(msgs);
    }

}
