package com.onion.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(name = "SignUpUser", description = "DTO for signing up a user")
@Getter
public class SignUpUser {
    @Schema(defaultValue = "onion")
    String username;
    @Schema(defaultValue = "onion1!")
    String password;
    @Schema(defaultValue = "onion@email.com")
    String email;
}
