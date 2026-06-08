package it.unisa.fidelio.presentation;

import java.util.List;

public record FilmCardDto(
        long tmdbId,
        String title,
        Integer year,
        double rating,
        String posterUrl,
        List<Integer> genreIds,
        List<String> genres
) {}

