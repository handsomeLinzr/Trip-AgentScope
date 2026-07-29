package com.azhe.ai.commons.configuration;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * @author linzherong
 * @date 2026/7/29 13:02
 */
@Configuration
@Data
public class EnvConfiguration {

    @Value("${DASH_SCOPE_API_KEY}")
    private String apiKey;

    @Value("${NACOS_URL}")
    private String nacosUrl;

}
