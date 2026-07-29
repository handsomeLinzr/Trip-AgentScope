package com.azhe.ai.trip.manager.agent;

import com.azhe.ai.commons.utils.AgentBuilderUtils;
import com.azhe.ai.commons.utils.ResponseUtils;
import com.azhe.ai.trip.manager.hook.TripHook;
import com.azhe.ai.trip.manager.plan.TripPlan;
import com.azhe.ai.trip.manager.tools.RemoteAgentTool;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.Event;
import io.agentscope.core.agent.EventType;
import io.agentscope.core.hook.Hook;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.plan.PlanNotebook;
import io.agentscope.core.tool.Toolkit;
import jakarta.annotation.Resource;
import org.reactivestreams.Publisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.function.Function;

/**
 * @author linzherong
 * @date 2026/7/28 01:14
 */
@Configuration
public class ManagerAgentConfiguration {

    @Resource
    private AgentBuilderUtils agentBuilderUtils;

    // 计划
    private PlanNotebook planNotebook = new TripPlan().getPlan();
    // 回调事件拦截器
    private Hook hook = new TripHook();

    /**
     * Create a manager agent that coordinates travel planning and route-making subtasks.
     *
     *
     *  无论用户的问题多么简单，每次都必须创建并调用以下两个子任务：
     * 1. trip-planner-agent：负责行程规划，包括旅行天数安排、景点、餐饮、住宿和注意事项。
     * 2. route-making-agent：负责路线规划，包括往返交通、市内通勤、线路顺序、预计耗时和换乘建议。
     * 执行规则：
     * - 用户提示词只描述旅程需求；不得要求用户指定子 Agent、任务拆分方式或输出格式。
     * - 必须先调用 trip-planner-agent，再调用 route-making-agent；不得跳过任一子 Agent。
     * - 每个子任务都必须明确写出调用的 Agent 名称及其负责内容。
     * - 仅在两个子任务均完成后，才能输出整合后的旅行建议。
     * 最终回答必须严格按以下结构组织：
     * 1. 【子任务 1｜调用 Agent：trip-planner-agent】说明其规划结果。
     * 2. 【子任务 2｜调用 Agent：route-making-agent】说明其路线结果。
     * 3. 【主管整合】输出可直接执行的完整旅程方案。
     */
    @Bean
    public ReActAgent managerAgent(RemoteAgentTool remoteAgentTool) {

        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(remoteAgentTool);

        return agentBuilderUtils.getReActAgentBuilder("managerAgent", "主管Agent，擅长对问题进行拆分并分发给子Agent")
                .sysPrompt(
                        """
                        你是主管旅行智能体，负责接收用户的旅程问题、拆分任务、协调子 Agent，并整合最终答案。
                        ## 你不需要有语言大模型的记忆能力，请根据当前的传入的对话进行回答即可
                        """
                )
                .planNotebook(planNotebook)
                .hook(hook)
                .toolkit(toolkit)
                .build();
    }


    @Bean
    public ReActAgent assistant(RemoteAgentTool remoteAgentTool) {

        return agentBuilderUtils.getReActAgentBuilder("assistant", "擅长进行思考和智能对话")
                .sysPrompt(
                        """
                        你是一个AI智能助手
                        """
                )
                .build();
    }


    /**
     * Execute a travel request through the manager agent.
     *
     * @param prompt travel question submitted by the user
     */
    public void run(String prompt) {
//        ResponseUtils.responseAgentStream(agent, prompt)
//                .doOnNext(event -> System.out.println(event.getMessage().getTextContent()))
//                .onErrorResume(new Function<Throwable, Publisher<? extends Event>>() {
//                    @Override
//                    public Publisher<? extends Event> apply(Throwable throwable) {
//                        return Flux.just(new Event(EventType.AGENT_RESULT, Msg.builder()
//                                .content(List.of(TextBlock.builder()
//                                        .text(throwable.getMessage()).build()))
//                                .build(), true));
//                    }
//                }).blockLast();
    }

}
