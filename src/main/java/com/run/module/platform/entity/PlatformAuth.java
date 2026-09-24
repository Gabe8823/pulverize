package com.run.module.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_user_platform_auth")
public class PlatformAuth {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String platform;

    private String platformUserId;

    private String accessToken;

    private String refreshToken;

    private LocalDateTime tokenExpireTime;

    private Integer syncStatus;

    private LocalDateTime lastSyncTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
