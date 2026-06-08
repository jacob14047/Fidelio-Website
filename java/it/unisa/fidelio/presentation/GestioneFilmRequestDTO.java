package it.unisa.fidelio.presentation;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
@Setter
@Getter
public class GestioneFilmRequestDTO {

    // Getter e Setter
    private Long tmdbId;

    private String stato;  // "VISTO", "DA_VEDERE", "PREFERITO"

    private LocalDate dataVisione;  // obbligatoria solo se stato = "VISTO", altrimenti può essere null

    // Costruttore vuoto (necessario per Jackson/Spring)
    public GestioneFilmRequestDTO() {}

    // Costruttore completo (comodo per test)
    public GestioneFilmRequestDTO(Long tmdbId, String stato, LocalDate dataVisione) {
        this.tmdbId = tmdbId;
        this.stato = stato;
        this.dataVisione = dataVisione;
    }

}