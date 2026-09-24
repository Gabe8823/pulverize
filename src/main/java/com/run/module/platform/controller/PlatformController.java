package com.run.module.platform.controller;

import com.run.common.result.R;
import com.run.module.platform.dto.*;
import com.run.module.platform.service.PlatformService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 运动平台连接管理
 *
 * 通用接口按平台标识路由（如 /platform/COROS/bind），
 * /platform/coros/* 为兼容旧版前端的保留接口。
 */
@RestController
@RequestMapping("/platform")
@RequiredArgsConstructor
public class PlatformController {

    private final PlatformService platformService;

    /** 所有平台的连接状态（连接页卡片数据） */
    @GetMapping("/list")
    public R<List<PlatformStatusVO>> listPlatforms(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(platformService.listPlatforms(userId));
    }

    /** 绑定平台账号 */
    @PostMapping("/{platform}/bind")
    public R<SyncStatusVO> bind(HttpServletRequest request,
                                 @PathVariable String platform,
                                 @Valid @RequestBody PlatformBindRequest bindRequest) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok("绑定成功", platformService.bind(userId, platform, bindRequest));
    }

    /** 同步平台数据 */
    @PostMapping("/{platform}/sync")
    public R<SyncStatusVO> sync(HttpServletRequest request, @PathVariable String platform) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(platformService.sync(userId, platform));
    }

    /** 查询平台同步状态 */
    @GetMapping("/{platform}/status")
    public R<SyncStatusVO> status(HttpServletRequest request, @PathVariable String platform) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(platformService.status(userId, platform));
    }

    /** 解绑平台账号 */
    @DeleteMapping("/{platform}/unbind")
    public R<Void> unbind(HttpServletRequest request, @PathVariable String platform) {
        Long userId = (Long) request.getAttribute("userId");
        platformService.unbind(userId, platform);
        return R.ok("解绑成功", null);
    }

    // ==================== 兼容旧接口 ====================

    /** 绑定高驰账号 */
    @PostMapping("/coros/bind")
    public R<SyncStatusVO> bindCoros(HttpServletRequest request,
                                      @Valid @RequestBody CorosLoginRequest loginRequest) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok("绑定成功", platformService.bindCoros(userId, loginRequest));
    }

    /** 同步高驰数据 */
    @PostMapping("/coros/sync")
    public R<SyncStatusVO> syncCorosData(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(platformService.syncCorosData(userId));
    }

    /** 获取高驰同步状态 */
    @GetMapping("/coros/status")
    public R<SyncStatusVO> getSyncStatus(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(platformService.getSyncStatus(userId));
    }

    /** 解绑高驰账号 */
    @DeleteMapping("/coros/unbind")
    public R<Void> unbindCoros(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        platformService.unbindCoros(userId);
        return R.ok("解绑成功", null);
    }
}
