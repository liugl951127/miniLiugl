package com.minimax.common.exception;

import com.minimax.common.result.ResultCode;
import lombok.Getter;

import java.io.Serial;

/**
 * 业务异常
 */
@Getter
public class BizException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Integer code;

    public BizException(String message) {
        super(message);
        this.code = ResultCode.FAIL.getCode();
    }

    public BizException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public BizException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public BizException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }
}
