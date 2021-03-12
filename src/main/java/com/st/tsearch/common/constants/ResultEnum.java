package com.st.tsearch.common.constants;

import lombok.Getter;

@Getter
public enum ResultEnum {

    MEANINGLESS(-1, "无意义")
    , SUCCESS(1, "成功")
    , FAILURE(0, "失败")
    , SYSTEM(10, "系统级别失败")
    , SYSTEM_NODE_INIT(11, "节点初始化失败")
    , SYSTEM_NODE_STATE(12, "节点信息缺失")
    , PARAMETER_CHECK(21, "参数校验失败")
    , REMOTE_QUERY(31, "远程请求失败")
    , ILLEGAL_METHOD(41, "不合理的方法使用")
    ;

    private final int code;
    private final String message;

    ResultEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public static ResultEnum getByCode(int code) {
        for (ResultEnum value : ResultEnum.values()) {
            if (code == value.getCode()) {
                return value;
            }
        }
        return MEANINGLESS;
    }
}
