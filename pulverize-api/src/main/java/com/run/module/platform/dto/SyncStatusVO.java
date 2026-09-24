package com.run.module.platform.dto;

import lombok.Data;

/** 同步状态响应 */
@Data
public class SyncStatusVO {

    /** 0-未绑定 1-同步中 2-同步完成 3-同步失败 */
    private Integer status;
    private String message;
    private Integer syncedCount;
    private String lastSyncTime;

    /** 是否已绑定 */
    private Boolean bound = false;
}
