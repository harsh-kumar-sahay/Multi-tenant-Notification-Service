package com.notifsvc.config;

import com.notifsvc.user.AppUser;
import com.notifsvc.user.AppUserRepository;
import com.notifsvc.user.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * A brand new database has no users at all, so nobody could ever log in to create the
 * first tenant admin. Seed exactly one platform admin on startup if none exists yet.
 */
@Component
public class BootstrapDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapDataInitializer.class);

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final String bootstrapUsername;
    private final String bootstrapPassword;

    public BootstrapDataInitializer(
            AppUserRepository appUserRepository,
            PasswordEncoder passwordEncoder,
            @Value("${notifsvc.bootstrap.platform-admin-username:admin}") String bootstrapUsername,
            @Value("${notifsvc.bootstrap.platform-admin-password:ChangeMe123!}") String bootstrapPassword) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.bootstrapUsername = bootstrapUsername;
        this.bootstrapPassword = bootstrapPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        boolean anyPlatformAdmin = appUserRepository.findByUsername(bootstrapUsername).isPresent();
        if (anyPlatformAdmin) {
            return;
        }
        AppUser admin = new AppUser();
        admin.setUsername(bootstrapUsername);
        admin.setPasswordHash(passwordEncoder.encode(bootstrapPassword));
        admin.setRole(Role.PLATFORM_ADMIN);
        admin.setTenant(null);
        appUserRepository.save(admin);
        log.info("Seeded default platform admin user '{}'. Change the password after first login.", bootstrapUsername);
    }
}
