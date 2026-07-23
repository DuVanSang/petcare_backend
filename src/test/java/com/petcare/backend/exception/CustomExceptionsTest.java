package com.petcare.backend.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class CustomExceptionsTest {

    static Stream<Arguments> exceptionMessages() {
        return Stream.of(
                Arguments.of(new BadRequestException("bad"), "bad"),
                Arguments.of(new ConflictException("conflict"), "conflict"),
                Arguments.of(new ForbiddenException("forbidden"), "forbidden"),
                Arguments.of(new ResourceNotFoundException("missing"), "missing")
        );
    }

    @ParameterizedTest
    @MethodSource("exceptionMessages")
    void customExceptions_PreserveMessages(RuntimeException exception, String message) {
        assertThat(exception).hasMessage(message).hasNoCause();
    }

    @Test
    void customExceptions_AcceptNullAndBlankMessages() {
        assertThat(new BadRequestException(null).getMessage()).isNull();
        assertThat(new ConflictException("")).hasMessage("");
        assertThat(new ForbiddenException("   ")).hasMessage("   ");
        assertThat(new ResourceNotFoundException(null).getMessage()).isNull();
    }
}
