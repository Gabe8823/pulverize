package com.run.module.mcp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.run.common.utils.JwtUtils;
import com.run.module.mcp.service.McpService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * MCP 端点（Streamable HTTP 传输）
 *
 * POST   /api/mcp  — JSON-RPC 2.0 请求（单条或批量）
 * GET    /api/mcp  — 不支持服务端主动推送，按规范返回 405
 * DELETE /api/mcp  — 关闭会话
 *
 * 认证：Authorization: Bearer <JWT> 或 X-RunAI-Token: <JWT>
 * 该端点已在 WebConfig 中排除 JWT 拦截器，以便返回标准 JSON-RPC 错误。
 */
@Slf4j
@RestController
@RequestMapping("/mcp")
@RequiredArgsConstructor
public class McpController {

    private final McpService mcpService;
    private final JwtUtils jwtUtils;
    private final ObjectMapper objectMapper;

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> handlePost(HttpServletRequest request, @RequestBody String body) {
        Long userId = resolveUserId(request);
        Object response = mcpService.handle(body, userId);
        if (response == null) {
            // 通知类消息（notifications/*）无需响应体
            return ResponseEntity.accepted()
                    .contentType(MediaType.APPLICATION_JSON)
                    .build();
        }
        try {
            return ResponseEntity.ok(objectMapper.writeValueAsString(response));
        } catch (Exception e) {
            log.error("MCP 响应序列化失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"jsonrpc\":\"2.0\",\"id\":null,\"error\":{\"code\":-32603,\"message\":\"Internal error\"}}");
        }
    }

    /** 不支持服务端主动 SSE 推送，按 MCP 规范返回 405 并声明 Allow */
    @GetMapping
    public ResponseEntity<Void> handleGet() {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .header("Allow", "POST, DELETE")
                .build();
    }

    /** 会话结束 */
    @DeleteMapping
    public ResponseEntity<Void> handleDelete() {
        return ResponseEntity.ok().build();
    }

    private Long resolveUserId(HttpServletRequest request) {
        String token = null;
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            token = auth.substring(7);
        } else {
            token = request.getHeader("X-RunAI-Token");
        }
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            return jwtUtils.validateToken(token) ? jwtUtils.getUserId(token) : null;
        } catch (Exception e) {
            log.warn("MCP token 解析失败: {}", e.getMessage());
            return null;
        }
    }
}
