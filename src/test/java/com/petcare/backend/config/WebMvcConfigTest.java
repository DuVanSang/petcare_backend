package com.petcare.backend.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;

class WebMvcConfigTest {
    @Test
    void addResourceHandlers_RegistersNormalizedAbsoluteUploadLocation() {
        WebMvcConfig config = new WebMvcConfig();
        ReflectionTestUtils.setField(config, "uploadRootDir", "build/../uploads-test");
        ResourceHandlerRegistry registry = new ResourceHandlerRegistry(new StaticApplicationContext(), new MockServletContext());

        config.addResourceHandlers(registry);

        assertThat(registry.hasMappingForPattern("/uploads/**")).isTrue();
        assertThat(Path.of("uploads-test").toAbsolutePath().normalize().toUri().toString()).startsWith("file:");
    }
}
