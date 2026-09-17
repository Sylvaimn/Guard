package com.temporedata.guard.modules.common.bootstrap;

import com.temporedata.guard.modules.platform.role.entity.RoleEntity;
import com.temporedata.guard.modules.platform.role.repository.RoleRepository;
import com.temporedata.guard.modules.platform.tenant.entity.TenantEntity;
import com.temporedata.guard.modules.platform.tenant.repository.TenantRepository;
import com.temporedata.guard.modules.platform.user.entity.UserEntity;
import com.temporedata.guard.modules.platform.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;

/**
 * Seeds default tenant, roles and admin account on first startup.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final TenantRepository tenantRepository;

    private final RoleRepository roleRepository;

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        TenantEntity tenant = tenantRepository.findAll().stream().findFirst().orElseGet(() -> {
            TenantEntity t = new TenantEntity();
            t.setId(UUID.randomUUID().toString().replace("-", ""));
            t.setName("默认租户");
            t.setRemark("P0 初始化租户");
            t.setCreateTime(LocalDateTime.now());
            return tenantRepository.save(t);
        });

        RoleEntity adminRole = roleRepository.findByCode("ADMIN").orElseGet(() -> {
            RoleEntity r = new RoleEntity();
            r.setId(UUID.randomUUID().toString().replace("-", ""));
            r.setCode("ADMIN");
            r.setName("管理员");
            r.setRemark("平台内置管理员角色，拥有全部权限");
            r.setProtectedRole(true);
            r.setCreateTime(LocalDateTime.now());
            return roleRepository.save(r);
        });
        if (!Boolean.TRUE.equals(adminRole.getProtectedRole())) {
            adminRole.setProtectedRole(true);
            roleRepository.save(adminRole);
        }

        RoleEntity developerRole = roleRepository.findByCode("DEVELOPER").orElseGet(() -> {
            RoleEntity r = new RoleEntity();
            r.setId(UUID.randomUUID().toString().replace("-", ""));
            r.setCode("DEVELOPER");
            r.setName("开发人员");
            r.setRemark("平台内置开发角色，可进行数据研发操作");
            r.setProtectedRole(true);
            r.setCreateTime(LocalDateTime.now());
            return roleRepository.save(r);
        });
        if (!Boolean.TRUE.equals(developerRole.getProtectedRole())) {
            developerRole.setProtectedRole(true);
            roleRepository.save(developerRole);
        }

        if (userRepository.findByUsername("admin").isEmpty()) {
            UserEntity admin = new UserEntity();
            admin.setId(UUID.randomUUID().toString().replace("-", ""));
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setTenantId(tenant.getId());
            admin.setStatus(1);
            admin.setMustChangePassword(true);
            admin.setCreateTime(LocalDateTime.now());
            admin.setRoles(Collections.singleton(adminRole));
            userRepository.save(admin);
            log.info("Seeded default admin account: admin / admin123");
        }
    }
}