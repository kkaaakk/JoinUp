package com.joinup.admin.service;

import com.joinup.admin.dto.OperationLogPageQuery;
import com.joinup.admin.enums.OperationResultEnum;
import com.joinup.admin.vo.OperationLogPageVO;
import com.joinup.common.context.LoginUser;
import jakarta.validation.Valid;

/**
 * 操作日志服务。
 */
public interface OperationLogService {

    /**
     * 记录一条后台操作日志。
     *
     * @param operator 当前操作人
     * @param moduleName 功能模块名
     * @param operationType 操作类型
     * @param bizType 业务对象类型
     * @param bizId 业务对象 ID
     * @param result 操作结果
     * @param errorMessage 错误信息
     * @param extraData 扩展数据
     */
    void recordOperation(LoginUser operator,
                         String moduleName,
                         String operationType,
                         String bizType,
                         Long bizId,
                         OperationResultEnum result,
                         String errorMessage,
                         String extraData);

    /**
     * 分页查询后台操作日志。
     *
     * @param loginUser 当前登录管理员
     * @param query 分页查询参数
     * @return 操作日志分页结果
     */
    OperationLogPageVO pageOperationLogs(LoginUser loginUser, @Valid OperationLogPageQuery query);
}