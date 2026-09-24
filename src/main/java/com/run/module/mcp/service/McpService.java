package com.run.module.mcp.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.run.common.exception.BizException;
import com.run.module.analysis.service.AnalysisService;
import com.run.module.goal.service.GoalService;
import com.run.module.mcp.dto.McpToolRegistry;
import com.run.module.plan.dto.GeneratePlanRequest;
import com.run.module.plan.service.PlanService;
import com.run.module.platform.service.PlatformService;
import com.run.module.report.dto.ReportQueryDTO;
import com.run.module.report.service.ReportService;
import com.run.module.running.dto.ActivityDetailVO;
import com.run.module.running.dto.ActivityQueryDTO;
import com.run.module.running.entity.RunningActivity;
import com.run.module.running.service.RunningService;
import com.run.module.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MCP (Model Context Protocol) 服务端实现
 *
 * 传输: Streamable HTTP 单次 POST JSON-RPC 2.0（响应为 application/json）
 * 端点: POST /api/mcp
 * 认证: Authorization: Bearer <JWT>（工具调用必须，initialize / tools/list 免认证）
 *
 * AI 客户端（Claude Desktop / Claude Code / Cursor 等）接入后即可
 * 读取用户跑步数据、生成计划，充当"AI 教练"的数据与执行通道。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class McpService {

    public static final String SERVER_NAME = "run-ai-mcp";
    public static final String SERVER_VERSION = "1.0.0";
    public static final String DEFAULT_PROTOCOL_VERSION = "2025-06-18";

    private final ObjectMapper objectMapper;
    private final UserService userService;
    private final RunningService runningService;
    private final AnalysisService analysisService;
    private final PlanService planService;
    private final GoalService goalService;
    private final PlatformService platformService;
    private final ReportService reportService;

    /**
     * 处理 JSON-RPC 请求（单个或批量）
     *
     * @param body   请求体 JSON
     * @param userId 已解析的用户 ID（未认证时为 null）
     * @return 响应对象；通知类请求（无需响应）返回 null
     */
    public Object handle(String body, Long userId) {
        JsonNode root;
        try {
            root = objectMapper.readTree(body);
        } catch (JsonProcessingException e) {
            return error(null, -32700, "Parse error: 无效的 JSON", null);
        }

        if (root == null || (!root.isObject() && !root.isArray())) {
            return error(null, -32600, "Invalid Request", null);
        }

        if (root.isArray()) {
            if (root.isEmpty()) {
                return error(null, -32600, "Invalid Request: 空批量请求", null);
            }
            List<Object> responses = new ArrayList<>();
            for (JsonNode node : root) {
                Object resp = process(node, userId);
                if (resp != null) {
                    responses.add(resp);
                }
            }
            return responses.isEmpty() ? null : responses;
        }

        return process(root, userId);
    }

    // ==================== 单条消息处理 ====================

    private Object process(JsonNode req, Long userId) {
        JsonNode idNode = req.path("id");
        Object id = (idNode.isMissingNode() || idNode.isNull())
                ? null
                : (idNode.isNumber() ? idNode.numberValue() : idNode.asText());

        String method = req.path("method").asText("");
        // 通知类消息：无需响应
        if (method.startsWith("notifications/") || method.isEmpty() && id == null) {
            return null;
        }

        JsonNode params = req.path("params");
        try {
            switch (method) {
                case "initialize" -> {
                    return ok(id, initializeResult(params));
                }
                case "ping" -> {
                    return ok(id, m());
                }
                case "tools/list" -> {
                    return ok(id, m("tools", McpToolRegistry.definitions()));
                }
                case "tools/call" -> {
                    return handleToolsCall(id, params, userId);
                }
                default -> {
                    if (method.isEmpty()) {
                        return error(id, -32600, "Invalid Request: 缺少 method", null);
                    }
                    return error(id, -32601, "Method not found: " + method, null);
                }
            }
        } catch (Exception e) {
            log.error("MCP 处理异常: method={}", method, e);
            return error(id, -32603, "Internal error: " + e.getMessage(), null);
        }
    }

    private Map<String, Object> initializeResult(JsonNode params) {
        return m(
                "protocolVersion", params.path("protocolVersion").asText(DEFAULT_PROTOCOL_VERSION),
                "capabilities", m("tools", m("listChanged", false)),
                "serverInfo", m("name", SERVER_NAME, "version", SERVER_VERSION),
                "instructions", "RunAI 是个人跑步数据分析与 AI 教练平台。" +
                        "调用工具可读取当前用户的跑步活动、周统计、分析结论、训练目标与训练计划，" +
                        "并可触发第三方平台（如高驰 COROS）数据同步。" +
                        "给出训练建议前，请先用 get_weekly_stats 和 list_activities 了解用户近期负荷。"
        );
    }

    private Object handleToolsCall(Object id, JsonNode params, Long userId) {
        if (userId == null) {
            return error(id, -32001, "未授权：请携带 Authorization: Bearer <JWT> 请求头", null);
        }
        String name = params.path("name").asText("");
        JsonNode args = params.path("arguments");
        try {
            String text = callTool(name, args, userId);
            return ok(id, m(
                    "content", List.of(m("type", "text", "text", text)),
                    "isError", false
            ));
        } catch (BizException e) {
            return ok(id, m(
                    "content", List.of(m("type", "text", "text", e.getMessage())),
                    "isError", true
            ));
        } catch (Exception e) {
            log.error("MCP 工具执行失败: tool={}", name, e);
            return ok(id, m(
                    "content", List.of(m("type", "text", "text", "工具执行失败: " + e.getMessage())),
                    "isError", true
            ));
        }
    }

    // ==================== 工具实现 ====================

    private String callTool(String name, JsonNode args, Long userId) throws Exception {
        return switch (name) {
            case "get_profile" -> json(userService.getUserInfo(userId));

            case "get_weekly_stats" -> json(m(
                    "week", runningService.getWeeklyStats(userId),
                    "allTime", runningService.getAllTimeStats(userId)
            ));

            case "list_activities" -> {
                ActivityQueryDTO query = new ActivityQueryDTO();
                query.setPageNum(Math.max(1, args.path("page").asInt(1)));
                query.setPageSize(Math.min(50, Math.max(1, args.path("size").asInt(10))));
                if (args.hasNonNull("startDate")) {
                    query.setStartDate(LocalDate.parse(args.get("startDate").asText()));
                }
                if (args.hasNonNull("endDate")) {
                    query.setEndDate(LocalDate.parse(args.get("endDate").asText()));
                }
                if (args.hasNonNull("activityType")) {
                    query.setActivityType(args.get("activityType").asText());
                }
                IPage<RunningActivity> page = runningService.listActivities(userId, query);
                yield json(m(
                        "total", page.getTotal(),
                        "pageNum", page.getCurrent(),
                        "pageSize", page.getSize(),
                        "records", page.getRecords()
                ));
            }

            case "get_activity_detail" -> json(requireActivity(userId, args));
            case "analyze_activity" -> json(analysisService.analyzeActivity(userId, longArg(args, "activityId")));
            case "get_analysis" -> json(analysisService.getAnalysis(userId, longArg(args, "activityId")));
            case "get_vdot" -> json(analysisService.getVdot(userId));

            case "list_goals" -> json(goalService.listGoals(userId));
            case "refresh_goals" -> json(goalService.refreshGoalProgress(userId));

            case "list_plans" -> json(planService.listPlans(userId));
            case "get_plan" -> json(planService.getPlanDetail(userId, longArg(args, "planId")));
            case "generate_plan" -> json(planService.generatePlan(userId, planRequest(args)));

            case "list_platforms" -> json(platformService.listPlatforms(userId));
            case "sync_platform" -> json(platformService.sync(userId, args.path("platform").asText("COROS")));

            case "generate_report" -> {
                ReportQueryDTO dto = new ReportQueryDTO();
                dto.setGranularity(args.path("granularity").asText("WEEK"));
                if (args.hasNonNull("startDate")) {
                    dto.setStartDate(args.get("startDate").asText());
                }
                if (args.hasNonNull("endDate")) {
                    dto.setEndDate(args.get("endDate").asText());
                }
                yield json(reportService.generate(userId, dto));
            }

            default -> throw new BizException("未知工具: " + name + "（用 tools/list 查看可用工具）");
        };
    }

    private ActivityDetailVO requireActivity(Long userId, JsonNode args) {
        return runningService.getActivityDetail(userId, longArg(args, "activityId"));
    }

    private GeneratePlanRequest planRequest(JsonNode args) {
        GeneratePlanRequest req = objectMapper.convertValue(args, GeneratePlanRequest.class);
        if (req.getGoalType() == null || req.getStartDate() == null || req.getEndDate() == null) {
            throw new BizException("generate_plan 需要 goalType、startDate、endDate 参数");
        }
        return req;
    }

    private long longArg(JsonNode args, String field) {
        JsonNode v = args.get(field);
        if (v == null || v.isNull()) {
            throw new BizException("缺少参数: " + field);
        }
        return v.asLong();
    }

    // ==================== JSON-RPC 构造 ====================

    private Map<String, Object> ok(Object id, Object result) {
        Map<String, Object> resp = m();
        resp.put("jsonrpc", "2.0");
        resp.put("id", id);
        resp.put("result", result);
        return resp;
    }

    private Map<String, Object> error(Object id, int code, String message, Object data) {
        Map<String, Object> err = m("code", code, "message", message);
        if (data != null) {
            err.put("data", data);
        }
        Map<String, Object> resp = m();
        resp.put("jsonrpc", "2.0");
        resp.put("id", id);
        resp.put("error", err);
        return resp;
    }

    private String json(Object value) throws JsonProcessingException {
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
    }

    private static Map<String, Object> m(Object... kv) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            map.put((String) kv[i], kv[i + 1]);
        }
        return map;
    }
}
