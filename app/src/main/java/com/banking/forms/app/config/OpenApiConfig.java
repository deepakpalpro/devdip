package com.banking.forms.app.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI documentation for the two BFFs. Each BFF is published as a separate grouped spec so the
 * consumer and admin surfaces are documented (and can be contract-tested) independently:
 * {@code /v3/api-docs/consumer} and {@code /v3/api-docs/admin}, both browsable via Swagger UI.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI bankingFormsOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Banking Forms Platform API")
                        .version("v1")
                        .description(
                                "Multi-tenant banking support platform. Requests require an X-Tenant-Id header; "
                                        + "actor identity is currently supplied via X-Dev-User-Id pending OIDC."));
    }

    @Bean
    GroupedOpenApi consumerApi() {
        return GroupedOpenApi.builder()
                .group("consumer")
                .displayName("Consumer BFF")
                .pathsToMatch("/api/consumer/v1/**")
                .build();
    }

    @Bean
    GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("admin")
                .displayName("Admin BFF")
                .pathsToMatch("/api/admin/v1/**")
                .build();
    }
}
