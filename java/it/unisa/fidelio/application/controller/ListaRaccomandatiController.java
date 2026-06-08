package it.unisa.fidelio.application.controller;

import it.unisa.fidelio.application.ListaRaccomandatiService;
import it.unisa.fidelio.presentation.TmdbMovieDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/raccomandazioni") // L'endpoint sarà localhost:8080/api/raccomandazioni
@RequiredArgsConstructor
public class ListaRaccomandatiController {

    private final ListaRaccomandatiService raccomandatiService;

    @GetMapping
    public ResponseEntity<List<TmdbMovieDto>> ottieniRaccomandazioni(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }
        String email = userDetails.getUsername(); // Assumo che l'email sia l'username
        List<TmdbMovieDto> filmSuggeriti = raccomandatiService.getRaccomandazioni(email);
        if (filmSuggeriti.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(filmSuggeriti);
    }
}