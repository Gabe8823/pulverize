package com.run.module.report.service;

import com.run.module.report.dto.ReportQueryDTO;
import com.run.module.report.dto.ReportVO;

public interface ReportService {

    /**
     * 生成训练报告（周期解析 + 汇总 + 分桶 + AI 报告文案，AI 失败自动规则兜底）
     *
     * @param userId 用户 ID
     * @param q      查询参数（granularity 必填）
     */
    ReportVO generate(Long userId, ReportQueryDTO q);
}
