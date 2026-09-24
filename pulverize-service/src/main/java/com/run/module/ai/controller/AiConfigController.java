package com.run.module.ai.controller;

import com.run.common.ai.AiClient;
import com.run.common.result.R;
import com.run.module.ai.dto.AiConfigDTO;
import com.run.module.ai.dto.AiConfigVO;
import com.run.module.ai.service.AiConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * AI 服务接入配置（前端「个人资料 → AI 服务接入」）
 *
 * - 密钥只存服务端，GET 仅回传掩码
 * - 保存后对后续 AI 调用立即生效（AiClient 每次实时读取生效配置，无需重启）
 * - /test 优先使用表单里未保存的新值，空白字段回退当前生效配置
 */
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiConfigController {

    private final AiConfigService aiConfigService;
    private final AiClient aiClient;

    /** 当前生效的接入配置（密钥仅掩码回传） */
    @GetMapping("/config")
    public R<AiConfigVO> getConfig() {
        return R.ok(aiConfigService.describe());
    }

    /** 保存接入配置（apiKey 留空表示不修改），保存后立即生效 */
    @PostMapping("/config")
    public R<AiConfigVO> saveConfig(@RequestBody AiConfigDTO dto) {
        return R.ok("AI 接入配置已保存",
                aiConfigService.save(dto.getBaseUrl(), dto.getApiKey(), dto.getModel()));
    }

    /** 连通性测试：用当前填写值发起一次真实调用，空白字段取当前生效配置 */
    @PostMapping("/test")
    public R<Map<String, Object>> test(@RequestBody(required = false) AiConfigDTO dto) {
        String url = (dto != null && notBlank(dto.getBaseUrl()))
                ? dto.getBaseUrl().trim() : aiConfigService.url();
        String key = (dto != null && notBlank(dto.getApiKey()))
                ? dto.getApiKey().trim() : aiConfigService.key();
        String model = (dto != null && notBlank(dto.getModel()))
                ? dto.getModel().trim() : aiConfigService.model();

        boolean keyUsable = notBlank(key) && !key.toLowerCase().contains("placeholder");
        if (isBlank(url) || !keyUsable) {
            return R.fail("未配置可用的接口地址或 API 密钥，请先填写");
        }

        long t0 = System.currentTimeMillis();
        String reply = aiClient.chat(url, key, model,
                "你是连通性测试助手。", "请只回复两个字：正常");
        if (reply != null && !reply.isBlank()) {
            long ms = System.currentTimeMillis() - t0;
            String text = reply.length() > 40 ? reply.substring(0, 40) + "…" : reply;
            return R.ok("连接成功", Map.of(
                    "latencyMs", ms,
                    "reply", text,
                    "model", model));
        }
        return R.fail("连接失败：请检查接口地址、API 密钥与模型名（详见后端日志）");
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
