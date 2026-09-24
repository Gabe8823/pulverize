package com.run.common.interceptor;

import com.run.common.exception.BizException;
import com.run.common.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class JwtAuthInterceptor implements HandlerInterceptor {

    private final JwtUtils jwtUtils;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // OPTIONS 请求放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String header = request.getHeader("Authorization");
        if (!StringUtils.hasText(header) || !header.startsWith("Bearer ")) {
            throw new BizException(401, "未登录或Token已过期");
        }

        String token = header.substring(7);
        if (!jwtUtils.validateToken(token)) {
            throw new BizException(401, "Token无效或已过期");
        }

        // 将用户信息放入request属性，后续Controller可获取
        Long userId = jwtUtils.getUserId(token);
        String username = jwtUtils.getUsername(token);
        request.setAttribute("userId", userId);
        request.setAttribute("username", username);

        return true;
    }
}
