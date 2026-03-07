package com.authservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MfaVerifyRequest {

    @NotBlank(message = "MFA code is required")
    @Size(min = 6, max = 6, message = "MFA code must be 6 digits")
    private String code;
}
