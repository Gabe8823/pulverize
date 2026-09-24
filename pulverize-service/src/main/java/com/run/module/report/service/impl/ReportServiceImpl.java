package com.run.module.report.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.run.common.ai.AiClient;
import com.run.common.exception.BizException;
import com.run.module.report.dto.ReportBucketVO;
import com.run.module.report.dto.ReportQueryDTO;
import com.run.module.report.dto.ReportSummaryVO;
import com.run.module.report.dto.ReportVO;
import com.run.module.report.service.ReportService;
import com.run.module.running.entity.RunningActivity;
import com.run.module.running.service.RunningStatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 训练报告
 *
 * - 周期解析：DAY/WEEK/MONTH/QUARTER/YEAR 为内建周期（end 均为今天），CUSTOM 用传入日期
 * - 分桶：必须包含区间内没有跑步的空桶（DAY 按小时例外，只含有数据的小时），label 升序
 * - AI 报告：大模型生成正文，未配置 key 或调用失败时降级为规则文案（aiGenerated=false）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String[] WEEKDAY_LABELS = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};

    private final RunningStatsService runningStats;
    private final AiClient aiClient;

    /** 周期解析结果 */
    private record Period(LocalDate start, LocalDate end, String label) {
    }

    /** 桶范围（含首含尾） */
    private record BucketRange(String label, LocalDate from, LocalDate to) {
    }

    @Override
    public ReportVO generate(Long userId, ReportQueryDTO q) {
        ReportVO vo = new ReportVO();
        String granularity = parseGranularity(q.getGranularity());
        vo.setGranularity(granularity);

        LocalDate today = LocalDate.now();
        Period period = resolvePeriod(granularity, q, today);
        vo.setStartDate(period.start().format(DATE_FMT));
        vo.setEndDate(period.end().format(DATE_FMT));
        vo.setPeriodLabel(period.label());

        // end 为闭区间
        List<RunningActivity> activities = runningStats.listByRange(
                userId, period.start().atStartOfDay(), period.end().plusDays(1).atStartOfDay());

        vo.setSummary(buildSummary(activities));
        vo.setBuckets(buildBuckets(granularity, period, activities));
        vo.setAiAvailable(aiClient.enabled());
        buildReport(vo, period);
        return vo;
    }

    // ==================== 周期解析 ====================

    private String parseGranularity(String granularity) {
        if (granularity == null || granularity.isBlank()) {
            throw new BizException("granularity 必填，可选 DAY/WEEK/MONTH/QUARTER/YEAR/CUSTOM");
        }
        return granularity.trim().toUpperCase();
    }

    private Period resolvePeriod(String granularity, ReportQueryDTO q, LocalDate today) {
        return switch (granularity) {
            case "DAY" -> new Period(today, today, String.format("今日（%s）", fmt(today)));
            case "WEEK" -> {
                LocalDate start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                yield new Period(start, today, String.format("本周（%s ~ %s）", fmt(start), fmt(today)));
            }
            case "MONTH" -> new Period(today.withDayOfMonth(1), today, "本月");
            case "QUARTER" -> {
                int quarterStartMonth = (today.getMonthValue() - 1) / 3 * 3 + 1;
                LocalDate start = LocalDate.of(today.getYear(), quarterStartMonth, 1);
                yield new Period(start, today, "本季度");
            }
            case "YEAR" -> new Period(LocalDate.of(today.getYear(), 1, 1), today, "今年");
            case "CUSTOM" -> {
                LocalDate end = parseDate(q.getEndDate(), false);
                LocalDate start = parseDate(q.getStartDate(), true);
                if (start.isAfter(end)) {
                    throw new BizException("开始日期不能晚于结束日期");
                }
                yield new Period(start, end, String.format("自定义（%s ~ %s）", fmt(start), fmt(end)));
            }
            default -> throw new BizException("granularity 无效，可选 DAY/WEEK/MONTH/QUARTER/YEAR/CUSTOM");
        };
    }

    /** 解析 yyyy-MM-dd，空值时返回默认（end=今天、start=今天减 29 天） */
    private LocalDate parseDate(String value, boolean isStart) {
        if (value == null || value.isBlank()) {
            return isStart ? LocalDate.now().minusDays(29) : LocalDate.now();
        }
        try {
            return LocalDate.parse(value.trim(), DATE_FMT);
        } catch (Exception e) {
            throw new BizException("日期格式错误，应为 yyyy-MM-dd：" + value);
        }
    }

    // ==================== 汇总 ====================

    private ReportSummaryVO buildSummary(List<RunningActivity> activities) {
        ReportSummaryVO s = new ReportSummaryVO();
        double totalM = 0;
        long totalSec = 0;
        long hrSum = 0;
        int hrCnt = 0;
        int calories = 0;
        double elevation = 0;

        for (RunningActivity a : activities) {
            totalM += a.getDistanceM() == null ? 0 : a.getDistanceM();
            totalSec += a.getDurationSeconds() == null ? 0 : a.getDurationSeconds();
            if (a.getAvgHeartRate() != null) {
                hrSum += a.getAvgHeartRate();
                hrCnt++;
            }
            calories += a.getCalories() == null ? 0 : a.getCalories();
            elevation += a.getElevationGainM() == null ? 0 : a.getElevationGainM();
        }

        double km = totalM / 1000.0;
        s.setRuns(activities.size());
        s.setDistanceKm(round2(km));
        s.setDurationMin((int) (totalSec / 60));
        s.setAvgPaceSecKm(km > 0 ? (int) Math.round(totalSec / km) : null);
        s.setAvgHeartRate(hrCnt > 0 ? (int) Math.round(hrSum / (double) hrCnt) : null);
        s.setCalories(calories);
        s.setElevationGainM(elevation);
        return s;
    }

    // ==================== 分桶 ====================

    /** 桶运行时累加器（米/秒/心率和，收尾时换算成 VO 字段） */
    private static class BucketAcc {
        final String label;
        int runs;
        double meters;
        long seconds;
        long hrSum;
        int hrCnt;

        BucketAcc(String label) {
            this.label = label;
        }

        void add(RunningActivity a) {
            runs++;
            meters += a.getDistanceM() == null ? 0 : a.getDistanceM();
            seconds += a.getDurationSeconds() == null ? 0 : a.getDurationSeconds();
            if (a.getAvgHeartRate() != null) {
                hrSum += a.getAvgHeartRate();
                hrCnt++;
            }
        }

        ReportBucketVO toVO() {
            ReportBucketVO b = new ReportBucketVO();
            double km = meters / 1000.0;
            b.setLabel(label);
            b.setRuns(runs);
            b.setDistanceKm(round2(km));
            b.setDurationMin((int) (seconds / 60));
            b.setAvgPaceSecKm(km > 0 ? (int) Math.round(seconds / km) : null);
            b.setAvgHeartRate(hrCnt > 0 ? (int) Math.round(hrSum / (double) hrCnt) : null);
            return b;
        }
    }

    private List<ReportBucketVO> buildBuckets(String granularity, Period period, List<RunningActivity> activities) {
        if ("DAY".equals(granularity)) {
            return buildHourBuckets(activities);
        }

        List<BucketRange> ranges = buildRanges(granularity, period);
        List<BucketAcc> accs = new ArrayList<>();
        for (BucketRange r : ranges) {
            accs.add(new BucketAcc(r.label()));
        }

        // 活动按 startTime 升序，落入对应日期范围桶（相邻范围不重叠，命中即停）
        for (RunningActivity a : activities) {
            if (a.getStartTime() == null) continue;
            LocalDate date = a.getStartTime().toLocalDate();
            for (int i = 0; i < ranges.size(); i++) {
                BucketRange r = ranges.get(i);
                if (!date.isBefore(r.from()) && !date.isAfter(r.to())) {
                    accs.get(i).add(a);
                    break;
                }
            }
        }

        List<ReportBucketVO> result = new ArrayList<>(accs.size());
        for (BucketAcc acc : accs) {
            result.add(acc.toVO());
        }
        return result;
    }

    /** DAY：按小时分桶，只含有数据的小时，label「6点」「7点」…（按小时升序） */
    private List<ReportBucketVO> buildHourBuckets(List<RunningActivity> activities) {
        Map<Integer, BucketAcc> byHour = new java.util.TreeMap<>();
        for (RunningActivity a : activities) {
            if (a.getStartTime() == null) continue;
            byHour.computeIfAbsent(a.getStartTime().getHour(), h -> new BucketAcc(h + "点")).add(a);
        }
        List<ReportBucketVO> result = new ArrayList<>(byHour.size());
        for (BucketAcc acc : byHour.values()) {
            result.add(acc.toVO());
        }
        return result;
    }

    /** 生成各粒度的桶范围（label 升序，含空桶） */
    private List<BucketRange> buildRanges(String granularity, Period period) {
        List<BucketRange> ranges = new ArrayList<>();
        LocalDate start = period.start();
        LocalDate end = period.end();

        switch (granularity) {
            case "WEEK" -> {
                // 按天：周一 ~ 今天，label「周一」…
                for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
                    ranges.add(new BucketRange(WEEKDAY_LABELS[d.getDayOfWeek().getValue() - 1], d, d));
                }
            }
            case "MONTH" -> {
                // 按天：1 号 ~ 今天，label「9/1」…
                for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
                    ranges.add(new BucketRange(d.getMonthValue() + "/" + d.getDayOfMonth(), d, d));
                }
            }
            case "QUARTER" -> {
                // 按周（周一为桶起点）：季度首日所在周 ~ 今天所在周，label 该周周一「MM-dd」
                LocalDate week = start.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                while (!week.isAfter(end)) {
                    ranges.add(new BucketRange(fmt(week), week, week.plusDays(6)));
                    week = week.plusDays(7);
                }
            }
            case "YEAR" -> {
                // 按月：1 月 ~ 当前月，label「1月」…
                for (int m = 1; m <= end.getMonthValue(); m++) {
                    LocalDate from = LocalDate.of(end.getYear(), m, 1);
                    LocalDate to = from.withDayOfMonth(from.lengthOfMonth());
                    ranges.add(new BucketRange(m + "月", from, to));
                }
            }
            case "CUSTOM" -> {
                long spanDays = ChronoUnit.DAYS.between(start, end) + 1; // 含首含尾天数
                if (spanDays <= 31) {
                    // 按天「MM-dd」
                    for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
                        ranges.add(new BucketRange(fmt(d), d, d));
                    }
                } else if (spanDays <= 180) {
                    // 按周「MM-dd」（桶起点为 start，每 7 天一桶）
                    for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(7)) {
                        ranges.add(new BucketRange(fmt(d), d, d.plusDays(6)));
                    }
                } else {
                    // 按月「M月」
                    LocalDate cursor = start.withDayOfMonth(1);
                    while (!cursor.isAfter(end)) {
                        LocalDate from = cursor;
                        LocalDate to = cursor.withDayOfMonth(cursor.lengthOfMonth());
                        ranges.add(new BucketRange(cursor.getMonthValue() + "月", from, to));
                        cursor = cursor.plusMonths(1);
                    }
                }
            }
            default -> {
            }
        }
        return ranges;
    }

    private static double round2(double v) {
        return Math.round(v * 100) / 100.0;
    }

    // ==================== 报告正文 ====================

    private void buildReport(ReportVO vo, Period period) {
        try {
            String stats = assembleStats(vo, period);
            String system = "你是资深跑步教练和数据分析师，用简体中文写一段训练报告，面向普通跑步爱好者，语气专业、具体、鼓励式。"
                    + "只输出纯文本报告正文（4~6 句或 2 个自然段），不要标题、不要 Markdown、不要列表符号。";
            String user = String.format(
                    "统计周期：%s%n统计汇总与分桶数据如下，请从训练密度（跑几次/多久一次）、配速变化趋势、心率水平三个角度观察并给出鼓励式建议：%n%s",
                    vo.getPeriodLabel(), stats);
            String ai = aiClient.chat(system, user);
            if (ai != null && !ai.isBlank()) {
                vo.setAiReport(ai.trim());
                vo.setAiGenerated(true);
                return;
            }
        } catch (Exception e) {
            log.warn("报告 AI 生成失败，降级为规则文案: {}", e.getMessage());
        }
        vo.setAiReport(ruleReport(vo, period));
        vo.setAiGenerated(false);
    }

    /** 组装给大模型的统计文本（桶 >40 时只传前 20 + 后 20 并注明） */
    private String assembleStats(ReportVO vo, Period period) {
        ReportSummaryVO s = vo.getSummary();
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("周期：%s（%s ~ %s）%n", vo.getPeriodLabel(), vo.getStartDate(), vo.getEndDate()));
        sb.append(String.format("汇总：跑步 %d 次，累计 %.2f 公里，总时长 %d 分钟，平均配速 %s，平均心率 %s，消耗 %d 千卡，累计爬升 %.1f 米%n",
                s.getRuns(), s.getDistanceKm(), s.getDurationMin(),
                paceOrDash(s.getAvgPaceSecKm()), hrOrDash(s.getAvgHeartRate()),
                s.getCalories() == null ? 0 : s.getCalories(), s.getElevationGainM()));

        List<ReportBucketVO> buckets = vo.getBuckets();
        sb.append("分桶趋势：\n");
        if (buckets.size() > 40) {
            List<ReportBucketVO> view = new ArrayList<>(buckets.subList(0, 20));
            view.addAll(buckets.subList(buckets.size() - 20, buckets.size()));
            sb.append(String.format("（共 %d 个统计桶，仅展示前 20 与后 20）%n", buckets.size()));
            buckets = view;
        }
        for (ReportBucketVO b : buckets) {
            sb.append(String.format("%s: %d次 %.2f km 平均配速 %s 平均心率 %s%n",
                    b.getLabel(), b.getRuns(), b.getDistanceKm(),
                    paceOrDash(b.getAvgPaceSecKm()), hrOrDash(b.getAvgHeartRate())));
        }
        return sb.toString();
    }

    /** 规则兜底文案（aiGenerated=false） */
    private String ruleReport(ReportVO vo, Period period) {
        ReportSummaryVO s = vo.getSummary();
        if (s.getRuns() == 0) {
            return "所选周期内暂无跑步记录，完成第一次跑步后再来生成报告。";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("本期（%s）共 %d 次跑步、累计 %.1f 公里、总时长 %d 分钟、平均配速 %s、平均心率 %s。",
                vo.getPeriodLabel(), s.getRuns(), s.getDistanceKm(), s.getDurationMin(),
                s.getAvgPaceSecKm() == null ? "无数据" : paceStr(s.getAvgPaceSecKm()) + "/km",
                s.getAvgHeartRate() == null ? "无数据" : s.getAvgHeartRate() + " 次/分"));

        // 单期最高跑量
        ReportBucketVO top = null;
        for (ReportBucketVO b : vo.getBuckets()) {
            if (b.getRuns() > 0 && (top == null || b.getDistanceKm() > top.getDistanceKm())) {
                top = b;
            }
        }
        if (top != null) {
            sb.append(String.format("单期最高跑量出现在 %s（%.1f 公里）。", top.getLabel(), top.getDistanceKm()));
        }

        // 配速水平评价（对比 5'30" = 330s/km）
        if (s.getAvgPaceSecKm() != null) {
            int diff = s.getAvgPaceSecKm() - 330;
            if (diff < 0) {
                sb.append(String.format("整体配速水平较快，比 5'30\" 参考配速快 %d 秒，有氧基础比较扎实。", -diff));
            } else {
                sb.append(String.format("整体配速水平偏慢，比 5'30\" 参考配速慢 %d 秒，可逐步加入节奏跑提升速度。", diff));
            }
        }

        // 训练频率建议
        long days = ChronoUnit.DAYS.between(period.start(), period.end()) + 1;
        int perWeek = Math.max(1, (int) Math.round(s.getRuns() * 7.0 / Math.max(1, days)));
        sb.append(String.format("建议下期保持每周 %d 次左右的频率，逐步增加有氧占比。", perWeek));
        return sb.toString();
    }

    // ==================== 格式化 ====================

    private String fmt(LocalDate d) {
        return d.format(DATE_FMT);
    }

    /** 配速 mm'ss"/km */
    private String paceStr(Integer secKm) {
        if (secKm == null || secKm <= 0) return "--";
        return String.format("%d'%02d\"", secKm / 60, secKm % 60);
    }

    private String paceOrDash(Integer secKm) {
        return secKm == null ? "--" : paceStr(secKm) + "/km";
    }

    private String hrOrDash(Integer hr) {
        return hr == null ? "--" : String.valueOf(hr);
    }
}
