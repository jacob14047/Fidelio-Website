package it.unisa.fidelio.application.controller;

import it.unisa.fidelio.application.UtenteService;
import it.unisa.fidelio.presentation.LoginRequestDTO;
import it.unisa.fidelio.presentation.UtenteDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AutenticazioneController {

    private final UtenteService utenteService;
    private final AuthenticationManager authenticationManager;

    public AutenticazioneController(UtenteService utenteService, AuthenticationManager authenticationManager) {
        this.utenteService = utenteService;
        this.authenticationManager = authenticationManager;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDTO request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            var utente = utenteService.findByEmail(request.getEmail());
            if (utente == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Utente non trovato.");
            }

            UtenteDTO utenteLoggato = utenteService.mapToDTO(utente);

            return ResponseEntity.ok(utenteLoggato);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Email o password errati.");
        }
    }
}