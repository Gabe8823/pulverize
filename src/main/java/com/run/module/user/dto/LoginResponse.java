package com.run.module.user.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {

    private Long userId;
    private String username;
    private String nickname;
    private String avatarUrl;
    private String token;
    private String refreshToken;
}
