package com.azhe.ai.commons.mcp;

import io.agentscope.core.tool.mcp.McpClientBuilder;
import io.agentscope.core.tool.mcp.McpClientWrapper;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mcp创建工具
 * @author linzherong
 * @date 2026/7/30 15:34
 */
@Slf4j
public class McpBuilder {

    private static ConcurrentHashMap<String, McpClientWrapper> map = new ConcurrentHashMap<>(256);

    public static McpClientWrapper getInstance(String mcpUrl, String name) {
        if (map.containsKey(mcpUrl)) {
            return map.get(mcpUrl);
        }
        synchronized (McpBuilder.class) {
            if (!map.containsKey(mcpUrl)) {
                McpClientWrapper mcp = McpClientBuilder.create(name)
                        .sseTransport(mcpUrl)
                        .buildAsync()
                        .timeout(Duration.ofSeconds(120))
                        .block();
                mcp.initialize().block();

                log.info("==============MCP列出所有工具========================");
                mcp.listTools().block().forEach(tool -> {
                    log.info(tool.name() + ":" + tool.description());
                });
                log.info("==============MCP列出所有工具完成========================");

                map.put(mcpUrl, mcp);
            }
        }
        return map.get(mcpUrl);
    }

}
