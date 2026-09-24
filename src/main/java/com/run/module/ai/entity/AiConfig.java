package com.run.module.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 服务接入配置（单行表，id 固定为 1）。
 * 密钥仅存服务端，接口只回传掩码。
 */
@Data
@TableName("t_ai_config")
public class AiConfig {

    /** 单行固定 id = 1 */
    @TableId(type = IdType.INPUT)
    private Long id;

    /** OpenAI 兼容接口地址，如 https://api.deepseek.com/chat/completions */
    private String baseUrl;

    /** API 密钥（明文仅存库，不回传前端） */
    private String apiKey;

    /** 模型名，如 deepseek-chat */
    private String model;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
