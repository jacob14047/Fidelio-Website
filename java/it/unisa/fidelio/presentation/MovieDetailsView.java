package it.unisa.fidelio.presentation;

import java.util.List;

public record MovieDetailsView(
        long tmdbId,
        String title,
        String originalTitle,
        String year,
        String tagline,
        String overview,
        String releaseDate,
        String runtimeLabel,
        double rating,
        int votes,
        List<String> genres,
        String posterUrl,
        String backdropUrl,
        List<PersonView> directors,
        List<PersonView> castTop
) {
    public record PersonView(
            int id,
            String name,
            String roleLabel,
            String profileUrl
    ) {}

    // Aggiungi getter per 'id' come alias di tmdbId
    public long getId() {
        return tmdbId;
    }
}