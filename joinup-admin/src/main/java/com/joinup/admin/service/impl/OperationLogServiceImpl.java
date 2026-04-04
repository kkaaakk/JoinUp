package com.joinup.admin.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.joinup.admin.constant.AdminPermissionConstants;
import com.joinup.admin.domain.AdminPermissionChecker;
import com.joinup.admin.dto.OperationLogPageQuery;
import com.joinup.admin.entity.OperationLogEntity;
import com.joinup.admin.enums.OperationResultEnum;
import com.joinup.admin.mapper.OperationLogMapper;
import com.joinup.admin.service.OperationLogService;
import com.joinup.admin.vo.OperationLogItemVO;
import com.joinup.admin.vo.OperationLogPageVO;
import com.joinup.common.context.LoginUser;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 操作日志服务实现。
 * <p>
 * 这里把“日志写入”和“日志分页查询”集中收口，避免每个管理后台服务都自己拼装日志实体，
 * 让后台操作审计有统一的格式和落库入口。
 * </p>
 */
@Service
@Validated
public class OperationLogServiceImpl implements OperationLogService {

    private final OperationLogMapper operationLogMapper;
    private final AdminPermissionChecker adminPermissionChecker;

    /**
     * 创建操作日志服务实现。
     *
     * @param operationLogMapper 操作日志 Mapper
     * @param adminPermissionChecker 管理员权限校验器
     */
    public OperationLogServiceImpl(OperationLogMapper operationLogMapper,
                                   AdminPermissionChecker adminPermissionChecker) {
        this.operationLogMapper = operationLogMapper;
        this.adminPermissionChecker = adminPermissionChecker;
    }

    /**
     * 记录一条后台操作日志。
     * <p>
     * 当前先记录最核心的审计字段：操作人、模块、业务对象、结果与补充说明。
     * 如果后续需要接入网关 requestId、用户 IP、浏览器信息等字段，
     * 可以继续在这个统一入口上扩展，而不需要到处改业务代码。
     * </p>
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
    @Override
    public void recordOperation(LoginUser operator,
                                String moduleName,
                                String operationType,
                                String bizType,
                                Long bizId,
                                OperationResultEnum result,
                                String errorMessage,
                                String extraData) {
        OperationLogEntity entity = new OperationLogEntity();
        entity.setOperatorId(operator != null ? operator.getUserId() : null);
        entity.setOperatorRole("ADMIN");
        entity.setModuleName(moduleName);
        entity.setOperationType(operationType);
        entity.setBizType(bizType);
        entity.setBizId(bizId);
        entity.setOperationResult(result != null ? result.getCode() : null);
        entity.setErrorMessage(errorMessage);
        entity.setExtraData(extraData);
        entity.setOperationTime(LocalDateTime.now());
        operationLogMapper.insert(entity);
    }

    /**
     * 分页查询后台操作日志。
     * <p>
     * 这里主要服务于管理后台审计查看场景，因此在进入查询前先校验管理员权限，
     * 避免普通用户借由日志接口间接看到后台治理动作。
     * </p>
     *
     * @param loginUser 当前登录管理员
     * @param query 分页查询参数
     * @return 操作日志分页结果
     */
    @Override
    public OperationLogPageVO pageOperationLogs(LoginUser loginUser, @Valid OperationLogPageQuery query) {
        adminPermissionChecker.assertAdmin(loginUser, AdminPermissionConstants.LOG_VIEW);

        Page<OperationLogEntity> page = operationLogMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()),
                Wrappers.<OperationLogEntity>lambdaQuery()
                        .eq(query.getOperatorId() != null, OperationLogEntity::getOperatorId, query.getOperatorId())
                        .eq(query.getOperationResult() != null, OperationLogEntity::getOperationResult, query.getOperationResult())
                        .eq(StringUtils.hasText(query.getModuleName()), OperationLogEntity::getModuleName, trimToNull(query.getModuleName()))
                        .eq(StringUtils.hasText(query.getOperationType()), OperationLogEntity::getOperationType, trimToNull(query.getOperationType()))
                        .orderByDesc(OperationLogEntity::getOperationTime)
                        .orderByDesc(OperationLogEntity::getId));

        List<OperationLogItemVO> records = page.getRecords().stream()
                .map(this::mapItem)
                .toList();

        return OperationLogPageVO.builder()
                .current(page.getCurrent())
                .size(page.getSize())
                .total(page.getTotal())
                .records(records)
                .build();
    }

    /**
     * 把日志实体转换成分页展示对象。
     *
     * @param entity 日志实体
     * @return 日志视图对象
     */
    private OperationLogItemVO mapItem(OperationLogEntity entity) {
        OperationResultEnum resultEnum = OperationResultEnum.fromCode(entity.getOperationResult());
        return OperationLogItemVO.builder()
                .id(entity.getId())
                .operatorId(entity.getOperatorId())
                .operatorRole(entity.getOperatorRole())
                .moduleName(entity.getModuleName())
                .operationType(entity.getOperationType())
                .bizType(entity.getBizType())
                .bizId(entity.getBizId())
                .operationResult(entity.getOperationResult())
                .operationResultDescription(resultEnum != null ? resultEnum.getDescription() : null)
                .errorMessage(entity.getErrorMessage())
                .extraData(entity.getExtraData())
                .operationTime(entity.getOperationTime())
                .build();
    }

    /**
     * 统一清洗字符串筛选条件。
     *
     * @param value 原始字符串
     * @return 去掉首尾空格后的结果；如果为空白则返回 {@code null}
     */
    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}