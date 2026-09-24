package com.run.module.platform.connector;

import com.run.common.exception.BizException;
import com.run.module.platform.dto.PlatformBindRequest;
import com.run.module.platform.dto.SyncStatusVO;
import com.run.module.platform.spi.PlatformConnector;
import org.springframework.stereotype.Component;

// 占位平台通过内部静态 @Component 类注册

/**
 * 未接入平台的占位连接器（仅用于前端"即将支持"展示）
 *
 * 后续接入真实 API 时，将 available() 改为 true 并实现四个方法，
 * 同时从 UPCOMING 占位清单中移除即可。
 */
public abstract class AbstractUpcomingConnector implements PlatformConnector {

    @Override
    public boolean available() {
        return false;
    }

    @Override
    public SyncStatusVO bind(Long userId, PlatformBindRequest request) {
        throw new BizException(displayName() + " 即将支持，敬请期待");
    }

    @Override
    public SyncStatusVO sync(Long userId) {
        throw new BizException(displayName() + " 即将支持，敬请期待");
    }

    @Override
    public SyncStatusVO status(Long userId) {
        SyncStatusVO vo = new SyncStatusVO();
        vo.setStatus(0);
        vo.setBound(false);
        vo.setSyncedCount(0);
        vo.setMessage(displayName() + " 即将支持");
        return vo;
    }

    @Override
    public void unbind(Long userId) {
        throw new BizException(displayName() + " 即将支持，敬请期待");
    }

    @Component
    public static class StravaConnector extends AbstractUpcomingConnector {
        @Override public String platform() { return "STRAVA"; }
        @Override public String displayName() { return "Strava"; }
        @Override public String description() { return "全球跑步骑行社区，支持官方开放平台 OAuth 授权。"; }
    }

    @Component
    public static class GarminConnector extends AbstractUpcomingConnector {
        @Override public String platform() { return "GARMIN"; }
        @Override public String displayName() { return "Garmin 佳明"; }
        @Override public String description() { return "佳明 Connect 训练与健康数据同步。"; }
    }

    @Component
    public static class AppleHealthConnector extends AbstractUpcomingConnector {
        @Override public String platform() { return "APPLE_HEALTH"; }
        @Override public String displayName() { return "Apple 健康"; }
        @Override public String description() { return "iOS 健康 App 中的跑步与体能训练数据。"; }
    }

    @Component
    public static class HuaweiHealthConnector extends AbstractUpcomingConnector {
        @Override public String platform() { return "HUAWEI"; }
        @Override public String displayName() { return "华为运动健康"; }
        @Override public String description() { return "华为运动健康 App 运动数据同步。"; }
    }
}
