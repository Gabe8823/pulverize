package com.run.module.user.service;

import com.run.module.user.dto.LoginRequest;
import com.run.module.user.dto.LoginResponse;
import com.run.module.user.dto.RegisterRequest;
import com.run.module.user.dto.UserDTO;

public interface UserService {

    LoginResponse register(RegisterRequest request);

    LoginResponse login(LoginRequest request);

    UserDTO getUserInfo(Long userId);

    UserDTO updateUserInfo(Long userId, UserDTO userDTO);

    /** 从高驰(COROS)同步身体数据到本地用户资料，返回最新资料 */
    UserDTO syncCorosProfile(Long userId);
}
