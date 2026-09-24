package com.run.module.user.controller;

import com.run.common.result.R;
import com.run.module.user.dto.LoginRequest;
import com.run.module.user.dto.LoginResponse;
import com.run.module.user.dto.RegisterRequest;
import com.run.module.user.dto.UserDTO;
import com.run.module.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 用户注册
     */
    @PostMapping("/auth/register")
    public R<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        LoginResponse response = userService.register(request);
        return R.ok("注册成功", response);
    }

    /**
     * 用户登录
     */
    @PostMapping("/auth/login")
    public R<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = userService.login(request);
        return R.ok("登录成功", response);
    }

    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/user/profile")
    public R<UserDTO> getProfile(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        UserDTO user = userService.getUserInfo(userId);
        return R.ok(user);
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/user/profile")
    public R<UserDTO> updateProfile(HttpServletRequest request, @RequestBody UserDTO userDTO) {
        Long userId = (Long) request.getAttribute("userId");
        UserDTO updated = userService.updateUserInfo(userId, userDTO);
        return R.ok("更新成功", updated);
    }
}
