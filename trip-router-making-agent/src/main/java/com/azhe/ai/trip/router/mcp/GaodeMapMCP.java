package com.azhe.ai.trip.router.mcp;

import io.agentscope.core.tool.mcp.McpClientBuilder;
import io.agentscope.core.tool.mcp.McpClientWrapper;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

/**
 * @author linzherong
 * @date 2026/7/29 12:10
 */
@Slf4j
public class GaodeMapMCP {

    private volatile boolean init = false;
    private McpClientWrapper gaodeMapMCPWrapper;

    // 创建高德地图 mcp
    public McpClientWrapper getInstance(String gaodeMcpUrl) {
        if (init) {
            return gaodeMapMCPWrapper;
        }
        synchronized (this) {
            if (!init) {
                // 注册高德地图MCP
                gaodeMapMCPWrapper = McpClientBuilder.create("gaodeMap-mcp")
                        .sseTransport(gaodeMcpUrl)
                        .buildAsync()
                        .timeout(Duration.ofSeconds(120))
                        .block();

                log.info("==============高德地图MCP初始化中========================");
                // 初始化
                gaodeMapMCPWrapper.initialize().block();
                log.info("==============高德地图MCP初始化完成========================");

                init = true;

                log.info("==============高德地图MCP列出所有工具========================");
                gaodeMapMCPWrapper.listTools().block().forEach(tool -> {
                    log.info(tool.name() + ":" + tool.description());
                });
            }
        }
        return gaodeMapMCPWrapper;
    }

}
