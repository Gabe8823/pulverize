package com.run.module.platform.dto;

import lombok.Data;

/**
 * COROS 登录响应
 *
 * 真实返回示例（错误）:
 * {"result":"1009","tlogId":"...","apiCode":"41C2B95C","message":"Request exceptions or parameter errors"}
 * 成功时 result 为 "0000"，并携带 data.accessToken / data.userId。
 * 注意: 高驰没有 code 字段，成功标志是 result == "0000"。
 */
@Data
public class CorosLoginResponse {

    /** 结果码，成功为 "0000"（字符串，非 code 数字） */
    private String result;
    private String message;
    private String apiCode;
    private LoginResult data;

    /** 是否登录成功 */
    public boolean isSuccess() {
        return "0000".equals(result);
    }

    @Data
    public static class LoginResult {
        private String accessToken;
        private String openId;
        private String userId;
        private Integer expiresIn;
    }
}
