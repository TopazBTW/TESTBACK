package com.topaz.back.dtos;

import lombok.Data;

@Data
public class RegisterRequest {
    private String username;
    private String password;
    private String role; // exemple : ROLE_USER ou ROLE_ADMIN
}
