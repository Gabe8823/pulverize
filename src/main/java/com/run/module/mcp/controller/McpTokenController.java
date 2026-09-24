package com.run.module.mcp.controller;

import com.run.common.result.R;
import com.run.common.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * MCP 专用访问令牌签发
 *
 * POST /api/mcp/token —— 登录态换取一个 30 天有效的 MCP 令牌。
 *
 * 安全约定：
 * - 该令牌与登录 JWT 同一签名密钥，MCP 端点（/api/mcp）可直接校验，但有效期 30 天，
 *   与 2 小时的登录令牌解耦 —— 外部工具（Claude/Cursor）无需登录密码。
 * - 明文只在本次响应返回一次；前端点击生成后立即复制到剪贴板，不写入 localStorage、不渲染进 DOM，
 *   页面只展示掩码（前6后4）与有效期。
 * - 本路径不在 WebConfig 的拦截排除清单内（排除项是精确的 /mcp），必须携带登录态调用。
 */
@RestController
@RequestMapping("/mcp/token")
@RequiredArgsConstructor
public class McpTokenController {

    private static final long MCP_TOKEN_DAYS = 30;

    private final JwtUtils jwtUtils;

    @PostMapping
    public R<Map<String, Object>> generate(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String username = (String) request.getAttribute("username");

        String token = jwtUtils.generateMcpToken(userId, username);
        LocalDateTime expireAt = LocalDateTime.now().plusDays(MCP_TOKEN_DAYS);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("token", token);
        data.put("expiresAt", expireAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        data.put("expiresInDays", MCP_TOKEN_DAYS);
        return R.ok("MCP 令牌已生成", data);
    }
}
