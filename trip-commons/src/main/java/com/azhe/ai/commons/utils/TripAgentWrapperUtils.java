package com.azhe.ai.commons.utils;

import com.azhe.ai.commons.domain.TripAgentWrapper;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.InMemoryMemory;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Optional;

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

    // 独占执行缓存清理任务，避免占用公共线程池。
    private final ExecutorService cleanupExecutor = Executors.newSingleThreadExecutor(
            Thread.ofPlatform().daemon(true).name("trip-agent-cleanup-", 0).factory());

    /**
     * 通过会话标识获取智能体包装对象。
     *
     * @param sessionId 会话标识
     */
    public TripAgentWrapper getTripAgentWrapper(String sessionId) {
        return wrapperMap.computeIfAbsent(sessionId, this::createTripAgentWrapper);
    }

    /**
     * 原子地获取并占用会话对应的智能体。
     *
     * @param sessionId 会话标识
     * @return 已占用的智能体包装对象；会话正被其他请求使用时为空
     */
    public Optional<TripAgentWrapper> acquireTripAgentWrapper(String sessionId) {
        while (true) {
            TripAgentWrapper tripAgentWrapper = getTripAgentWrapper(sessionId);
            // 成功从空闲状态切换到请求使用状态后返回。
            if (tripAgentWrapper.tryStartUsing()) {
                return Optional.of(tripAgentWrapper);
            }
            // 清理任务正在移除旧对象时，重新获取当前会话的包装对象。
            if (tripAgentWrapper.isCleaning()) {
                Thread.onSpinWait();
                continue;
            }
            return Optional.empty();
        }
    }

    /**
     * 创建并初始化会话专属的智能体包装对象。
     *
     * @param sessionId 会话标识
     */
    private TripAgentWrapper createTripAgentWrapper(String sessionId) {
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

        try {
            cleanupExecutor.execute(() -> {
                try {
                    cleanupExpiredWrappers();
                } finally {
                    // 无论清理任务是否异常，都允许后续请求再次触发清理。
                    cleaning.set(false);
                }
            });
        } catch (RejectedExecutionException exception) {
            // 应用关闭期间无法提交任务时，恢复触发标记。
            cleaning.set(false);
        }
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

    /**
     * 关闭专用的会话清理线程。
     */
    @PreDestroy
    public void shutdownCleanupExecutor() {
        cleanupExecutor.shutdown();
    }

}
