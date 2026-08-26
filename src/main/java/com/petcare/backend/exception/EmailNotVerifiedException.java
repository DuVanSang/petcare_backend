package com.petcare.backend.exception;

public class EmailNotVerifiedException extends RuntimeException {
    private final String email;

    public EmailNotVerifiedException(String email) {
        super("Tài khoản chưa xác thực email. Vui lòng nhập mã OTP để tiếp tục.");
        this.email = email;
    }

    public String getEmail() {
        return email;
    }
}
