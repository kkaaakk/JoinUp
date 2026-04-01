package com.joinup.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.joinup.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("operation_log")
public class OperationLogEntity extends BaseEntity {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("operator_id")
    private Long operatorId;

    @TableField("operator_role")
    private String operatorRole;

    @TableField("module_name")
    private String moduleName;

    @TableField("operation_type")
    private String operationType;

    @TableField("biz_type")
    private String bizType;

    @TableField("biz_id")
    private Long bizId;

    @TableField("request_id")
    private String requestId;

    @TableField("request_path")
    private String requestPath;

    @TableField("request_method")
    private String requestMethod;

    @TableField("request_ip")
    private String requestIp;

    @TableField("user_agent")
    private String userAgent;

    @TableField("operation_result")
    private Integer operationResult;

    @TableField("error_message")
    private String errorMessage;

    @TableField("extra_data")
    private String extraData;

    @TableField("operation_time")
    private LocalDateTime operationTime;
}
