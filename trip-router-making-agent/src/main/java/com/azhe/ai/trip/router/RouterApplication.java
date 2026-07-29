package com.azhe.ai.trip.router;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author linzherong
 * @date 2026/7/28 00:20
 */
@SpringBootApplication(scanBasePackages = {"com.azhe.ai.commons","com.azhe.ai.trip.router"})
public class RouterApplication {

    public static void main(String[] args) {
        SpringApplication.run(RouterApplication.class, args);
    }

}
