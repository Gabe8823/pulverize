package com.run.module.platform.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CorosLoginRequest {

    @NotBlank(message = "高驰邮箱不能为空")
    private String email;

    @NotBlank(message = "高驰密码不能为空")
    private String password;

    /** 区域: cn / eu / us，默认 cn */
    private String region = "cn";
}
