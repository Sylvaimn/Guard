package com.temporedata.guard.modules.platform.auth.service;

import com.temporedata.guard.api.auth.LoginReq;
import com.temporedata.guard.api.auth.LoginRes;
import com.temporedata.guard.api.auth.UserInfoRes;
import com.temporedata.guard.api.base.exceptions.BusinessException;
import com.temporedata.guard.security.LoginUser;
import com.temporedata.guard.modules.platform.tenant.repository.TenantRepository;
import com.temporedata.guard.modules.platform.user.entity.UserEntity;
import com.temporedata.guard.modules.platform.user.repository.UserRepository;
import com.temporedata.guard.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

/**
 * Authentication business logic: verify credentials and issue a JWT.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;

    private final JwtTokenProvider tokenProvider;

    private final TenantRepository tenantRepository;

    private final UserRepository userRepository;

    private final org.springframework.beans.factory.ObjectProvider<StringRedisTemplate> redisProvider;

    private final PasswordEncoder passwordEncoder;

    private static final String FAIL_KEY = "zy:login:fail:";
    private static final int MAX_FAILS = 5;
    private static final long LOCK_SECONDS = 300;

    public LoginRes login(LoginReq req) {
        String username = req.getUsername() == null ? "" : req.getUsername().trim();

        // 登录失败限流(Redis 计数; Redis 不可用时降级为无锁)
        if (isLocked(username)) {
            throw new BusinessException("失败次数过多，账号已锁定5分钟，请稍后再试");
        }

        // 提前校验账号状态：停用账号禁止登录（不依赖 Security 异常机制，保证提示准确）
        userRepository.findByUsername(username)
                .filter(u -> u.getStatus() != null && u.getStatus() == 0)
                .ifPresent(u -> { throw new BusinessException("账号已停用，请联系管理员"); });

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, req.getPassword()));
        } catch (Exception e) {
            recordFailure(username);
            throw new BusinessException("用户名或密码错误");
        }
        clearFailure(username);
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        String token = tokenProvider.generateToken(loginUser.getUserId(), loginUser.getTenantId(),
                loginUser.getUsername(), loginUser.getDensity(), loginUser.getRoleCodes());

        String tenantName = tenantRepository.findById(loginUser.getTenantId())
                .map(t -> t.getName())
                .orElse("");

        Boolean mustChange = userRepository.findById(loginUser.getUserId())
                .map(u -> u.getMustChangePassword()).orElse(false);
        UserInfoRes user = new UserInfoRes(loginUser.getUserId(), loginUser.getUsername(),
                loginUser.getTenantId(), tenantName, new ArrayList<>(loginUser.getRoleCodes()),
                loginUser.getDensity(), mustChange);
        return new LoginRes(token, user);
    }

    private StringRedisTemplate redis() {
        try {
            return redisProvider.getIfAvailable();
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isLocked(String username) {
        StringRedisTemplate redis = redis();
        if (redis == null) return false;
        try {
            String v = redis.opsForValue().get(FAIL_KEY + username);
            return v != null && Integer.parseInt(v) >= MAX_FAILS;
        } catch (Exception e) {
            return false;
        }
    }

    private void recordFailure(String username) {
        StringRedisTemplate redis = redis();
        if (redis == null || username.isBlank()) return;
        try {
            Long count = redis.opsForValue().increment(FAIL_KEY + username);
            if (count != null && count == 1) {
                redis.expire(FAIL_KEY + username, LOCK_SECONDS, java.util.concurrent.TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            // Redis 不可用, 降级不记录
        }
    }

    private void clearFailure(String username) {
        StringRedisTemplate redis = redis();
        if (redis == null || username.isBlank()) return;
        try {
            redis.delete(FAIL_KEY + username);
        } catch (Exception e) {
            // ignore
        }
    }

    /** 修改密码: 校验旧密码, 更新后清除强制改密标记 */
    @org.springframework.transaction.annotation.Transactional
    public void changePassword(String username, String oldPassword, String newPassword) {
        if (newPassword == null || newPassword.length() < 8) {
            throw new BusinessException("新密码长度至少8位");
        }
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException("旧密码错误");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);
        userRepository.save(user);
    }
}