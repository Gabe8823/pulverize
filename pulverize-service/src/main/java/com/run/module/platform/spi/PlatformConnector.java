package com.run.module.platform.spi;

import com.run.module.platform.dto.PlatformBindRequest;
import com.run.module.platform.dto.SyncStatusVO;

/**
 * 第三方运动平台连接器 SPI
 *
 * 新增平台接入步骤:
 * 1. 实现本接口（bind / sync / status / unbind）
 * 2. 注册为 Spring Bean（@Component），自动被 PlatformConnectorRegistry 收集
 *
 * 当前已实现: COROS(高驰)
 * 规划中: STRAVA / GARMIN / APPLE_HEALTH / HUAWEI
 */
public interface PlatformConnector {

    /** 平台唯一标识（大写），如 COROS */
    String platform();

    /** 展示名称，如 "高驰 COROS" */
    String displayName();

    /** 平台描述，用于前端卡片文案 */
    String description();

    /** 是否已接入（未接入的平台仅在前端展示"即将支持"） */
    boolean available();

    /** 绑定平台账号，绑定成功返回状态 */
    SyncStatusVO bind(Long userId, PlatformBindRequest request);

    /** 从平台拉取数据同步到本地 */
    SyncStatusVO sync(Long userId);

    /** 查询当前用户的绑定与同步状态 */
    SyncStatusVO status(Long userId);

    /** 解绑平台账号（本地保留已同步数据） */
    void unbind(Long userId);
}
