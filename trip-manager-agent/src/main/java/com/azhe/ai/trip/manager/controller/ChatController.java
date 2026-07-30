package com.azhe.ai.trip.manager.controller;

import com.alibaba.nacos.common.utils.StringUtils;
import com.azhe.ai.commons.domain.TripAgentWrapper;
import com.azhe.ai.commons.response.ResponseSchema;
import com.azhe.ai.commons.utils.ResponseUtils;
import com.azhe.ai.commons.utils.TripAgentWrapperUtils;
import com.azhe.ai.trip.manager.hook.TripHook;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.Event;
import io.agentscope.core.agent.EventType;
import io.agentscope.core.hook.Hook;
import io.agentscope.core.message.*;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SignalType;
import reactor.core.publisher.Sinks;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;


/**
 * @author linzherong
 * @date 2026/7/29 13:28
 */
@Slf4j
@RestController
@RequestMapping("/chat")
public class ChatController {

    @Resource
    @Qualifier("managerAgent")
    private ReActAgent managerAgent;

    @Resource
    @Qualifier("assistant")
    private ReActAgent assistant;

    @Resource
    private TripAgentWrapperUtils tripAgentWrapperUtils;

    // 记忆管理
    private final ConcurrentHashMap<String, List<Msg>> memoryMap = new ConcurrentHashMap<>(256);

    // 客户端用于续接多轮会话的响应头名称。
    private static final String SESSION_ID_HEADER = "X-Trip-Session-Id";


    /**
     * 旅游行程，记忆管理
     * @param prompt
     * @param response
     * @param sessionId
     * @return
     */
    @GetMapping("/trip")
    public ResponseSchema getTrip(@RequestParam("prompt") String prompt,
                                HttpServletResponse response,
                                @RequestParam(value = "sessionId", required = false) String sessionId) {
        response.setCharacterEncoding("utf-8");

        // 随机id
        if (StringUtils.isBlank(sessionId)) {
            sessionId = UUID.randomUUID().toString();
        }

        // 将会话 ID 返回给客户端，前端可用它发起下一轮请求。
        response.setHeader(SESSION_ID_HEADER, sessionId);
        response.setHeader("Access-Control-Expose-Headers", SESSION_ID_HEADER);

        List<Msg> msgs = memoryMap.computeIfAbsent(sessionId, s -> new ArrayList<>(10));
        // 构建历史+现在提示词
        List<Msg> out = buildMessages(prompt, msgs);
        // 思考过程是通过 hook 获取事件
        Sinks.Many<String> sinks = Sinks.many().unicast().onBackpressureBuffer();
        for (Hook hook : managerAgent.getHooks()) {
            if (hook instanceof TripHook tripHook) {
                tripHook.setCallback(sinks::tryEmitNext);
            }
        }
        // 响应内容
//        Flux<String> result = ResponseUtils.responseAgentStream(managerAgent, out)
//                .mapNotNull(new Function<Event, String>() {
//                    boolean thinking = true;
//                    @Override
//                    public String apply(Event event) {
//                        if (event.getType().equals(EventType.REASONING) && !event.isLast()) {
//                            ContentBlock contentBlock = event.getMessage().getContent().get(0);
//                            // 思考过程
//                            if (contentBlock instanceof ThinkingBlock thinkingBlock) {
//                                String resp = thinkingBlock.getThinking();
//                                if (thinking) {
//                                    thinking = false;
//                                    resp = "(思考中：**" + resp;
//                                }
//                                return resp;
//                            } else if (contentBlock instanceof TextBlock textBlock){
//                                // 返回
//                                String resp = textBlock.getText();
//                                if (!thinking) {
//                                    thinking = true;
//                                    resp = "** 思考结束)" + resp;
//                                }
//                                return resp;
//                            }
//                        } else if (event.getType().equals(EventType.AGENT_RESULT) && event.isLast()) {
//                            // 保存记忆
//                            msgs.add(event.getMessage());
//                        }
//                        return null;
//                    }
//                })
//                .filter(StringUtils::isNotBlank);


        Msg call = ResponseUtils.call(managerAgent, prompt, ResponseSchema.class);
        ResponseSchema responseSchema = call.getStructuredData(ResponseSchema.class);

//        return Flux.merge(sinks.asFlux(), result);
        return responseSchema;
    }

    /**
     * 单 Agent
     * @param prompt
     * @param response
     * @return
     */
    @GetMapping("/call")
    public Flux<String> call(@RequestParam("prompt") String prompt,
                                HttpServletResponse response) {
        response.setCharacterEncoding("utf-8");
        return ResponseUtils.responseAgentStream(assistant, prompt)
                .map(event -> event.getMessage().getTextContent())
                .filter(StringUtils::isNotBlank);
    }


    /**
     * 并行多 Agent
     * @param prompt
     * @param response
     * @return
     */
    @GetMapping("/stream")
    public Flux<String> stream(@RequestParam("prompt") String prompt,
                               HttpServletResponse response,
                               @RequestParam(value = "sessionId", required = false) String sessionId) {
        // 首次对话，初始化 sessionId
        if (StringUtils.isBlank(sessionId)) {
            sessionId = UUID.randomUUID().toString();
        }
        log.info("=====================》》》》》》=======================");
        log.info("{}:{}", sessionId, prompt);
        log.info("=====================》》》》》》=======================");
        // 设置编码
        response.setCharacterEncoding("utf-8");
        // 将会话 ID 返回给客户端，前端可用它发起下一轮请求。
        response.setHeader(SESSION_ID_HEADER, sessionId);
        response.setHeader("Access-Control-Expose-Headers", SESSION_ID_HEADER);

        // 异步回收已过期的空闲会话，避免缓存持续增长。
        tripAgentWrapperUtils.cleanupExpiredWrappersAsync();

        // 原子地获取并占用会话专属智能体，工具类会自动避开正在清理的旧对象。
        Optional<TripAgentWrapper> tripAgentWrapperOptional = tripAgentWrapperUtils.acquireTripAgentWrapper(sessionId);
        if (tripAgentWrapperOptional.isEmpty()) {
            // 只有被其他用户请求占用时，才提示调用端等待。
            return Flux.just("当前智能体正在运行中，请等待结束后再提问！");
        }

        // 固定本次请求成功占用的包装对象，供响应流的回调安全引用。
        TripAgentWrapper activeTripAgentWrapper = tripAgentWrapperOptional.get();
        // 构建历史对话
        List<Msg> msgs = buildMessages(prompt, activeTripAgentWrapper.getHistory());
        return ResponseUtils.responseAgentStream(activeTripAgentWrapper.getAgent(), msgs)
                // 打印输出
                .doOnNext(event -> log.info(event.getMessage().getTextContent()))
                .map(event -> {
                    if (event.getType().equals(EventType.AGENT_RESULT) && event.isLast()) {
                        activeTripAgentWrapper.getHistory().add(event.getMessage());
                    }
                    return event.getMessage().getTextContent();
                })
                // 流结束、取消或出错时，更新使用时间并释放会话占用。
                .doFinally(signalType -> {
                    activeTripAgentWrapper.updateLastUsed();
                    activeTripAgentWrapper.stopUsing();
                })
                .filter(StringUtils::isNotBlank);
    }


    /**
     * 构建并裁剪会话历史记录。
     *
     * @param prompt 当前用户输入
     * @param msgs 当前会话的消息列表
     */
    private List<Msg> buildMessages(String prompt, List<Msg> msgs) {
        if (msgs.size() < 10) {
            msgs.add(Msg.builder().content(List.of(TextBlock.builder().text(prompt).build())).build());
            return msgs;
        }

        // 去掉最前边的一轮对话
        boolean find = false;
        List<Msg> out = new ArrayList<>(10);
        for (Msg msg : msgs) {
            if (!find && msg.getRole() != MsgRole.USER) {
                find = true;
                continue;
            }

            // 首轮对话结束后，保留后续的全部历史消息。
            if (find) {
                out.add(msg);
            }
        }
        out.add(Msg.builder()
                .role(MsgRole.USER).content(List.of(TextBlock.builder()
                        .text(prompt)
                        .build()))
                .build());

        // 保持会话缓存中的列表引用不变，并用裁剪后的记录覆盖其内容。
        msgs.clear();
        msgs.addAll(out);
        return msgs;
    }

}
