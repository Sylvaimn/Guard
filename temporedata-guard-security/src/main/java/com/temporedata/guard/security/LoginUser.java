package com.temporedata.guard.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Spring Security 认证主体（登录或 JWT claims 构建）。
 */
public class LoginUser implements UserDetails {

    private final String userId;
    private final String username;
    private final String password;
    private final String tenantId;
    private final String density;
    private final Set<String> roleCodes;
    private final boolean enabled;

    public LoginUser(String userId, String username, String password, String tenantId, String density,
                     Set<String> roleCodes, boolean enabled) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.tenantId = tenantId;
        this.density = density;
        this.roleCodes = roleCodes;
        this.enabled = enabled;
    }

    public String getUserId() {
        return userId;
    }

    public String getTenantId() {
        return tenantId;
    }

    /** 人员密级（数据访问行级过滤依据） */
    public String getDensity() {
        return density;
    }

    public Set<String> getRoleCodes() {
        return roleCodes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roleCodes.stream()
                .map(code -> new SimpleGrantedAuthority("ROLE_" + code))
                .collect(Collectors.toSet());
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
