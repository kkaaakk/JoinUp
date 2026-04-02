package com.joinup.signup.event;

import com.joinup.signup.enums.SignupCommandTypeEnum;
import com.joinup.signup.enums.SignupReservationTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Kafka command payload for signup and cancel operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignupCommandEvent {

    private String requestId;
    private Long activityId;
    private Long userId;
    private SignupCommandTypeEnum commandType;
    private SignupReservationTypeEnum reservationType;
    private Integer queueNo;
    private String cancelReason;
    private LocalDateTime occurredAt;
}