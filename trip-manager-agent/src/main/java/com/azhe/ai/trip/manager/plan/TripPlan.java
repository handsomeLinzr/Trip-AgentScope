package com.azhe.ai.trip.manager.plan;

import io.agentscope.core.plan.PlanNotebook;

/**
 * @author linzherong
 * @date 2026/7/28 12:32
 */
public class TripPlan {

    public PlanNotebook getPlan() {
        return PlanNotebook.builder()
                .needUserConfirm(false)   // 计划需要确认
                .maxSubtasks(5)  // 最大的分解步骤数
                .build();
    }

}
