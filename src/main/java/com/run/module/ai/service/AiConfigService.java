package com.run.module.ai.service;

import com.run.common.ai.AiConfigProvider;
import com.run.module.ai.dto.AiConfigVO;

/**
 * AI 服务接入配置：读取/保存 t_ai_config，并提供当前生效的 url/key/model。
 * 保存后对下一次 AI 调用立即生效，无需重启后端。
 */
public interface AiConfigService extends AiConfigProvider {

    /** 当前生效配置（密钥仅掩码回传） */
    AiConfigVO describe();

    /** 保存接入配置；空白字段保持原值。首次保存前，空白字段取配置文件默认值 */
    AiConfigVO save(String baseUrl, String apiKey, String model);
}
