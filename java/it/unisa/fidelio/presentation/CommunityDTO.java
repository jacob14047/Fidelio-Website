package it.unisa.fidelio.presentation;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommunityDTO {
    private Integer id;
    private String nome;
    private String descrizione;
    private Instant dataCreazione;
    private Integer numMembri;
    private String creatoreUsername;
}
