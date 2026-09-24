package com.run.module.report.controller;

import com.run.common.result.R;
import com.run.module.report.dto.ReportQueryDTO;
import com.run.module.report.dto.ReportVO;
import com.run.module.report.service.ReportService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/report")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    /** 生成训练报告 */
    @PostMapping("/generate")
    public R<ReportVO> generate(HttpServletRequest request, @RequestBody ReportQueryDTO q) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(reportService.generate(userId, q));
    }
}
