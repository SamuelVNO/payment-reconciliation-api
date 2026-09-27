package io.github.samuelvno.paymentreconciliation.shared.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Swagger UI em /swagger-ui.html com botão "Authorize" para colar o JWT. */
@Configuration
public class OpenApiConfig {

    private static final String BEARER = "bearerAuth";

    @Bean
    OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Payment Reconciliation API")
                        .version("v1")
                        .description("""
                                Concilia pedidos das lojas com os repasses do adquirente e aponta divergências: \
                                repasse faltando, valor errado, taxa acima da contratada, pagamento em dobro \
                                e repasse sem pedido. Faça login em POST /api/auth/login e use o token no Authorize.""")
                        .contact(new Contact().name("Samuel Vitor").url("https://github.com/SamuelVNO")))
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }
}
