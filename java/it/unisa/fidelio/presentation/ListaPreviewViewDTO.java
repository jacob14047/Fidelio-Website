package it.unisa.fidelio.presentation;

import java.util.List;

public record ListaPreviewViewDTO(
        Integer id,
        String nome,
        String descrizione,
        String username,
        int filmCount,
        List<TmdbMovieDto> previewMovies
) {}
