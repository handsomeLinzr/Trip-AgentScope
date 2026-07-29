package com.azhe.ai.trip.manager.controller;

import com.azhe.ai.commons.utils.ResponseUtils;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.Event;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SynchronousSink;

import java.util.function.BiConsumer;

/**
 * @author linzherong
 * @date 2026/7/29 13:28
 */
@RestController
@RequestMapping("/chat")
public class ChatController {

    @Resource
    @Qualifier("managerAgent")
    private ReActAgent managerAgent;


    @GetMapping("/trip")
    public Flux<String> getTrip(@RequestParam("prompt") String prompt, HttpServletResponse response) {
        response.setCharacterEncoding("utf-8");
        return ResponseUtils.responseAgentStream(managerAgent, prompt)
                .<String>handle((event, sink) -> sink.next(event.getMessage().getTextContent()));
    }

}
