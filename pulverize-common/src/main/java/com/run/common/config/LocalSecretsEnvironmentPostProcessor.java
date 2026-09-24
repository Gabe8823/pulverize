package com.run.common.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

/**
 * 本地密钥装载器（本地 keystore 约定）。
 *
 * 优先级：环境变量 > 本机密钥文件 > yml 占位默认值
 * - jwt.secret               &lt;- RUNAI_JWT_SECRET，否则 ~/.pulverize/jwt.secret（缺失则随机生成并落盘）
 * - spring.datasource.password &lt;- RUNAI_DB_PASSWORD，否则 ~/.pulverize/db.password（缺失则沿用 yml 默认）
 *
 * 密钥文件位于用户主目录（仓库之外），永不进入版本库、构建产物与日志。
 * 注册位置：META-INF/spring.factories。
 */
public class LocalSecretsEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String DIR = System.getProperty("user.home") + "/.pulverize";
    private static final String JWT_ENV = "RUNAI_JWT_SECRET";
    private static final String JWT_FILE = "jwt.secret";
    private static final String DB_ENV = "RUNAI_DB_PASSWORD";
    private static final String DB_FILE = "db.password";
    /** HS256 要求密钥至少 32 字节 */
    private static final int JWT_MIN_LEN = 32;

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> overrides = new HashMap<>();

        String jwt = resolve(environment, JWT_ENV, JWT_FILE, true);
        if (jwt != null && jwt.length() >= JWT_MIN_LEN) {
            overrides.put("jwt.secret", jwt);
        }

        String db = resolve(environment, DB_ENV, DB_FILE, false);
        if (db != null && !db.isBlank()) {
            overrides.put("spring.datasource.password", db);
        }

        if (!overrides.isEmpty()) {
            environment.getPropertySources().addFirst(new MapPropertySource("localSecrets", overrides));
        }
    }

    /** 环境变量优先；否则读本机文件；autoCreate 时文件缺失则生成随机密钥并写回 */
    private String resolve(ConfigurableEnvironment env, String envVar, String fileName, boolean autoCreate) {
        String fromEnv = env.getProperty(envVar);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv.trim();
        }
        Path file = Path.of(DIR, fileName);
        try {
            if (Files.exists(file)) {
                String value = Files.readString(file).trim();
                if (!value.isEmpty()) {
                    return value;
                }
            }
            if (autoCreate) {
                Files.createDirectories(file.getParent());
                String generated = randomHex(48);
                Files.writeString(file, generated);
                return generated;
            }
        } catch (IOException e) {
            // 读写失败：回退 yml 默认值，保证应用可启动
        }
        return null;
    }

    private static String randomHex(int bytes) {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(bytes * 2);
        for (int i = 0; i < bytes; i++) {
            sb.append(String.format("%02x", random.nextInt(256)));
        }
        return sb.toString();
    }

    @Override
    public int getOrder() {
        // ConfigDataEnvironmentPostProcessor 为 HIGHEST_PRECEDENCE + 10，此处稍后执行以覆盖 yml 解析结果
        return Ordered.HIGHEST_PRECEDENCE + 20;
    }
}
