package com.st.tsearch.exception;

import com.st.tsearch.common.constants.ResultEnum;
import lombok.Data;

@Data
public class TSearchException extends RuntimeException {

    private Integer code;

    public TSearchException(ResultEnum resultEnum) {
        super(resultEnum.getMessage());
        this.code = resultEnum.getCode();
    }

    public TSearchException(ResultEnum resultEnum, String message) {
        super(message);
        this.code = resultEnum.getCode();
    }

    public TSearchException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}