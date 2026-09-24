package com.run.module.platform.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 通用平台绑定请求（账号密码型平台，如高驰）
 */
@Data
public class PlatformBindRequest {

    /** 账号（邮箱/手机号） */
    @NotBlank(message = "账号不能为空")
    private String account;

    /** 密码 */
    @NotBlank(message = "密码不能为空")
    private String password;

    /** 区域: cn / eu / us，默认 cn */
    private String region = "cn";
}
