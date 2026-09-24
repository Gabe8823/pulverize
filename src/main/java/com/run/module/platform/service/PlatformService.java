package com.run.module.platform.service;

import com.run.module.platform.dto.*;

import java.util.List;

public interface PlatformService {

    /** 当前用户所有平台的连接状态（含未接入平台占位） */
    List<PlatformStatusVO> listPlatforms(Long userId);

    /** 绑定平台账号 */
    SyncStatusVO bind(Long userId, String platform, PlatformBindRequest request);

    /** 同步平台数据到本地 */
    SyncStatusVO sync(Long userId, String platform);

    /** 查询单个平台同步状态 */
    SyncStatusVO status(Long userId, String platform);

    /** 解绑平台账号 */
    void unbind(Long userId, String platform);

    // ==================== 兼容旧接口（高驰） ====================

    /** 绑定高驰账号 */
    SyncStatusVO bindCoros(Long userId, CorosLoginRequest request);

    /** 同步高驰数据到本地 */
    SyncStatusVO syncCorosData(Long userId);

    /** 获取高驰同步状态 */
    SyncStatusVO getSyncStatus(Long userId);

    /** 解绑高驰账号 */
    void unbindCoros(Long userId);
}
