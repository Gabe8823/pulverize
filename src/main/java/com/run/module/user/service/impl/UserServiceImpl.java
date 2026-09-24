package com.run.module.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.run.common.exception.BizException;
import com.run.common.utils.JwtUtils;
import com.run.module.platform.client.CorosApiClient;
import com.run.module.platform.entity.PlatformAuth;
import com.run.module.platform.mapper.PlatformAuthMapper;
import com.run.module.user.dto.LoginRequest;
import com.run.module.user.dto.LoginResponse;
import com.run.module.user.dto.RegisterRequest;
import com.run.module.user.dto.UserDTO;
import com.run.module.user.entity.User;
import com.run.module.user.mapper.UserMapper;
import com.run.module.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final String PLATFORM_COROS = "COROS";

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final PlatformAuthMapper platformAuthMapper;
    private final CorosApiClient corosApiClient;

    /** Redis可选注入，开发环境无Redis时自动为null */
    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public LoginResponse register(RegisterRequest request) {
        long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername()));
        if (count > 0) {
            throw new BizException("用户名已存在");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname() != null ? request.getNickname() : request.getUsername());
        user.setGender(0);
        user.setStatus(1);
        user.setDeleted(0);
        userMapper.insert(user);

        return buildLoginResponse(user);
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername()));
        if (user == null) {
            throw new BizException("用户名或密码错误");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BizException("用户名或密码错误");
        }
        if (user.getStatus() != 1) {
            throw new BizException("账号已被禁用");
        }
        return buildLoginResponse(user);
    }

    @Override
    public UserDTO getUserInfo(Long userId) {
        // 先尝试Redis缓存
        if (redisTemplate != null) {
            String cacheKey = "user:info:" + userId;
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof UserDTO dto) {
                return dto;
            }
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException("用户不存在");
        }

        UserDTO dto = toDTO(user);

        // 写入缓存
        if (redisTemplate != null) {
            redisTemplate.opsForValue().set("user:info:" + userId, dto, 30, TimeUnit.MINUTES);
        }
        return dto;
    }

    @Override
    public UserDTO updateUserInfo(Long userId, UserDTO userDTO) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException("用户不存在");
        }

        if (userDTO.getNickname() != null) user.setNickname(userDTO.getNickname());
        if (userDTO.getEmail() != null) user.setEmail(userDTO.getEmail());
        if (userDTO.getPhone() != null) user.setPhone(userDTO.getPhone());
        if (userDTO.getGender() != null) user.setGender(userDTO.getGender());
        if (userDTO.getBirthday() != null) user.setBirthday(userDTO.getBirthday());
        if (userDTO.getHeightCm() != null) user.setHeightCm(userDTO.getHeightCm());
        if (userDTO.getWeightKg() != null) user.setWeightKg(userDTO.getWeightKg());
        if (userDTO.getMaxHeartRate() != null) user.setMaxHeartRate(userDTO.getMaxHeartRate());
        if (userDTO.getRestHeartRate() != null) user.setRestHeartRate(userDTO.getRestHeartRate());

        userMapper.updateById(user);

        // 清除缓存
        if (redisTemplate != null) {
            redisTemplate.delete("user:info:" + userId);
        }
        return toDTO(user);
    }

    /**
     * 从高驰(COROS)同步身体数据: stature->身高 weight->体重 maxHr->最大心率
     * rhr->静息心率 birthday(yyyyMMdd)->生日 昵称仅在本地为空时填充；性别不可靠，跳过。
     * 复用 updateUserInfo（只写非空字段 + 清 Redis 缓存），返回与 GET /user/profile 同结构。
     */
    @Override
    public UserDTO syncCorosProfile(Long userId) {
        PlatformAuth auth = platformAuthMapper.selectOne(
                new LambdaQueryWrapper<PlatformAuth>()
                        .eq(PlatformAuth::getUserId, userId)
                        .eq(PlatformAuth::getPlatform, PLATFORM_COROS));
        if (auth == null || auth.getAccessToken() == null || auth.getAccessToken().isBlank()) {
            throw new BizException("请先连接高驰账号");
        }

        Map<String, Object> data = corosApiClient.getAccount(auth.getAccessToken());
        if (data == null || data.isEmpty()) {
            throw new BizException("获取高驰账户信息失败");
        }

        UserDTO patch = new UserDTO();
        Double stature = toDouble(data.get("stature"));
        if (stature != null) patch.setHeightCm(stature);
        Double weight = toDouble(data.get("weight"));
        if (weight != null) patch.setWeightKg(weight);
        Integer maxHr = toIntValue(data.get("maxHr"));
        if (maxHr != null) patch.setMaxHeartRate(maxHr);
        Integer rhr = toIntValue(data.get("rhr"));
        if (rhr != null) patch.setRestHeartRate(rhr);
        LocalDate birthday = parseBirthday(data.get("birthday"));
        if (birthday != null) patch.setBirthday(birthday);

        // 昵称不强制覆盖，仅本地为空时填充
        Object corosNickname = data.get("nickname");
        String localNickname = getUserInfo(userId).getNickname();
        if ((localNickname == null || localNickname.isBlank())
                && corosNickname != null && !String.valueOf(corosNickname).isBlank()) {
            patch.setNickname(String.valueOf(corosNickname));
        }
        // 性别(sex/userProfile.gender)不可靠，跳过
        return updateUserInfo(userId, patch);
    }

    private Double toDouble(Object v) {
        if (v instanceof Number n) return n.doubleValue();
        if (v == null) return null;
        try {
            return Double.parseDouble(String.valueOf(v).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer toIntValue(Object v) {
        if (v instanceof Number n) return n.intValue();
        if (v == null) return null;
        try {
            return (int) Double.parseDouble(String.valueOf(v).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 高驰 birthday 数字 20031208 -> LocalDate，非法值返回 null */
    private LocalDate parseBirthday(Object v) {
        if (v == null) return null;
        String s = String.valueOf(v).trim();
        if (s.length() != 8 || !s.chars().allMatch(Character::isDigit)) return null;
        try {
            return LocalDate.parse(s, DateTimeFormatter.BASIC_ISO_DATE);
        } catch (Exception e) {
            return null;
        }
    }

    private LoginResponse buildLoginResponse(User user) {
        String token = jwtUtils.generateToken(user.getId(), user.getUsername());
        String refreshToken = jwtUtils.generateRefreshToken(user.getId(), user.getUsername());

        // 存储refreshToken
        if (redisTemplate != null) {
            redisTemplate.opsForValue().set(
                    "refresh:token:" + user.getId(), refreshToken, 7, TimeUnit.DAYS);
        }

        return LoginResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .token(token)
                .refreshToken(refreshToken)
                .build();
    }

    private UserDTO toDTO(User user) {
        UserDTO dto = new UserDTO();
        BeanUtils.copyProperties(user, dto);
        return dto;
    }
}
