package it.unisa.fidelio.presentation;

import lombok.Data;

@Data
public class LoginRequestDTO {
    private String email;
    private String password;
}
