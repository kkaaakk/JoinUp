package com.joinup.waitlist.support;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.redis.core.ZSetOperations;

/**
 * 候补队列成员视图。
 * <p>
 * 该对象主要用于把 Redis ZSet 中的成员信息转换成强类型对象，便于候补模块做队头选择与排名展示。
 * </p>
 */
@Getter
@Builder
public class WaitlistQueueMember {

    private final Long userId;
    private final Integer queueNo;

    /**
     * 把 Redis ZSet 元组转换为候补队列成员对象。
     *
     * @param tuple Redis ZSet 元组
     * @return 解析后的候补队列成员；如果元组为空则返回 {@code null}
     */
    public static WaitlistQueueMember fromTuple(ZSetOperations.TypedTuple<String> tuple) {
        if (tuple == null || tuple.getValue() == null) {
            return null;
        }
        return WaitlistQueueMember.builder()
                .userId(Long.parseLong(tuple.getValue()))
                .queueNo(tuple.getScore() == null ? 0 : tuple.getScore().intValue())
                .build();
    }
}
