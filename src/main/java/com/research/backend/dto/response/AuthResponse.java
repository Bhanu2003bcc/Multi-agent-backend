package com.research.backend.dto.response;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class AuthResponse {
    String accessToken;
    String refreshToken;
    String tokenType;
    long expiresIn;
    String username;
}
