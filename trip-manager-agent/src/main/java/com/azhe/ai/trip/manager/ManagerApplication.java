package com.azhe.ai.trip.manager;

import com.azhe.ai.commons.configuration.EnvConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author linzherong
 * @date 2026/7/28 00:22
 */
@SpringBootApplication(scanBasePackages = {"com.azhe.ai.commons","com.azhe.ai.trip.manager"})
public class ManagerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ManagerApplication.class, args).getBean(EnvConfiguration.class);
    }

}
