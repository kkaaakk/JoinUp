package com.joinup.credit.domain;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.joinup.credit.entity.UserCreditRecordEntity;
import com.joinup.credit.enums.CreditChangeTypeEnum;
import com.joinup.credit.enums.CreditRestrictionTypeEnum;
import com.joinup.credit.mapper.UserCreditRecordMapper;
import com.joinup.credit.support.CreditRestrictionDecision;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 信用限制判断器。
 * <p>
 * 这个类负责把“当前分数 + 最近行为记录”翻译成业务限制结果，例如能不能报名热门活动、要不要降低候补优先级。
 * </p>
 */
@Component
public class CreditRestrictionChecker {

    private final UserCreditRecordMapper userCreditRecordMapper;
    private final CreditRuleProperties properties;

    /**
     * 创建信用限制判断器。
     *
     * @param userCreditRecordMapper 信用记录 Mapper
     * @param properties 信用规则配置
     */
    public CreditRestrictionChecker(UserCreditRecordMapper userCreditRecordMapper,
                                    CreditRuleProperties properties) {
        this.userCreditRecordMapper = userCreditRecordMapper;
        this.properties = properties;
    }

    /**
     * 评估用户当前的信用限制状态。
     *
     * @param userId 用户 ID
     * @param creditScore 当前信用分
     * @return 限制判断结果
     */
    public CreditRestrictionDecision evaluate(Long userId, Integer creditScore) {
        List<UserCreditRecordEntity> behaviorRecords = loadRecentBehaviorRecords(userId);
        int consecutiveNoShowCount = calculateConsecutiveNoShowCount(behaviorRecords);
        List<CreditRestrictionTypeEnum> restrictionTypes = new ArrayList<>();

        if (creditScore != null && creditScore < properties.getCreateActivityMinScore()) {
            restrictionTypes.add(CreditRestrictionTypeEnum.BLOCK_ACTIVITY_CREATE);
        }

        if ((creditScore != null && creditScore < properties.getHotActivitySignupMinScore())
                || consecutiveNoShowCount >= properties.getConsecutiveNoShowLimit()) {
            restrictionTypes.add(CreditRestrictionTypeEnum.BLOCK_HOT_ACTIVITY_SIGNUP);
        }

        if ((creditScore != null && creditScore < properties.getWaitlistPenaltyMinScore())
                || consecutiveNoShowCount >= properties.getConsecutiveNoShowLimit()) {
            restrictionTypes.add(CreditRestrictionTypeEnum.DEPRIORITIZE_WAITLIST);
        }

        return CreditRestrictionDecision.builder()
                .userId(userId)
                .creditScore(creditScore)
                .consecutiveNoShowCount(consecutiveNoShowCount)
                .allowCreateActivity(!restrictionTypes.contains(CreditRestrictionTypeEnum.BLOCK_ACTIVITY_CREATE))
                .allowHotActivitySignup(!restrictionTypes.contains(CreditRestrictionTypeEnum.BLOCK_HOT_ACTIVITY_SIGNUP))
                .deprioritizeWaitlist(restrictionTypes.contains(CreditRestrictionTypeEnum.DEPRIORITIZE_WAITLIST))
                .restrictionTypes(Collections.unmodifiableList(restrictionTypes))
                .build();
    }

    /**
     * 加载最近一段时间内与信用限制相关的行为记录。
     *
     * @param userId 用户 ID
     * @return 最近的行为记录列表，按时间倒序排列
     */
    public List<UserCreditRecordEntity> loadRecentBehaviorRecords(Long userId) {
        return userCreditRecordMapper.selectList(Wrappers.<UserCreditRecordEntity>lambdaQuery()
                .eq(UserCreditRecordEntity::getUserId, userId)
                .ge(UserCreditRecordEntity::getCreatedAt,
                        LocalDateTime.now().minusDays(properties.getConsecutiveNoShowWindowDays()))
                .orderByDesc(UserCreditRecordEntity::getCreatedAt)
                .last("limit " + properties.getRecentRecordLimit()));
    }

    /**
     * 计算最近连续爽约次数。
     * <p>
     * 这里的“连续”采用比较务实的判断方式：
     * </p>
     * <p>
     * 1. 按最近信用行为倒序读取。
     * </p>
     * <p>
     * 2. 遇到爽约记录则累计。
     * </p>
     * <p>
     * 3. 遇到守约或取消等正常闭环行为就中断。
     * </p>
     * <p>
     * 4. 人工调整类记录不会打断连续性，因为它不代表真实履约行为。
     * </p>
     *
     * @param records 最近行为记录
     * @return 连续爽约次数
     */
    public int calculateConsecutiveNoShowCount(List<UserCreditRecordEntity> records) {
        if (records == null || records.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (UserCreditRecordEntity record : records) {
            CreditChangeTypeEnum changeType = CreditChangeTypeEnum.fromCode(record.getChangeType());
            if (changeType == null) {
                continue;
            }
            if (changeType == CreditChangeTypeEnum.MANUAL_ADJUST) {
                continue;
            }
            if (changeType == CreditChangeTypeEnum.NO_SHOW_PENALTY) {
                count++;
                continue;
            }
            break;
        }
        return count;
    }
}