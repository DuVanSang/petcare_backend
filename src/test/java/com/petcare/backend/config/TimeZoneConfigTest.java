package com.petcare.backend.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

class TimeZoneConfigTest {

    private final ObjectMapper objectMapper = objectMapper();

    @Test
    void localDateTimeSerializesWithUtcOffset() throws Exception {
        LocalDateTime value = LocalDateTime.of(2026, 8, 24, 8, 39, 11);

        String json = objectMapper.writeValueAsString(value);

        assertThat(json).isEqualTo("\"2026-08-24T08:39:11Z\"");
    }

    @Test
    void localDateTimeDeserializesOffsetInputToUtcLocalDateTime() throws Exception {
        LocalDateTime value = objectMapper.readValue("\"2026-08-24T15:39:11+07:00\"", LocalDateTime.class);

        assertThat(value).isEqualTo(LocalDateTime.of(2026, 8, 24, 8, 39, 11));
    }

    private static ObjectMapper objectMapper() {
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        new TimeZoneConfig().utcLocalDateTimeCustomizer().customize(builder);
        return builder.build();
    }
}
