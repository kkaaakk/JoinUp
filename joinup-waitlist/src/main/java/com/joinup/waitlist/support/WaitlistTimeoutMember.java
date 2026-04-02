package com.joinup.waitlist.support;

import lombok.Builder;
import lombok.Getter;
import org.springframework.util.StringUtils;

/**
 * 候补补位超时索引成员。
 * <p>
 * Redis 中的全局超时索引使用 "activityId:userId" 作为成员值，
 * 这里负责把该字符串和强类型对象互相转换。
 * </p>
 */
@Getter
@Builder
public class WaitlistTimeoutMember {

    private final Long activityId;
    private final Long userId;

    /**
     * 将对象序列化为 Redis 成员值。
     *
     * @return 形如 "activityId:userId" 的字符串
     */
    public String serialize() {
        return activityId + ":" + userId;
    }

    /**
     * 将 Redis 成员值反序列化为对象。
     *
     * @param raw Redis 中的原始成员值
     * @return 解析后的对象；若原始值为空则返回 {@code null}
     */
    public static WaitlistTimeoutMember parse(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String[] parts = raw.split(":", -1);
        if (parts.length != 2) {
            return null;
        }
        return WaitlistTimeoutMember.builder()
                .activityId(Long.parseLong(parts[0]))
                .userId(Long.parseLong(parts[1]))
                .build();
    }
}
