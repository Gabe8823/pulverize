package com.run.module.analysis.service;

import com.run.module.analysis.dto.AnalysisResultVO;
import com.run.module.analysis.dto.VdotVO;

public interface AnalysisService {

    /** 分析单次跑步活动，返回多维度分析结果 */
    AnalysisResultVO analyzeActivity(Long userId, Long activityId);

    /** 获取已存储的分析结果 */
    AnalysisResultVO getAnalysis(Long userId, Long activityId);

    /** 跑力指数 VDOT：基于全部历史数据中最优有氧表现推算 */
    VdotVO getVdot(Long userId);
}
