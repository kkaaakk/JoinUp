package com.joinup.activity.domain;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 活动截止结算分布式锁服务。
 * <p>
 * 该服务提供两层锁：
 * 1. 扫描锁：保证多实例部署下同一时刻只有一个节点负责扫描待结算活动；
 * 2. 活动锁：保证同一个活动只会被一个线程执行一次结算逻辑。
 * </p>
 */
@Component
public class ActivitySettlementLockService {

    private static final String SCAN_LOCK_KEY = "joinup:activity:settlement:scan:lock";
    private static final String ACTIVITY_LOCK_KEY_PREFIX = "joinup:activity:settlement:activity:lock:";

    private final RedissonClient redissonClient;
    private final ActivitySettlementProperties activitySettlementProperties;

    /**
     * 构造活动截止结算分布式锁服务。
     *
     * @param redissonClient Redisson 客户端
     * @param activitySettlementProperties 活动结算配置属性
     */
    public ActivitySettlementLockService(RedissonClient redissonClient,
                                         ActivitySettlementProperties activitySettlementProperties) {
        this.redissonClient = redissonClient;
        this.activitySettlementProperties = activitySettlementProperties;
    }

    /**
     * 在持有全局扫描锁的前提下执行任务。
     *
     * @param task 需要在扫描锁内执行的任务
     * @return 如果成功获得锁并执行任务，则返回 {@code true}
     */
    public boolean executeWithScanLock(Runnable task) {
        return executeWithLock(
                SCAN_LOCK_KEY,
                activitySettlementProperties.getScanLockWaitSeconds(),
                activitySettlementProperties.getScanLockLeaseSeconds(),
                task);
    }

    /**
     * 在持有活动级锁的前提下执行任务。
     *
     * @param activityId 活动 ID
     * @param task 需要在活动锁内执行的任务
     * @return 如果成功获得锁并执行任务，则返回 {@code true}
     */
    public boolean executeWithActivityLock(Long activityId, Runnable task) {
        return executeWithLock(
                ACTIVITY_LOCK_KEY_PREFIX + activityId,
                0L,
                activitySettlementProperties.getActivityLockLeaseSeconds(),
                task);
    }

    /**
     * 统一执行分布式锁模板逻辑。
     *
     * @param lockKey 锁 Key
     * @param waitSeconds 等待锁的秒数
     * @param leaseSeconds 锁自动释放秒数
     * @param task 需要执行的任务
     * @return 如果成功获得锁并执行任务，则返回 {@code true}
     */
    private boolean executeWithLock(String lockKey, long waitSeconds, long leaseSeconds, Runnable task) {
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = false;
        try {
            locked = lock.tryLock(waitSeconds, leaseSeconds, TimeUnit.SECONDS);
            if (!locked) {
                return false;
            }
            task.run();
            return true;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return false;
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}