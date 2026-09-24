package com.run.module.platform.spi;

import com.run.common.exception.BizException;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 平台连接器注册表：收集所有 PlatformConnector Bean，按平台标识路由
 */
@Component
public class PlatformConnectorRegistry {

    private final Map<String, PlatformConnector> connectors = new LinkedHashMap<>();

    public PlatformConnectorRegistry(List<PlatformConnector> connectorList) {
        for (PlatformConnector connector : connectorList) {
            connectors.put(connector.platform().toUpperCase(Locale.ROOT), connector);
        }
    }

    /** 所有已注册连接器（含未接入的占位平台） */
    public List<PlatformConnector> all() {
        return List.copyOf(connectors.values());
    }

    /** 获取连接器，不存在时抛出业务异常 */
    public PlatformConnector get(String platform) {
        PlatformConnector connector = connectors.get(platform == null ? "" : platform.toUpperCase(Locale.ROOT));
        if (connector == null) {
            throw new BizException("暂不支持的运动平台: " + platform);
        }
        if (!connector.available()) {
            throw new BizException(connector.displayName() + " 即将支持，敬请期待");
        }
        return connector;
    }
}
