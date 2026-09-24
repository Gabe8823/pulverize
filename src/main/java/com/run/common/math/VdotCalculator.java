package com.run.common.math;

/**
 * VDOT（跑力指数）计算器 —— Jack Daniels 公式（纯静态函数，无外部依赖）。
 *
 * 推算链路：
 *   距离/时长 → 平均速度 v（m/min）
 *   → 该速度需氧量 VO2(v) = -4.60 + 0.182258·v + 0.000104·v²
 *   → 该时长下配速占最大摄氧量百分比 %VO2max(t)（Daniels 经验锚点分段线性）
 *   → VO2max = VO2(v) / %VO2max(t) ≈ VDOT
 *
 * 反向（等效完赛时间）：固定点迭代 t = D / (%VO2max(t) × vVO2max)，3~4 轮收敛。
 * 校验：贝克勒 5000m 12:37 → VDOT≈85，其等效 5K ≈ 12:39；VDOT 42 → 等效 5K ≈ 22:5x。
 *
 * 训练区间：%vVO2max 五档（Daniels）：
 *   轻松跑 59–74 / 马拉松配速 75–84 / 乳酸阈配速 85–88 / 间歇配速 95–100 / 重复跑配速 100–105。
 */
public final class VdotCalculator {

    private VdotCalculator() {
    }

    /** Daniels 五区间名称 */
    public static final String[] ZONE_NAMES =
            {"轻松跑", "马拉松配速", "乳酸阈配速", "间歇配速", "重复跑配速"};

    /** Daniels 五区间 %vVO2max 上下界（含小数） */
    public static final double[][] ZONE_PCTS =
            {{0.59, 0.74}, {0.75, 0.84}, {0.85, 0.88}, {0.95, 1.00}, {1.00, 1.05}};

    /** Daniels 五区间强度标签（与 ZONE_PCTS 一一对应） */
    public static final String[] ZONE_LABELS =
            {"59–74%", "75–84%", "85–88%", "95–100%", "100–105%"};

    /** 某速度（m/min）的需氧量（ml/kg/min） */
    public static double vo2AtSpeed(double mPerMin) {
        return -4.60 + 0.182258 * mPerMin + 0.000104 * mPerMin * mPerMin;
    }

    /**
     * 比赛/努力时长（秒）→ 该配速占最大摄氧量百分比（0~1）。
     * Daniels 经验锚点分段线性：8 分钟全力≈100%，13 分钟（5K 快）≈99%，
     * 30 分钟（10K）≈95%，75 分钟（半马）≈90.8%，130 分钟（全马）≈88.1%。
     */
    public static double pctOfVo2max(double tSec) {
        double t = tSec / 60.0;
        double[][] pts = {
                {8, 1.000}, {13, 0.990}, {20, 0.972}, {30, 0.950}, {40, 0.935},
                {60, 0.918}, {75, 0.908}, {95, 0.896}, {115, 0.886}, {130, 0.881},
                {160, 0.875}, {240, 0.868}
        };
        if (t <= pts[0][0]) {
            return pts[0][1];
        }
        if (t >= pts[pts.length - 1][0]) {
            return pts[pts.length - 1][1];
        }
        for (int i = 1; i < pts.length; i++) {
            if (t <= pts[i][0]) {
                double t0 = pts[i - 1][0];
                double p0 = pts[i - 1][1];
                double t1 = pts[i][0];
                double p1 = pts[i][1];
                return p0 + (p1 - p0) * (t - t0) / (t1 - t0);
            }
        }
        return 0.90;
    }

    /** 由距离（米）与时长（秒）推算 VDOT；数据非法返回 0 */
    public static double compute(double distanceM, double durationSec) {
        if (distanceM <= 0 || durationSec <= 0) {
            return 0;
        }
        double speed = distanceM / durationSec * 60.0; // m/min
        double vo2 = vo2AtSpeed(speed);
        double pct = pctOfVo2max(durationSec);
        if (pct <= 0) {
            return 0;
        }
        return Math.max(0, vo2 / pct);
    }

    /** VDOT → vVO2max 对应速度（m/min），对 Daniels 公式一元二次求根 */
    public static double vvo2maxSpeed(double vdot) {
        if (vdot <= 0) {
            return 0;
        }
        double a = 0.000104;
        double b = 0.182258;
        double c = -4.60 - vdot;
        double disc = b * b - 4 * a * c;
        if (disc < 0) {
            return 0;
        }
        return (-b + Math.sqrt(disc)) / (2 * a);
    }

    /** 由 VDOT 推算目标距离（米）的等效完赛时间（秒）；无法推算返回 0 */
    public static long equivalentTimeSec(double vdot, double distanceM) {
        double vvo2 = vvo2maxSpeed(vdot);
        if (vvo2 <= 0 || distanceM <= 0) {
            return 0;
        }
        double t = distanceM / (vvo2 * 0.90) * 60; // 初值
        for (int i = 0; i < 4; i++) {
            double pct = pctOfVo2max(t);
            double speed = vvo2 * pct;   // m/min
            if (speed <= 0) {
                return 0;
            }
            t = distanceM / speed * 60;  // 秒
        }
        return Math.round(t);
    }

    /** %vVO2max 强度 → 配速（秒/公里）。pct 为占 vVO2max 百分比（含小数） */
    public static int paceSecKm(double pct, double vvo2maxMPerMin) {
        double speed = pct * vvo2maxMPerMin;
        if (speed <= 0) {
            return 0;
        }
        return (int) Math.round(60000.0 / speed);
    }
}
