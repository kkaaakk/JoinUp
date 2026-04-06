package com.joinup.signup.support;

import com.joinup.signup.enums.SignupReservationTypeEnum;
import com.joinup.signup.enums.SignupResultStatusEnum;
import lombok.Builder;
import lombok.Getter;
import org.springframework.util.StringUtils;

/**
 * Redis 中保存的轻量级报名状态对象。
 * <p>
 * 该对象只保留高并发报名链路最关键的最小状态集合，用于：
 * 1. 防止重复点击重复扣减名额；
 * 2. 在异步落库未完成前快速返回处理状态；
 * 3. 在结果查询接口中优先给前端即时反馈。
 * </p>
 */
@Getter
@Builder
public class SignupCachedState {

    private static final String SEPARATOR = "|";

    private final SignupResultStatusEnum status;
    private final SignupReservationTypeEnum reservationType;
    private final String requestId;
    private final Integer queueNo;

    /**
     * 将当前状态序列化为紧凑字符串。
     * <p>
     * 这里没有使用 JSON，而是采用固定分隔符格式，目的是减少 Redis 存储体积和序列化开销。
     * </p>
     *
     * @return 可直接写入 Redis 的字符串
     */
    public String serialize() {
        return safe(status == null ? null : status.getCode())
                + SEPARATOR
                + safe(reservationType == null ? null : reservationType.getCode())
                + SEPARATOR
                + safe(requestId)
                + SEPARATOR
                + safe(queueNo == null ? null : String.valueOf(queueNo));
    }

    /**
     * 把 Redis 中的字符串反序列化为状态对象。
     *
     * @param raw Redis 中保存的原始字符串
     * @return 解析后的状态对象；如果原始值为空，则返回 {@code null}
     */
    public static SignupCachedState parse(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String[] parts = raw.split("\\|", -1);
        return SignupCachedState.builder()
                .status(SignupResultStatusEnum.fromCode(parts.length > 0 ? parts[0] : null))
                .reservationType(SignupReservationTypeEnum.fromCode(parts.length > 1 ? parts[1] : null))
                .requestId(parts.length > 2 && StringUtils.hasText(parts[2]) ? parts[2] : null)
                .queueNo(parts.length > 3 && StringUtils.hasText(parts[3]) ? Integer.parseInt(parts[3]) : null)
                .build();
    }

    /**
     * 把可空字符串安全转为非空值。
     *
     * @param value 原始字符串
     * @return 非空字符串；为空时返回空串
     */
    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
