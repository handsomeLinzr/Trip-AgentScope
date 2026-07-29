package com.azhe.ai.commons.domain;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author linzherong
 * @date 2026/7/30 00:40
 */
@Data
public class TripAgentWrapper {

    // 会话的唯一标识。
    private String sessionId;

    // 当前会话关联的智能体实例。
    private ReActAgent agent;

    // 当前会话保存的历史消息。
    private List<Msg> history;

    // 控制会话处于空闲、请求使用或缓存清理三种状态之一。
    private final AtomicReference<UsageState> usageState = new AtomicReference<>(UsageState.IDLE);

    // 会话最近一次结束使用的时间。
    private volatile LocalDateTime lastUsed = LocalDateTime.now();

    /**
     * 原子地尝试占用当前会话。
     */
    public boolean tryStartUsing() {
        return usageState.compareAndSet(UsageState.IDLE, UsageState.USING);
    }

    /**
     * 原子地标记当前会话正在被缓存清理任务处理。
     */
    public boolean tryStartCleaning() {
        return usageState.compareAndSet(UsageState.IDLE, UsageState.CLEANING);
    }

    /**
     * 判断当前会话是否正在被缓存清理任务处理。
     */
    public boolean isCleaning() {
        return usageState.get() == UsageState.CLEANING;
    }

    /**
     * 释放当前会话的占用状态。
     */
    public void stopUsing() {
        usageState.set(UsageState.IDLE);
    }

    /**
     * 更新会话最近一次结束使用的时间。
     */
    public void updateLastUsed() {
        this.lastUsed = LocalDateTime.now();
    }

    /**
     * 会话智能体的原子占用状态。
     */
    private enum UsageState {
        // 当前会话可以被请求或清理任务占用。
        IDLE,
        // 当前会话正在处理用户请求。
        USING,
        // 当前会话正在被缓存清理任务移除。
        CLEANING
    }

}
