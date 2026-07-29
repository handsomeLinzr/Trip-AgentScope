package com.azhe.ai.commons.utils;

import com.azhe.ai.commons.configuration.EnvConfiguration;
import com.azhe.ai.commons.domain.TripAgentWrapper;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.InMemoryMemory;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author linzherong
 * @date 2026/7/30 00:42
 */
@Component
public class TripAgentWrapperUtils {

    // 用于创建会话智能体的构建工具。
    @Resource
    public AgentBuilderUtils agentBuilderUtils;

    // 单次清理扫描的缓存比例。
    private static final int CLEANUP_BATCH_DIVISOR = 4;

    // 空闲会话的过期时长。
    private static final int SESSION_EXPIRE_MINUTES = 10;

    // 保存各会话智能体的并发容器。
    private final ConcurrentHashMap<String, TripAgentWrapper> wrapperMap = new ConcurrentHashMap<>(256);

    // 防止多个请求同时发起重复的缓存清理任务。
    private final AtomicBoolean cleaning = new AtomicBoolean(false);

    /**
     * 通过会话标识获取智能体包装对象。
     *
     * @param sessionId 会话标识
     */
    public TripAgentWrapper getTripAgentWrapper(String sessionId) {

        if (wrapperMap.containsKey(sessionId)) {
            return wrapperMap.get(sessionId);
        }
        TripAgentWrapper tripAgentWrapper = new TripAgentWrapper();
        // 创建智能体
        ReActAgent agent = agentBuilderUtils.getReActAgentBuilder("streamAgent", "个人AI助理")
                // 内存记忆
                .memory(new InMemoryMemory())
                // 系统提示词
                .sysPrompt(
                        """
                        你是一个聪明的AI智能助理，可以根据用户的提问，和用户进行聊天对话。
                        """
                )
                .build();
        tripAgentWrapper.setAgent(agent);
        tripAgentWrapper.setHistory(new ArrayList<>(20));
        tripAgentWrapper.setSessionId(sessionId);
        // 存放到容器
        wrapperMap.put(sessionId, tripAgentWrapper);
        return tripAgentWrapper;
    }

    /**
     * 异步清理超过十分钟未使用的会话智能体。
     */
    public void cleanupExpiredWrappersAsync() {
        // 已有清理任务运行时，不重复提交相同任务。
        if (!cleaning.compareAndSet(false, true)) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                // 清理线程
                cleanupExpiredWrappers();
            } finally {
                // 无论清理任务是否异常，都允许后续请求再次触发清理。
                cleaning.set(false);
            }
        });
    }

    /**
     * 分批清理过期且未使用的会话智能体。
     */
    private void cleanupExpiredWrappers() {
        while (!wrapperMap.isEmpty()) {
            // 当前需要进行惰性清理的数量，全量的 1/4
            int batchSize = Math.max(1, (wrapperMap.size() + CLEANUP_BATCH_DIVISOR - 1) / CLEANUP_BATCH_DIVISOR);
            // 已扫描数
            int scannedCount = 0;
            // 删除数
            int removedCount = 0;
            // 10分钟前的时刻，这个时刻之前且当前未使用的，都需要删除
            LocalDateTime expiredBefore = LocalDateTime.now().minusMinutes(SESSION_EXPIRE_MINUTES);

            for (Map.Entry<String, TripAgentWrapper> entry : wrapperMap.entrySet()) {
                // 已经到达 1/4 了，则停止当前批次的清理
                if (scannedCount >= batchSize) {
                    break;
                }
                // 扫描数+1
                scannedCount++;

                TripAgentWrapper tripAgentWrapper = entry.getValue();
                // 未超过过期时间的会话保留到下一次清理。
                if (!tripAgentWrapper.getLastUsed().isBefore(expiredBefore)) {
                    continue;
                }

                // 通过 CAS 标记清理状态，避免与新请求同时使用同一个智能体。
                if (!tripAgentWrapper.tryStartCleaning()) {
                    continue;
                }

                // 仅移除当前扫描到的包装对象，避免误删已被替换的会话。
                if (wrapperMap.remove(entry.getKey(), tripAgentWrapper)) {
                    removedCount++;
                } else {
                    // 回退使用
                    tripAgentWrapper.stopUsing();
                }
            }

            // 删除数量低于扫描数量的4分之一时，剩余过期数据较少，留待下次异步清理。
            int minimumRemovedCount = Math.max(1,
                    (scannedCount + CLEANUP_BATCH_DIVISOR - 1) / CLEANUP_BATCH_DIVISOR);
            if (scannedCount == 0 || removedCount < minimumRemovedCount) {
                return;
            }
        }
    }

}
