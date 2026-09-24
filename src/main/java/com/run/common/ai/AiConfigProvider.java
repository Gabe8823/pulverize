package com.run.common.ai;

/**
 * AI 接入配置提供者（依赖倒置：common 只定义契约，由 module/ai 落地实现）。
 *
 * 三者均返回"当前生效值"：界面「AI 服务接入」保存的 t_ai_config 优先，
 * 空白字段回退配置文件 ai.api.*。每次调用实时解析，保存后立即生效。
 */
public interface AiConfigProvider {

    /** 生效的接口地址（可能为空字符串） */
    String url();

    /** 生效的 API 密钥（可能为空字符串或占位符 sk-placeholder） */
    String key();

    /** 生效的模型名 */
    String model();
}
