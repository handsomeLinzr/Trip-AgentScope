package com.azhe.ai.commons.hook;

import io.agentscope.core.hook.*;
import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.ThinkingBlock;
import io.agentscope.core.message.ToolUseBlock;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * @author linzherong
 * @date 2026/7/28 12:36
 */
@Slf4j
public class PrintHook implements Hook {

    /* **********************
     *
     * Hook 是对 HookEvent事件 的拦截
     * HookEvent事件：
     *
     * PreReasoningEvent：用户的输入事件
     * PostReasoningEvent: Agent推理思考过程事件
     * PreActingEvent： Agent执行过程准备调用工具的事件
     * PostActingEvent：Agent执行过程调用工具完成的事件
     *
     *
     * *********************/
    @Override
    public <T extends HookEvent> Mono<T> onEvent(T event) {
        switch (event) {
            case PreReasoningEvent e -> {
                // 用户输入事件
                log.info("####### 用户输入，用户的prompt #######");
                log.info(e.getInputMessages().get(0).getTextContent());
            }
            case ReasoningChunkEvent e ->  {
                // 思考过程
                List<ContentBlock> content = e.getIncrementalChunk().getContent();
                ContentBlock contentBlock = content.get(0);
                if (contentBlock instanceof ThinkingBlock thinkingBlock) {
                    System.out.println(thinkingBlock.getThinking());
                } else if (contentBlock instanceof ToolUseBlock toolUseBlock) {
                    if (toolUseBlock.getName().equals("__fragment__")) {
                        System.out.println(toolUseBlock.getContent());
                    } else {
                        System.out.println("调用工具：" + toolUseBlock.getName());
                    }
                }

            }
            case PreActingEvent e -> {
                log.info("####### Agent执行过程准备调用工具的事件 #######");
                log.info("工具名：{}", e.getToolUse().getName());
                log.info("工具内容：{}：", e.getToolUse().getContent());
            }

            case PostActingEvent e -> {
                log.info("####### Agent执行过程调用工具完成的事件 #######");
                log.info("工具名：{}", e.getToolResult().getName());
                log.info("工具调用结果：{}：", e.getToolResultMsg().getTextContent());
            }

            default -> {
                // 其他事件处理
            }
        }

        // 需要返回事件回去
        return Mono.just(event);
    }
}
