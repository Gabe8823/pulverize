package com.run.module.platform.dto;

import lombok.Data;

/**
 * 平台连接状态（前端"连接"页卡片数据）
 */
@Data
public class PlatformStatusVO {

    /** 平台标识，如 COROS */
    private String platform;

    /** 展示名称 */
    private String name;

    /** 描述文案 */
    private String description;

    /** 是否已接入（false 仅展示占位） */
    private Boolean available;

    /** 是否已绑定 */
    private Boolean bound;

    /** 0-未绑定 1-同步中 2-同步完成 3-同步失败 */
    private Integer syncStatus;

    /** 状态描述 */
    private String message;

    /** 已同步记录数 */
    private Integer syncedCount;

    /** 上次同步时间 */
    private String lastSyncTime;
}
