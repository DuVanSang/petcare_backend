
package com.petcare.backend.config;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(
    title = "CForum APIs Document",
    version = "6.6.6", 
    description = "API Documentation"), 
    security = {@SecurityRequirement(name = "bearerAuth") },
    servers = {@Server(url = "https://api.petdiarycare.io.vn", description = "Local server") }
    )
@SecurityScheme(name = "bearerAuth", 
    type = SecuritySchemeType.HTTP, 
    scheme = "bearer", 
    bearerFormat = "JWT")
public class SwaggerConfig {
}