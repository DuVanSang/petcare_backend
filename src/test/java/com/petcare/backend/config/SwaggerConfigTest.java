package com.petcare.backend.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.junit.jupiter.api.Test;

class SwaggerConfigTest {
    @Test
    void swaggerConfig_DeclaresExpectedOpenApiMetadataAndBearerScheme() {
        OpenAPIDefinition definition = SwaggerConfig.class.getAnnotation(OpenAPIDefinition.class);
        SecurityScheme scheme = SwaggerConfig.class.getAnnotation(SecurityScheme.class);

        assertThat(new SwaggerConfig()).isNotNull();
        assertThat(definition.info().title()).isEqualTo("CForum APIs Document");
        assertThat(definition.info().version()).isEqualTo("6.6.6");
        assertThat(scheme.name()).isEqualTo("bearerAuth");
        assertThat(scheme.scheme()).isEqualTo("bearer");
        assertThat(scheme.bearerFormat()).isEqualTo("JWT");
    }
}
