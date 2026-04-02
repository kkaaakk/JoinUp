package com.joinup.notify.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 通知分页查询请求。
 */
@Data
public class NotifyPageQuery {

    /** 当前页码，默认从 1 开始。 */
    @Min(value = 1, message = "页码不能小于 1")
    private long pageNum = 1;

    /** 每页大小，避免一次拉取过多消息。 */
    @Min(value = 1, message = "每页大小不能小于 1")
    @Max(value = 100, message = "每页大小不能超过 100")
    private long pageSize = 10;

    /** 可选的消息状态过滤条件。 */
    private Integer status;

    /** 可选的消息类型过滤条件。 */
    private Integer messageType;

    /** 是否只查看未读消息。 */
    private Boolean unreadOnly;
}