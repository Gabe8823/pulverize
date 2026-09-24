package com.run.module.ai.dto;

import lombok.Data;

/** AI 接入配置视图（密钥只回传掩码，绝不回传明文） */
@Data
public class AiConfigVO {

    /** 生效的接口地址 */
    private String baseUrl;

    /** 生效的模型名 */
    private String model;

    /** 是否已配置可用密钥 */
    private boolean keySet;

    /** 密钥掩码，如 sk-a1****z9f2；未配置为 null */
    private String keyMasked;

    /** 配置来源：DB=界面配置 YML=配置文件 NONE=未配置 */
    private String source;
}
