package com.run.module.ai.dto;

import lombok.Data;

/** AI 接入配置保存/测试入参（全部可选，空白字段保持原值） */
@Data
public class AiConfigDTO {

    /** OpenAI 兼容接口地址 */
    private String baseUrl;

    /** API 密钥；留空表示不修改已配置的密钥 */
    private String apiKey;

    /** 模型名 */
    private String model;
}
