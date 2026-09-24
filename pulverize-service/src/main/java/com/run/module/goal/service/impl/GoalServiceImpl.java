package com.run.module.goal.service.impl;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.run.common.ai.AiClient;
import com.run.common.exception.BizException;
import com.run.module.goal.dto.AiGoalSuggestionVO;
import com.run.module.goal.entity.UserGoal;
import com.run.module.goal.mapper.UserGoalMapper;
import com.run.module.goal.service.GoalService;
import com.run.module.running.entity.RunningActivity;
import com.run.module.running.service.RunningStatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 运动目标
 *
 * - 跑步数据统一走 RunningStatsService（跨模块只读出口），不直接依赖 RunningActivityMapper
 * - PB 类目标（BEST_5K/10K/HALF）建议文案附「对应成绩时间」（由目标配速 × 距离换算）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoalServiceImpl implements GoalService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final UserGoalMapper goalMapper;
    private final RunningStatsService runningStats;
    private final AiClient aiClient;

    @Override
    public UserGoal createGoal(Long userId, UserGoal goal) {
        goal.setId(null);
        goal.setUserId(userId);
        goal.setCurrentValue(0.0);
        goal.setStatus(0);
        goalMapper.insert(goal);
        refreshGoalProgress(userId); // 创建后立即刷新进度
        return goal;
    }

    @Override
    public List<UserGoal> listGoals(Long userId) {
        return goalMapper.selectList(
                new LambdaQueryWrapper<UserGoal>()
                        .eq(UserGoal::getUserId, userId)
                        .orderByDesc(UserGoal::getCreateTime));
    }

    @Override
    public void updateGoal(Long userId, Long goalId, UserGoal goal) {
        UserGoal existing = goalMapper.selectById(goalId);
        if (existing == null || !existing.getUserId().equals(userId)) {
            throw new BizException("目标不存在");
        }
        if (goal.getGoalType() != null) existing.setGoalType(goal.getGoalType());
        if (goal.getTargetValue() != null) existing.setTargetValue(goal.getTargetValue());
        if (goal.getUnit() != null) existing.setUnit(goal.getUnit());
        if (goal.getDeadline() != null) existing.setDeadline(goal.getDeadline());
        goalMapper.updateById(existing);
    }

    @Override
    public void deleteGoal(Long userId, Long goalId) {
        UserGoal goal = goalMapper.selectById(goalId);
        if (goal == null || !goal.getUserId().equals(userId)) {
            throw new BizException("目标不存在");
        }
        goalMapper.deleteById(goalId);
    }

    @Override
    public List<UserGoal> refreshGoalProgress(Long userId) {
        List<UserGoal> goals = goalMapper.selectList(
                new LambdaQueryWrapper<UserGoal>()
                        .eq(UserGoal::getUserId, userId)
                        .eq(UserGoal::getStatus, 0));

        for (UserGoal goal : goals) {
            double progress = calculateProgress(userId, goal);
            goal.setCurrentValue(progress);

            // 检查是否达标
            if (progress >= goal.getTargetValue()) {
                goal.setStatus(1); // 已完成
            }
            // 检查是否过期
            if (goal.getDeadline() != null && LocalDate.now().isAfter(goal.getDeadline())) {
                goal.setStatus(2); // 已过期
            }

            goalMapper.updateById(goal);
        }
        return goals;
    }

    private double calculateProgress(Long userId, UserGoal goal) {
        LocalDate now = LocalDate.now();
        LocalDateTime start, end;

        switch (goal.getGoalType()) {
            case "WEEKLY_DISTANCE" -> {
                start = now.with(java.time.DayOfWeek.MONDAY).atStartOfDay();
                end = start.plusDays(7);
            }
            case "MONTHLY_DISTANCE" -> {
                start = now.withDayOfMonth(1).atStartOfDay();
                end = now.withDayOfMonth(now.lengthOfMonth()).plusDays(1).atStartOfDay();
            }
            case "BEST_5K", "BEST_10K", "BEST_HALF" -> {
                // 个人最佳成绩：取最快的一次
                double targetKm = goal.getGoalType().equals("BEST_5K") ? 5 :
                        goal.getGoalType().equals("BEST_10K") ? 10 : 21.0975;
                return getBestPace(userId, targetKm);
            }
            default -> {
                // 自定义目标，取所有历史
                start = LocalDateTime.of(2020, 1, 1, 0, 0);
                end = LocalDateTime.now();
            }
        }

        // 统计跑量（公里）
        List<RunningActivity> activities = runningStats.listByRange(userId, start, end);

        return activities.stream()
                .filter(a -> a.getDistanceM() != null)
                .mapToDouble(a -> a.getDistanceM() / 1000.0).sum();
    }

    // ==================== 目标 AI 推荐 ====================

    @Override
    public AiGoalSuggestionVO aiSuggest(Long userId, String goalType) {
        String type = (goalType == null || goalType.isBlank()) ? "WEEKLY_DISTANCE" : goalType.trim().toUpperCase();
        try {
            // 1. 拉取近 8 周跑步记录
            LocalDate today = LocalDate.now();
            LocalDate start8w = today.minusWeeks(8);
            LocalDate start4w = today.minusWeeks(4);
            List<RunningActivity> activities = runningStats.listByRange(
                    userId, start8w.atStartOfDay(), today.plusDays(1).atStartOfDay());

            // 2. 基础统计
            double totalKm = 0;
            double last4Km = 0;
            for (RunningActivity a : activities) {
                double km = a.getDistanceM() == null ? 0 : a.getDistanceM() / 1000.0;
                totalKm += km;
                if (a.getStartTime() != null && !a.getStartTime().isBefore(start4w.atStartOfDay())) {
                    last4Km += km;
                }
            }
            double avgWeeklyKm = totalKm / 8.0;
            int best5k = bestPace(activities, 5000, 330);
            int best10k = bestPace(activities, 10000, 360);
            int bestHalf = bestPace(activities, 21097.5, 390);

            // 3. 规则基线（同时作为 AI 失败兜底）
            AiGoalSuggestionVO vo = ruleSuggest(type, avgWeeklyKm, last4Km, best5k, best10k, bestHalf, today);

            // 4. AI 覆盖：任一字段被采纳即 source = AI
            applyAiSuggestion(vo, type, avgWeeklyKm, last4Km, best5k, best10k, bestHalf, today);
            return vo;
        } catch (Exception e) {
            log.warn("目标 AI 推荐失败，回退规则基线: {}", e.getMessage());
            AiGoalSuggestionVO vo = new AiGoalSuggestionVO();
            vo.setTargetValue(30.0);
            vo.setUnit("km");
            vo.setDeadline(LocalDate.now().plusDays(90).format(DATE_FMT));
            vo.setSuggestion("根据你近 8 周的历史跑量设定目标，暂无足够数据给出精确建议，坚持规律跑步后再来复盘。");
            vo.setSource("RULE");
            return vo;
        }
    }

    /** 区间内满足最小距离且有有效配速的最快配速（s/km），无数据返回默认值 */
    private int bestPace(List<RunningActivity> activities, double minDistanceM, int defaultPace) {
        int best = 0;
        for (RunningActivity a : activities) {
            if (a.getDistanceM() == null || a.getDistanceM() < minDistanceM) continue;
            Integer pace = a.getAvgPaceSecKm();
            if (pace == null || pace < 120 || pace > 900) continue;
            if (best == 0 || pace < best) best = pace;
        }
        return best > 0 ? best : defaultPace;
    }

    /** 规则基线建议 */
    private AiGoalSuggestionVO ruleSuggest(String type, double avgWeeklyKm, double last4Km,
                                           int best5k, int best10k, int bestHalf, LocalDate today) {
        AiGoalSuggestionVO vo = new AiGoalSuggestionVO();
        vo.setSource("RULE");
        switch (type) {
            case "WEEKLY_DISTANCE" -> {
                double target = Math.max(5, Math.ceil(avgWeeklyKm * 1.15 / 5) * 5);
                vo.setTargetValue(target);
                vo.setUnit("km");
                vo.setDeadline(today.plusDays(56).format(DATE_FMT));
                vo.setSuggestion(String.format(
                        "你近 8 周平均每周跑 %.1f 公里，按 15%% 渐进负荷原则，建议将周跑量目标定为 %.0f 公里。该幅度兼顾提升与防伤，8 周后复盘调整。",
                        avgWeeklyKm, target));
            }
            case "MONTHLY_DISTANCE" -> {
                double target = Math.max(20, Math.ceil(avgWeeklyKm * 4.3 * 1.1 / 5) * 5);
                vo.setTargetValue(target);
                vo.setUnit("km");
                vo.setDeadline(today.plusDays(84).format(DATE_FMT));
                vo.setSuggestion(String.format(
                        "你近 4 周累计跑 %.1f 公里，近 8 周平均每周 %.1f 公里，按 10%% 渐进负荷建议将月跑量目标定为 %.0f 公里。分 12 周逐步达成，期间注意控制高强度比例。",
                        last4Km, avgWeeklyKm, target));
            }
            case "BEST_5K" -> {
                double target = Math.max(150, best5k - 15);
                vo.setTargetValue(target);
                vo.setUnit("s/km");
                vo.setDeadline(today.plusDays(60).format(DATE_FMT));
                vo.setSuggestion(String.format(
                        "你当前 5K 最佳配速为 %s/km（成绩 %s），建议将目标提升至 %s/km（每公里快 15 秒，对应 5K 成绩 %s）。配合每周 1 次间歇与 1 次节奏跑，两个月后复盘检验。",
                        paceStr(best5k), timeStr(best5k * 5L),
                        paceStr((int) target), timeStr(Math.round(target * 5))));
            }
            case "BEST_10K" -> {
                double target = Math.max(150, best10k - 15);
                vo.setTargetValue(target);
                vo.setUnit("s/km");
                vo.setDeadline(today.plusDays(75).format(DATE_FMT));
                vo.setSuggestion(String.format(
                        "你当前 10K 最佳配速为 %s/km（成绩 %s），建议将目标提升至 %s/km（对应 10K 成绩 %s）。以有氧基础加乳酸阈训练组合推进，75 天后复盘检验。",
                        paceStr(best10k), timeStr(best10k * 10L),
                        paceStr((int) target), timeStr(Math.round(target * 10))));
            }
            case "BEST_HALF" -> {
                double target = Math.max(150, bestHalf - 20);
                vo.setTargetValue(target);
                vo.setUnit("s/km");
                vo.setDeadline(today.plusDays(90).format(DATE_FMT));
                vo.setSuggestion(String.format(
                        "你当前半马最佳配速为 %s/km（成绩 %s），建议将目标提升至 %s/km（每公里快 20 秒，对应半马成绩 %s）。逐步增加长距离慢跑占比，90 天后复盘检验。",
                        paceStr(bestHalf), timeStr(Math.round(bestHalf * 21.0975)),
                        paceStr((int) target), timeStr(Math.round(target * 21.0975))));
            }
            default -> {
                double target = Math.max(30, Math.ceil((avgWeeklyKm * 4.3) / 10) * 10);
                vo.setTargetValue(target);
                vo.setUnit("km");
                vo.setDeadline(today.plusDays(90).format(DATE_FMT));
                vo.setSuggestion(String.format(
                        "该目标类型暂无内置模板，根据你近 8 周平均每周 %.1f 公里（月均约 %.1f 公里）的历史跑量，建议将目标设定为 %.0f 公里。按历史跑量循序渐进，90 天后复盘调整。",
                        avgWeeklyKm, avgWeeklyKm * 4.3, target));
            }
        }
        return vo;
    }

    /** AI 覆盖：targetValue / deadline / suggestion 逐字段校验后采纳，任一被采纳 source=AI */
    private void applyAiSuggestion(AiGoalSuggestionVO vo, String type, double avgWeeklyKm, double last4Km,
                                   int best5k, int best10k, int bestHalf, LocalDate today) {
        if (!aiClient.enabled()) {
            return;
        }
        try {
            String system = "你是资深跑步教练，根据用户跑步数据给出目标建议。只输出一个 JSON 对象，不要输出任何其他文字或代码块标记。";
            boolean paceType = type.startsWith("BEST_");
            String user = String.format(
                    """
                    目标类型 goalType：%s
                    近 8 周平均每周跑量：%.1f 公里
                    近 4 周总跑量：%.1f 公里
                    最佳 5K 配速：%s/km
                    最佳 10K 配速：%s/km
                    最佳半马配速：%s/km
                    规则基线建议值：目标 %.0f %s，截止日期 %s
                    请结合 %s 目标给出你的建议。
                    要求只输出 JSON：{"targetValue":数字,"deadline":"yyyy-MM-dd","suggestion":"中文2~4句"}
                    """,
                    type, avgWeeklyKm, last4Km,
                    paceStr(best5k), paceStr(best10k), paceStr(bestHalf),
                    vo.getTargetValue(), vo.getUnit(), vo.getDeadline(),
                    paceType ? "配速类" : "距离类");

            String reply = aiClient.chat(system, user);
            if (reply == null || reply.isBlank()) {
                return;
            }
            int s = reply.indexOf('{');
            int e = reply.lastIndexOf('}');
            if (s < 0 || e <= s) {
                return;
            }
            JSONObject obj = JSONUtil.parseObj(reply.substring(s, e + 1));
            boolean adopted = false;

            // targetValue 校验：>0，配速类 120~600，距离类 1~500；解析失败保留规则值
            try {
                Object tvRaw = obj.get("targetValue");
                if (tvRaw != null) {
                    double tv = Double.parseDouble(String.valueOf(tvRaw));
                    boolean rangeOk = paceType ? (tv >= 120 && tv <= 600) : (tv >= 1 && tv <= 500);
                    if (tv > 0 && rangeOk) {
                        vo.setTargetValue(tv);
                        adopted = true;
                    }
                }
            } catch (Exception ex) {
                log.debug("AI targetValue 非法，保留规则值: {}", ex.getMessage());
            }

            // deadline 必须是今天或之后，否则（含格式非法）保留规则值
            try {
                String dl = obj.getStr("deadline", null);
                if (dl != null && !dl.isBlank()) {
                    LocalDate dlDate = LocalDate.parse(dl.trim());
                    if (!dlDate.isBefore(today)) {
                        vo.setDeadline(dlDate.format(DATE_FMT));
                        adopted = true;
                    }
                }
            } catch (Exception ex) {
                log.debug("AI deadline 非法，保留规则值: {}", ex.getMessage());
            }

            // suggestion 非空才覆盖
            String suggestion = obj.getStr("suggestion", null);
            if (suggestion != null && !suggestion.isBlank()) {
                vo.setSuggestion(suggestion.trim());
                adopted = true;
            }

            if (adopted) {
                vo.setSource("AI");
            }
        } catch (Exception e) {
            // AI 结果任何一步失败都保留已采纳字段之外的规则字段
            log.warn("目标 AI 建议解析失败，保留规则基线: {}", e.getMessage());
        }
    }

    /** 配速格式 mm'ss"/km */
    private String paceStr(int secKm) {
        return String.format("%d'%02d\"", secKm / 60, secKm % 60);
    }

    /** 成绩时间 h:mm:ss 或 mm:ss（PB 目标文案展示「对应成绩时间」用） */
    private String timeStr(long sec) {
        if (sec >= 3600) {
            return String.format("%d:%02d:%02d", sec / 3600, (sec % 3600) / 60, sec % 60);
        }
        return String.format("%d:%02d", sec / 60, sec % 60);
    }

    /** 达到目标距离 95% 以上、配速最快的一次（s/km），无数据返回 0 */
    private double getBestPace(Long userId, double targetKm) {
        return runningStats.listAll(userId).stream()
                .filter(a -> a.getDistanceM() != null && a.getDistanceM() >= targetKm * 1000 * 0.95)
                .filter(a -> a.getAvgPaceSecKm() != null && a.getAvgPaceSecKm() > 0)
                .mapToInt(RunningActivity::getAvgPaceSecKm)
                .min()
                .orElse(0);
    }
}
