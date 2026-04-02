package com.joinup.common.result;

import com.joinup.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 统一接口返回结构。
 * controller 层只返回 Result，方便前端和网关统一处理成功/失败语义。
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    /** 业务状态码。 */
    private Integer code;
    /** 对外提示信息。 */
    private String message;
    /** 真实业务载荷。 */
    private T data;

    public static <T> Result<T> success(T data) {
        return Result.<T>builder()
                .code(ErrorCode.SUCCESS.getCode())
                .message(ErrorCode.SUCCESS.getMessage())
                .data(data)
                .build();
    }

    public static Result<Void> success() {
        return success(null);
    }

    public static <T> Result<T> fail(ErrorCode errorCode) {
        // 默认复用错误码内置文案，特殊场景再传自定义 message。
        return fail(errorCode, errorCode.getMessage());
    }

    public static <T> Result<T> fail(ErrorCode errorCode, String message) {
        return Result.<T>builder()
                .code(errorCode.getCode())
                .message(message)
                .build();
    }
}
