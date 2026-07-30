package com.azhe.ai.trip.planner;

import com.azhe.ai.commons.utils.ResponseUtils;
import com.azhe.ai.trip.planner.agent.TripPlannerAgentConfig;
import io.agentscope.core.ReActAgent;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * @author linzherong
 * @date 2026/7/28 00:20
 */
@SpringBootApplication(scanBasePackages = {"com.azhe.ai.commons", "com.azhe.ai.trip.planner"})
public class PlannerApplication {

    public static void main(String[] args) {
        SpringApplication.run(PlannerApplication.class, args);
    }

}
