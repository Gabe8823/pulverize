package com.run.common.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 大模型对话客户端（OpenAI 兼容协议，默认 DeepSeek）
 *
 * 生效配置由 AiConfigProvider 提供（依赖倒置：接口配置由「AI 服务接入」模块落地）：
 * 界面保存的地址/密钥/模型优先，空白字段回退 yml 的 ai.api.* —— 保存后下一次调用即生效，无需重启。
 * 未配置（或仍是占位符）时 enabled() 为 false，所有调用返回 null，
 * 由业务方降级到规则文案 —— 保证 AI 功能"可接即用、无 key 不报错"。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final AiConfigProvider configProvider;

    /** 是否配置了可用的真实 key */
    public boolean enabled() {
        return usable(configProvider.url(), configProvider.key());
    }

    /** 使用当前生效配置对话（未配置返回 null，不抛异常） */
    public String chat(String systemPrompt, String userPrompt) {
        return chat(configProvider.url(), configProvider.key(), configProvider.model(),
                systemPrompt, userPrompt);
    }

    /**
     * 指定配置对话（供「测试连接」使用表单里未保存的新值）。
     * 未配置或调用失败返回 null（不抛异常），调用方自行降级。
     */
    public String chat(String url, String key, String model,
                       String systemPrompt, String userPrompt) {
        if (!usable(url, key)) {
            return null;
        }
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("model", model);
            body.put("temperature", 0.6);
            body.put("max_tokens", 900);
            body.put("messages", List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", userPrompt)));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(key);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            String content = root.path("choices").path(0).path("message").path("content").asText(null);
            return content == null || content.isBlank() ? null : content.trim();
        } catch (Exception e) {
            log.warn("AI 接口调用失败: {}", e.getMessage());
            return null;
        }
    }

    /** 地址非空且密钥可用（非空、非占位符） */
    private boolean usable(String url, String key) {
        return url != null && !url.isBlank()
                && key != null && !key.isBlank()
                && !key.toLowerCase().contains("placeholder");
    }
}
