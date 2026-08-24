package com.petcare.backend.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.TimeZone;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
public class TimeZoneConfig {
    private static final ZoneOffset API_TIME_OFFSET = ZoneOffset.UTC;
    private static final DateTimeFormatter OFFSET_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    @Bean
    Jackson2ObjectMapperBuilderCustomizer utcLocalDateTimeCustomizer() {
        return builder -> builder
                .timeZone(TimeZone.getTimeZone("UTC"))
                .serializerByType(LocalDateTime.class, new UtcLocalDateTimeSerializer())
                .deserializerByType(LocalDateTime.class, new UtcLocalDateTimeDeserializer());
    }

    private static final class UtcLocalDateTimeSerializer extends JsonSerializer<LocalDateTime> {
        @Override
        public void serialize(
                LocalDateTime value,
                JsonGenerator gen,
                SerializerProvider serializers
        ) throws IOException {
            gen.writeString(value.atOffset(API_TIME_OFFSET).format(OFFSET_FORMATTER));
        }
    }

    private static final class UtcLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {
        @Override
        public LocalDateTime deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            String value = parser.getValueAsString();
            if (!StringUtils.hasText(value)) {
                return null;
            }

            String normalized = value.trim();
            try {
                return OffsetDateTime.parse(normalized, OFFSET_FORMATTER)
                        .withOffsetSameInstant(API_TIME_OFFSET)
                        .toLocalDateTime();
            } catch (DateTimeParseException ignored) {
                // Fall through to legacy formats used by older clients.
            }

            try {
                return Instant.parse(normalized)
                        .atOffset(API_TIME_OFFSET)
                        .toLocalDateTime();
            } catch (DateTimeParseException ignored) {
                // Fall through to LocalDateTime for backward-compatible requests.
            }

            return LocalDateTime.parse(normalized, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
    }
}
