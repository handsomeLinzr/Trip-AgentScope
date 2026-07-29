package com.azhe.ai.commons.utils;

import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.ai.AiFactory;
import com.alibaba.nacos.api.ai.AiService;
import com.alibaba.nacos.api.exception.NacosException;
import com.azhe.ai.commons.configuration.EnvConfiguration;
import io.agentscope.core.nacos.a2a.discovery.NacosAgentCardResolver;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.Properties;

/**
 * @author linzherong
 * @date 2026/7/29 12:57
 */
@Component
public class NacosUtils {

    @Resource
    public EnvConfiguration env;

    private volatile NacosAgentCardResolver nacosCard;

    // 获取 nacos 上对应的智能体卡片
    public NacosAgentCardResolver getNacosCard() {
        if (nacosCard == null) {
            synchronized (this) {
                if (nacosCard == null) {

                    // 设置 Nacos 地址
                    Properties properties = new Properties();
                    properties.put(PropertyKeyConst.SERVER_ADDR, env.getNacosUrl());
                    // 创建 Nacos Client
                    AiService aiService = null;
                    try {
                        aiService = AiFactory.createAiService(properties);
                    } catch (NacosException e) {
                        throw new RuntimeException(e);
                    }
                    nacosCard = new NacosAgentCardResolver(aiService);
                }
            }
        }
        return nacosCard;
    }
}
