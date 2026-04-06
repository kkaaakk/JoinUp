package com.joinup.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.joinup.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 管理后台操作日志实体。
 * <p>
 * 这张表主要记录后台人工操作的审计轨迹，方便后续排查“谁在什么时候对哪个对象做了什么”。
 * 当前先聚焦后台核心操作，例如举报处理、信用人工调整、异常活动下架等，
 * 后续如有需要，还可以进一步接入请求参数快照、链路追踪 ID、终端来源等字段。
 * </p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("operation_log")
public class OperationLogEntity extends BaseEntity {

    /** 日志主键。 */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 操作人用户 ID。 */
    @TableField("operator_id")
    private Long operatorId;

    /** 操作人角色标识，现阶段通常写 ADMIN。 */
    @TableField("operator_role")
    private String operatorRole;

    /** 所属功能模块，例如 activity_admin / report_admin / credit_admin。 */
    @TableField("module_name")
    private String moduleName;

    /** 操作类型，例如 REVIEW / HANDLE / ADJUST / QUERY。 */
    @TableField("operation_type")
    private String operationType;

    /** 业务对象类型，例如 activity / report / user / credit。 */
    @TableField("biz_type")
    private String bizType;

    /** 业务对象主键。 */
    @TableField("biz_id")
    private Long bizId;

    /** 请求 ID，后续如果接入网关或链路追踪可以透传。 */
    @TableField("request_id")
    private String requestId;

    /** 请求路径。 */
    @TableField("request_path")
    private String requestPath;

    /** 请求方法，例如 GET / POST。 */
    @TableField("request_method")
    private String requestMethod;

    /** 请求来源 IP。 */
    @TableField("request_ip")
    private String requestIp;

    /** 终端 User-Agent。 */
    @TableField("user_agent")
    private String userAgent;

    /** 操作结果，取值见 {@code OperationResultEnum}。 */
    @TableField("operation_result")
    private Integer operationResult;

    /** 失败时记录错误信息，成功时可以为空。 */
    @TableField("error_message")
    private String errorMessage;

    /** 额外扩展信息，现阶段用简单字符串承载，后续可以升级为 JSON。 */
    @TableField("extra_data")
    private String extraData;

    /** 操作发生时间。 */
    @TableField("operation_time")
    private LocalDateTime operationTime;
}