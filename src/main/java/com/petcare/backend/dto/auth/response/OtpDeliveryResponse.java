package com.petcare.backend.dto.auth.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OtpDeliveryResponse {
    private boolean emailSent;
    private String devOtp;
}
