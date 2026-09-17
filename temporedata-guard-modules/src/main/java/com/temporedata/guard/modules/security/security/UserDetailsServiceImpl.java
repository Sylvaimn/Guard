/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.temporedata.guard.api.base.exceptions.BusinessException
 *  com.temporedata.guard.security.LoginUser
 *  org.springframework.security.authentication.DisabledException
 *  org.springframework.security.core.userdetails.UserDetailsService
 *  org.springframework.security.core.userdetails.UsernameNotFoundException
 *  org.springframework.stereotype.Service
 */
package com.temporedata.guard.modules.security.security;

import com.temporedata.guard.api.base.exceptions.BusinessException;
import com.temporedata.guard.modules.platform.user.entity.UserEntity;
import com.temporedata.guard.modules.platform.user.repository.UserRepository;
import com.temporedata.guard.security.LoginUser;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl
implements UserDetailsService {
    private final UserRepository userRepository;

    public LoginUser loadUserByUsername(String username) throws UsernameNotFoundException {
        UserEntity user = this.userRepository.findByUsername(username).orElseThrow(() -> new BusinessException("\u7528\u6237\u540d\u6216\u5bc6\u7801\u9519\u8bef"));
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new DisabledException("\u8d26\u53f7\u5df2\u505c\u7528\uff0c\u8bf7\u8054\u7cfb\u7ba1\u7406\u5458");
        }
        return new LoginUser(user.getId(), user.getUsername(), user.getPassword(), user.getTenantId(), user.getDensity(), user.getRoleCodes(), user.getStatus() == null || user.getStatus() == 1);
    }

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
}

