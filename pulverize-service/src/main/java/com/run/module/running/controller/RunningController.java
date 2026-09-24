package com.run.module.running.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.run.common.result.R;
import com.run.module.running.dto.*;
import com.run.module.running.entity.RunningActivity;
import com.run.module.running.service.RunningService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/running")
@RequiredArgsConstructor
public class RunningController {

    private final RunningService runningService;

    /** 新增跑步记录 */
    @PostMapping("/activities")
    public R<RunningActivity> createActivity(HttpServletRequest request,
                                              @Valid @RequestBody ActivityCreateDTO dto) {
        Long userId = (Long) request.getAttribute("userId");
        RunningActivity activity = runningService.createActivity(userId, dto);
        return R.ok("添加成功", activity);
    }

    /** 跑步记录列表（分页） */
    @GetMapping("/activities")
    public R<IPage<RunningActivity>> listActivities(HttpServletRequest request,
                                                     ActivityQueryDTO query) {
        Long userId = (Long) request.getAttribute("userId");
        IPage<RunningActivity> page = runningService.listActivities(userId, query);
        return R.ok(page);
    }

    /** 跑步详情 */
    @GetMapping("/activities/{id}")
    public R<ActivityDetailVO> getActivityDetail(HttpServletRequest request,
                                                   @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("userId");
        ActivityDetailVO detail = runningService.getActivityDetail(userId, id);
        return R.ok(detail);
    }

    /** 删除跑步记录 */
    @DeleteMapping("/activities/{id}")
    public R<Void> deleteActivity(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("userId");
        runningService.deleteActivity(userId, id);
        return R.ok("删除成功", null);
    }

    /** 本周统计 */
    @GetMapping("/stats/weekly")
    public R<WeeklyStatsVO> getWeeklyStats(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(runningService.getWeeklyStats(userId));
    }

    /** 全量统计 */
    @GetMapping("/stats/all")
    public R<WeeklyStatsVO> getAllTimeStats(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(runningService.getAllTimeStats(userId));
    }
}
