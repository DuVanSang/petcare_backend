package com.petcare.backend.service;

import lombok.Getter;

@Getter
public class GoogleUserPayload {
    private final String subject;
    private final String email;
    private final String name;
    private final String picture;

    public GoogleUserPayload(String subject, String email, String name, String picture) {
        this.subject = subject;
        this.email = email;
        this.name = name;
        this.picture = picture;
    }
}
