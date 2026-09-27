package io.github.samuelvno.paymentreconciliation.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import io.github.samuelvno.paymentreconciliation.shared.security.SecurityProperties;

/** Cria o primeiro ADMIN no boot (senão ninguém conseguiria logar para cadastrar lojas). */
@Component
class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final AuthService authService;
    private final SecurityProperties properties;

    AdminBootstrap(AuthService authService, SecurityProperties properties) {
        this.authService = authService;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        SecurityProperties.Admin admin = properties.admin();
        if (!admin.isConfigured()) {
            log.info("ADMIN_EMAIL/ADMIN_PASSWORD not set, skipping admin bootstrap");
            return;
        }
        if (authService.createAdminIfMissing(admin.email(), admin.password())) {
            log.info("Bootstrap admin {} created", admin.email());
        }
    }
}
