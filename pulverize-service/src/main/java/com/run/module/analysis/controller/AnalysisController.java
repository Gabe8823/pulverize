package com.run.module.analysis.controller;

import com.run.common.result.R;
import com.run.module.analysis.dto.AnalysisResultVO;
import com.run.module.analysis.dto.MuscleMapVO;
import com.run.module.analysis.dto.VdotVO;
import com.run.module.analysis.service.AnalysisService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;

    /** 分析跑步活动 */
    @PostMapping("/activities/{id}")
    public R<AnalysisResultVO> analyzeActivity(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("userId");
        AnalysisResultVO result = analysisService.analyzeActivity(userId, id);
        return R.ok("分析完成", result);
    }

    /** 获取分析结果 */
    @GetMapping("/activities/{id}")
    public R<AnalysisResultVO> getAnalysis(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("userId");
        AnalysisResultVO result = analysisService.getAnalysis(userId, id);
        return R.ok(result);
    }

    /** 跑力指数 VDOT（基于全部历史数据中的最优表现推算） */
    @GetMapping("/vdot")
    public R<VdotVO> getVdot(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(analysisService.getVdot(userId));
    }

    /** 肌肉热力图：近 N 天肌群负荷估算（运动类型 × 时长 × 强度，参考高驰 App） */
    @GetMapping("/muscle-map")
    public R<MuscleMapVO> muscleMap(HttpServletRequest request,
                                    @RequestParam(name = "days", defaultValue = "28") Integer days) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(analysisService.getMuscleMap(userId, days));
    }
}
