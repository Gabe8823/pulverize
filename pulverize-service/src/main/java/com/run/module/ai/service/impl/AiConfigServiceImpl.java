package com.run.module.ai.service.impl;

import com.run.common.exception.BizException;
import com.run.module.ai.dto.AiConfigVO;
import com.run.module.ai.entity.AiConfig;
import com.run.module.ai.mapper.AiConfigMapper;
import com.run.module.ai.service.AiConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * AI 接入配置实现：
 * 生效值 = t_ai_config（界面配置）优先，空白字段回退 application-*.yml 的 ai.api.*。
 * 每次读取都做单行主键点查，保存后对下一次 AI 调用立即生效。
 * 表不存在或查询异常时回退配置文件值，绝不因配置问题中断业务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiConfigServiceImpl implements AiConfigService {

    /** 配置单行固定 id */
    private static final long CONFIG_ID = 1L;

    @Value("${ai.api.url:}")
    private String ymlUrl;

    @Value("${ai.api.key:}")
    private String ymlKey;

    @Value("${ai.api.model:deepseek-chat}")
    private String ymlModel;

    private final AiConfigMapper configMapper;

    @Override
    public String url() {
        AiConfig c = dbConfig();
        return pick(c == null ? null : c.getBaseUrl(), ymlUrl);
    }

    @Override
    public String key() {
        AiConfig c = dbConfig();
        return pick(c == null ? null : c.getApiKey(), ymlKey);
    }

    @Override
    public String model() {
        AiConfig c = dbConfig();
        return pick(c == null ? null : c.getModel(), ymlModel);
    }

    @Override
    public AiConfigVO describe() {
        AiConfig c = dbConfig();
        String dbKey = c == null ? null : c.getApiKey();
        String url = pick(c == null ? null : c.getBaseUrl(), ymlUrl);
        String model = pick(c == null ? null : c.getModel(), ymlModel);
        String key = pick(dbKey, ymlKey);

        boolean keyFromDb = usableKey(dbKey);
        boolean usable = !isBlank(url) && (keyFromDb || usableKey(ymlKey));

        AiConfigVO vo = new AiConfigVO();
        vo.setBaseUrl(url);
        vo.setModel(model);
        vo.setKeySet(usable);
        vo.setKeyMasked(usable ? mask(key) : null);
        vo.setSource(usable ? (keyFromDb ? "DB" : "YML") : "NONE");
        return vo;
    }

    @Override
    public AiConfigVO save(String baseUrl, String apiKey, String model) {
        AiConfig c = dbConfig();
        boolean isNew = c == null;
        if (isNew) {
            c = new AiConfig();
            c.setId(CONFIG_ID);
            // 首次创建：空白字段以配置文件为初值
            c.setBaseUrl(ymlUrl);
            c.setModel(ymlModel);
            c.setApiKey(ymlKey);
        }
        if (!isBlank(baseUrl)) {
            String url = baseUrl.trim();
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                throw new BizException("接口地址必须以 http:// 或 https:// 开头");
            }
            c.setBaseUrl(url);
        }
        if (!isBlank(model)) {
            c.setModel(model.trim());
        }
        if (!isBlank(apiKey)) {
            c.setApiKey(apiKey.trim());
        }
        if (isNew) {
            configMapper.insert(c);
        } else {
            configMapper.updateById(c);
        }
        log.info("AI 接入配置已保存: url={}, model={}, key={}", c.getBaseUrl(), c.getModel(),
                mask(c.getApiKey()));
        return describe();
    }

    /** 读取单行配置；表缺失/查询异常返回 null（绝不向调用方抛出，自动回退 yml） */
    private AiConfig dbConfig() {
        try {
            return configMapper.selectById(CONFIG_ID);
        } catch (Exception e) {
            log.debug("读取 t_ai_config 失败，回退配置文件: {}", e.getMessage());
            return null;
        }
    }

    /** 非空取值：界面配置优先 */
    private String pick(String db, String yml) {
        return !isBlank(db) ? db.trim() : (yml == null ? "" : yml.trim());
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    /** 可用密钥：非空且非占位符 */
    private boolean usableKey(String k) {
        return !isBlank(k) && !k.toLowerCase().contains("placeholder");
    }

    /** 掩码：前 4 位 + **** + 后 4 位；过短只显示 **** */
    private String mask(String k) {
        if (isBlank(k)) {
            return null;
        }
        String v = k.trim();
        if (v.length() <= 10) {
            return "****";
        }
        return v.substring(0, 4) + "****" + v.substring(v.length() - 4);
    }
}
