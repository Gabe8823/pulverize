package com.run.module.platform.service.impl;

import com.run.module.platform.dto.*;
import com.run.module.platform.service.PlatformService;
import com.run.module.platform.spi.PlatformConnector;
import com.run.module.platform.spi.PlatformConnectorRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 平台服务门面：通过连接器注册表路由到具体平台实现
 */
@Service
@RequiredArgsConstructor
public class PlatformServiceImpl implements PlatformService {

    private final PlatformConnectorRegistry registry;

    @Override
    public List<PlatformStatusVO> listPlatforms(Long userId) {
        List<PlatformStatusVO> result = new ArrayList<>();
        for (PlatformConnector connector : registry.all()) {
            PlatformStatusVO vo = new PlatformStatusVO();
            vo.setPlatform(connector.platform());
            vo.setName(connector.displayName());
            vo.setDescription(connector.description());
            vo.setAvailable(connector.available());

            if (connector.available()) {
                SyncStatusVO status = connector.status(userId);
                vo.setBound(status.getBound());
                vo.setSyncStatus(status.getStatus());
                vo.setMessage(status.getMessage());
                vo.setSyncedCount(status.getSyncedCount());
                vo.setLastSyncTime(status.getLastSyncTime());
            } else {
                vo.setBound(false);
                vo.setSyncStatus(0);
                vo.setMessage(connector.displayName() + " 即将支持");
                vo.setSyncedCount(0);
            }
            result.add(vo);
        }
        return result;
    }

    @Override
    public SyncStatusVO bind(Long userId, String platform, PlatformBindRequest request) {
        return registry.get(platform).bind(userId, request);
    }

    @Override
    public SyncStatusVO sync(Long userId, String platform) {
        return registry.get(platform).sync(userId);
    }

    @Override
    public SyncStatusVO status(Long userId, String platform) {
        return registry.get(platform).status(userId);
    }

    @Override
    public void unbind(Long userId, String platform) {
        registry.get(platform).unbind(userId);
    }

    // ==================== 兼容旧接口（高驰） ====================

    @Override
    public SyncStatusVO bindCoros(Long userId, CorosLoginRequest request) {
        PlatformBindRequest bindRequest = new PlatformBindRequest();
        bindRequest.setAccount(request.getEmail());
        bindRequest.setPassword(request.getPassword());
        bindRequest.setRegion(request.getRegion());
        return bind(userId, "COROS", bindRequest);
    }

    @Override
    public SyncStatusVO syncCorosData(Long userId) {
        return sync(userId, "COROS");
    }

    @Override
    public SyncStatusVO getSyncStatus(Long userId) {
        return status(userId, "COROS");
    }

    @Override
    public void unbindCoros(Long userId) {
        unbind(userId, "COROS");
    }
}
