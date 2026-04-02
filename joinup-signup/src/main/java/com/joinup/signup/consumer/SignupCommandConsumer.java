package com.joinup.signup.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.joinup.common.exception.BusinessException;
import com.joinup.signup.constant.SignupKafkaConstants;
import com.joinup.signup.domain.SignupActivityCacheService;
import com.joinup.signup.domain.SignupCommandHandler;
import com.joinup.signup.enums.SignupCommandTypeEnum;
import com.joinup.signup.event.SignupCommandEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 报名命令 Kafka 消费者。
 * <p>
 * 该组件负责消费异步命令，并驱动数据库落库与 Redis 状态收敛。
 * 它处在报名高并发链路的后半段，重点解决的是最终一致性问题。
 * </p>
 */
@Component
public class SignupCommandConsumer {

    private final ObjectMapper objectMapper;
    private final SignupCommandHandler signupCommandHandler;
    private final SignupActivityCacheService signupActivityCacheService;

    /**
     * 构造报名命令消费者。
     *
     * @param objectMapper JSON 反序列化工具
     * @param signupCommandHandler 报名命令处理器
     * @param signupActivityCacheService 报名缓存状态服务
     */
    public SignupCommandConsumer(ObjectMapper objectMapper,
                                 SignupCommandHandler signupCommandHandler,
                                 SignupActivityCacheService signupActivityCacheService) {
        this.objectMapper = objectMapper;
        this.signupCommandHandler = signupCommandHandler;
        this.signupActivityCacheService = signupActivityCacheService;
    }

    /**
     * 消费一条报名命令消息。
     * <p>
     * 处理流程为：
     * 1. 反序列化 Kafka 消息；
     * 2. 调用领域处理器执行事务性写库；
     * 3. 若业务处理失败，则按命令类型执行不同的缓存补偿策略。
     * </p>
     *
     * @param payload Kafka 原始消息体
     * @throws Exception 反序列化或处理过程中的异常
     */
    @KafkaListener(
            topics = SignupKafkaConstants.SIGNUP_COMMAND_TOPIC,
            groupId = SignupKafkaConstants.SIGNUP_COMMAND_GROUP,
            containerFactory = "kafkaListenerContainerFactory")
    public void onMessage(String payload) throws Exception {
        SignupCommandEvent event = objectMapper.readValue(payload, SignupCommandEvent.class);
        try {
            signupCommandHandler.handle(event);
        } catch (BusinessException ex) {
            if (event.getCommandType() == SignupCommandTypeEnum.APPLY) {
                signupActivityCacheService.compensateFailedApply(event);
            } else {
                signupActivityCacheService.markCancelFailed(event);
            }
        }
    }
}
