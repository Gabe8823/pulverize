package com.run.common.config;

import org.springframework.context.annotation.Configuration;

/**
 * 缓存配置占位。
 * Redis可用时 RedisConfig 生效，不可用时 UserServiceImpl 通过 @Autowired(required=false) 优雅降级。
 */
@Configuration
public class CacheConfig {
}
