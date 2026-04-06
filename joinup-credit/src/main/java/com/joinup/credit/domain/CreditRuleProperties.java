package com.joinup.credit.domain;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 信用规则配置。
 * <p>
 * 信用模块的阈值变化会比较频繁，适合通过配置项托管，避免每次运营调整都改代码。
 * 当前先把最核心的分值和限制阈值收拢到这里，后续可以进一步拆成更细的规则组。
 * </p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "joinup.credit")
public class CreditRuleProperties {

    private Integer initialScore = 100;
    private Integer minScore = 0;
    private Integer maxScore = 200;
    private Integer attendBonus = 1;
    private Integer earlyCancelPenalty = 0;
    private Integer lateCancelPenalty = -3;
    private Integer noShowPenalty = -10;
    private Integer freeCancelHoursBeforeStart = 24;
    private Integer hotActivityHeatScoreThreshold = 80;
    private Integer hotActivitySignupMinScore = 70;
    private Integer createActivityMinScore = 60;
    private Integer waitlistPenaltyMinScore = 75;
    private Integer consecutiveNoShowLimit = 2;
    private Integer consecutiveNoShowWindowDays = 90;
    private Integer recentRecordLimit = 10;
    private Integer manualAdjustMaxAbsoluteDelta = 50;
}