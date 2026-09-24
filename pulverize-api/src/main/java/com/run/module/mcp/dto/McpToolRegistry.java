package com.run.module.mcp.dto;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MCP 工具定义注册表（tools/list 返回内容）
 */
public final class McpToolRegistry {

    private McpToolRegistry() {
    }

    public static List<Map<String, Object>> definitions() {
        return List.of(
                tool("get_profile",
                        "获取当前用户的跑步者档案：昵称、性别、身高体重、最大心率/静息心率。用于个性化训练建议。",
                        props()),

                tool("get_weekly_stats",
                        "获取跑步统计：本周与全量的总距离、次数、总时长、平均配速、平均心率、消耗热量。评估训练负荷的第一步。",
                        props()),

                tool("list_activities",
                        "分页查询跑步活动记录（按时间倒序），包含距离、时长、配速、心率、来源平台。",
                        props(
                                "page", prop("integer", "页码，默认 1"),
                                "size", prop("integer", "每页条数，默认 10，最大 50"),
                                "startDate", prop("string", "起始日期，格式 yyyy-MM-dd"),
                                "endDate", prop("string", "结束日期，格式 yyyy-MM-dd"),
                                "activityType", prop("string", "活动类型过滤，如 RUNNING / TRAIL_RUN")
                        )),

                tool("get_activity_detail",
                        "获取单次跑步详情：完整指标 + 每公里分段（配速/心率/用时）。",
                        props("activityId", prop("integer", "活动 ID"))),

                tool("analyze_activity",
                        "分析跑步表现：对指定的一次跑步做配速、心率、步频、疲劳度与训练效果分析，给出综合评分与教练摘要，结果会自动保存。",
                        props("activityId", prop("integer", "活动 ID"))),

                tool("get_analysis",
                        "读取分析结果：查看某次跑步已保存的评分、教练摘要与各维度结论；如果这次跑步还没分析过，会提示你先进行分析。",
                        props("activityId", prop("integer", "活动 ID"))),

                tool("generate_report",
                        "生成训练报告：按 天/周/月/季度/年 或自定义时间周期汇总跑量、时长、配速与心率趋势，并生成一段 AI 教练文字报告，可用于周期性复盘。",
                        props(
                                "granularity", prop("string", "统计粒度：DAY / WEEK / MONTH / QUARTER / YEAR / CUSTOM，默认 WEEK"),
                                "startDate", prop("string", "起始日期 yyyy-MM-dd，CUSTOM 时可选"),
                                "endDate", prop("string", "结束日期 yyyy-MM-dd，CUSTOM 时可选")
                        )),

                tool("get_vdot",
                        "查询跑力指数（VDOT）：基于该用户全部历史跑步数据中最优的一次有氧表现推算有氧能力，附 5K/10K/半马/全马等效完赛时间与 Daniels 五档训练配速区间。",
                        props()),

                tool("list_goals",
                        "列出用户的运动目标（周跑量/最佳配速等）及当前进度。",
                        props()),

                tool("refresh_goals",
                        "根据最新跑步数据刷新所有目标进度，并返回刷新后的目标列表。",
                        props()),

                tool("list_plans",
                        "列出用户的训练计划。",
                        props()),

                tool("get_plan",
                        "获取训练计划详情：计划说明（AI 推理过程）与每日训练安排。",
                        props("planId", prop("integer", "计划 ID"))),

                tool("generate_plan",
                        "生成训练计划：结合你的跑步目标与可用时间，生成一份个性化周期训练计划并自动保存，之后可在训练计划列表中查看。训练目标支持 5 公里 PB、10 公里 PB、半程马拉松、全程马拉松、减脂与保持健康。",
                        props(
                                "goalType", prop("string", "训练目标类型（必填）：5K_PB / 10K_PB / HALF_MARATHON / MARATHON / LOSE_WEIGHT / KEEP_FIT"),
                                "goalValue", prop("string", "目标值，如 25:00 或 完赛"),
                                "startDate", prop("string", "开始日期 yyyy-MM-dd（必填）"),
                                "endDate", prop("string", "结束日期 yyyy-MM-dd（必填）"),
                                "weeklyTrainingDays", prop("integer", "每周训练天数 2-7，默认 4"),
                                "trainingDurationMinutes", prop("integer", "每次训练时长（分钟），默认 60")
                        )),

                tool("list_platforms",
                        "列出所有第三方运动平台的连接与同步状态（高驰 COROS 等）。",
                        props()),

                tool("sync_platform",
                        "同步运动数据：从指定运动平台（默认高驰 COROS）拉取最新的跑步记录并保存，首次同步耗时较长。",
                        props("platform", prop("string", "平台标识，默认 COROS")))
        );
    }

    private static Map<String, Object> tool(String name, String description, Map<String, Object> properties) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);

        Map<String, Object> tool = new LinkedHashMap<>();
        tool.put("name", name);
        tool.put("description", description);
        tool.put("inputSchema", schema);
        return tool;
    }

    private static Map<String, Object> props(Object... kv) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            map.put((String) kv[i], kv[i + 1]);
        }
        return map;
    }

    private static Map<String, Object> prop(String type, String description) {
        Map<String, Object> prop = new LinkedHashMap<>();
        prop.put("type", type);
        prop.put("description", description);
        return prop;
    }
}
