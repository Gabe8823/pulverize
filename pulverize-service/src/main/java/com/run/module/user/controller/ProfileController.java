package com.run.module.user.controller;

import com.run.common.result.R;
import com.run.module.user.dto.UserDTO;
import com.run.module.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 个人资料相关接口（与 UserController 的 /auth、/user 路径分开）
 * 最终 URL: /api/profile/sync-coros（context-path=/api）
 */
@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserService userService;

    /**
     * 从高驰(COROS)同步身体数据（身高/体重/最大心率/静息心率/生日），
     * 返回与 GET /user/profile 相同的用户资料结构
     */
    @PostMapping("/sync-coros")
    public R<UserDTO> syncCoros(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        UserDTO user = userService.syncCorosProfile(userId);
        return R.ok("同步成功", user);
    }
}
