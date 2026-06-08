package it.unisa.fidelio.application;

import it.unisa.fidelio.presentation.*;
import it.unisa.fidelio.presentation.TmdbMovieCreditsDTO;
import it.unisa.fidelio.presentation.TmdbMovieDetailsDTO;
import it.unisa.fidelio.presentation.TmdbMovieDto;
import it.unisa.fidelio.storage.api_data.TmdbMovieListResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FilmService {

    private final TmdbClient tmdbClient;
    private final GenreService genreService;

    private final String posterBase;

    private static final String BACKDROP_BASE = "https://image.tmdb.org/t/p/w1280";
    private static final String PROFILE_BASE = "https://image.tmdb.org/t/p/w185";

    public FilmService(TmdbClient tmdbClient,
                       GenreService genreService,
                       @Value("${tmdb.poster-base}") String posterBase) {
        this.tmdbClient = tmdbClient;
        this.genreService = genreService;
        this.posterBase = posterBase;
    }

    public List<FilmCardDto> ricercaFilm(String query, int page) {
        TmdbMovieListResponse response = tmdbClient.searchMovies(query, page);
        Map<Integer, String> genreMap = genreService.getGenreMap();

        if (response == null || response.results() == null) return List.of();

        return response.results().stream()
                .map(movieDto -> toFilmCardDto(movieDto, genreMap))
                .toList();
    }


    public List<FilmCardDto> getFilmPopolari(int page) {
        TmdbMovieListResponse response = tmdbClient.getPopular(page);
        Map<Integer, String> genreMap = genreService.getGenreMap();

        if (response == null || response.results() == null) return List.of();

        return response.results().stream()
                .map(movieDto -> toFilmCardDto(movieDto, genreMap))
                .toList();
    }

    public List<FilmCardDto> getNowPlaying(int page) {
        TmdbMovieListResponse response = tmdbClient.getNowPlaying(page);
        Map<Integer, String> genreMap = genreService.getGenreMap();

        if (response == null || response.results() == null) return List.of();

        return response.results().stream()
                .map(movieDto -> toFilmCardDto(movieDto, genreMap))
                .toList();
    }

    public MovieDetailsView getMovieDetailsView(Long tmdbId) {
        TmdbMovieDetailsDTO d = tmdbClient.getMovieDetails(tmdbId);
        TmdbMovieCreditsDTO c = tmdbClient.getMovieCredits(tmdbId);

        if (d == null) return null;

        String year = extractYear(d.releaseDate());
        String runtimeLabel = formatRuntime(d.runtime());
        String posterUrl = d.posterPath() != null ? posterBase + d.posterPath() : null;
        String backdropUrl = d.backdropPath() != null ? BACKDROP_BASE + d.backdropPath() : null;

        List<String> genreNames = (d.genres() == null) ? List.of() :
                d.genres().stream().map(TmdbMovieDetailsDTO.TmdbGenre::name).toList();

        List<MovieDetailsView.PersonView> directors = (c == null || c.crew() == null) ? List.of() :
                c.crew().stream()
                        .filter(p -> "Director".equalsIgnoreCase(p.job()))
                        .limit(3)
                        .map(p -> new MovieDetailsView.PersonView(
                                p.id(), p.name(), "Director",
                                p.profilePath() != null ? PROFILE_BASE + p.profilePath() : null))
                        .toList();

        List<MovieDetailsView.PersonView> castTop = (c == null || c.cast() == null) ? List.of() :
                c.cast().stream()
                        .sorted(Comparator.comparingInt(x -> x.order() == null ? Integer.MAX_VALUE : x.order()))
                        .limit(12)
                        .map(p -> new MovieDetailsView.PersonView(
                                p.id(), p.name(),
                                p.character() != null ? p.character() : "Cast",
                                p.profilePath() != null ? PROFILE_BASE + p.profilePath() : null))
                        .toList();

        return new MovieDetailsView(
                d.id(), d.title(), d.originalTitle(), year,
                d.tagline() != null ? d.tagline() : "",
                d.overview(), d.releaseDate(), runtimeLabel,
                d.voteAverage(), d.voteCount(),
                genreNames, posterUrl, backdropUrl,
                directors, castTop
        );
    }

    public List<FilmCardDto> ricercaFiltrata(String query, String genereNome, String anno) {

        List<TmdbMovieDto> tmdbResults = List.of();

        if (query != null && !query.isEmpty()) {
            TmdbMovieListResponse response = tmdbClient.searchMovies(query, 1);
            if (response != null && response.results() != null) {
                tmdbResults = response.results();
            }
        } else {
            Integer genreId = null;
            if (genereNome != null && !genereNome.isBlank()) {
                genreId = genreService.getGenreIdByName(genereNome);
            }
            TmdbMovieListResponse response = tmdbClient.discoverMovies(genreId, anno, 1);
            if (response != null && response.results() != null) {
                tmdbResults = response.results();
            }


            Map<Integer, String> genreMap = genreService.getGenreMap();
            return tmdbResults.stream()
                    .map(movieDto -> toFilmCardDto(movieDto, genreMap))
                    .toList();
        }


        if (!tmdbResults.isEmpty()) {
            // A. Filtra per Anno
            if (anno != null && !anno.isEmpty()) {
                tmdbResults = tmdbResults.stream()
                        .filter(m -> m.releaseDate() != null && m.releaseDate().startsWith(anno))
                        .collect(Collectors.toList());
            }

            // B. Filtra per Genere
            if (genereNome != null && !genereNome.isEmpty()) {
                Integer genreIdToFilter = genreService.getGenreIdByName(genereNome);
                if (genreIdToFilter != null) {
                    tmdbResults = tmdbResults.stream()
                            .filter(m -> m.genreIds() != null && m.genreIds().contains(genreIdToFilter))
                            .collect(Collectors.toList());
                }
            }
        }

        Map<Integer, String> genreMap = genreService.getGenreMap();
        return tmdbResults.stream()
                .map(movieDto -> toFilmCardDto(movieDto, genreMap))
                .toList();
    }


    public void creaLista(String titolo, int utenteId) {
        // Implementazione placeholder: qui dovresti salvare la lista nel DB.
    }


    private FilmCardDto toFilmCardDto(TmdbMovieDto m, Map<Integer, String> genreMap) {
        Integer year = null;
        if (m.releaseDate() != null && m.releaseDate().length() >= 4) {
            try {
                year = Integer.parseInt(m.releaseDate().substring(0, 4));
            } catch (NumberFormatException e) {
                // Ignora date malformate
            }
        }
        String posterUrl = m.posterPath() == null ? null : posterBase + m.posterPath();

        List<String> genreNames = m.genreIds() == null || m.genreIds().isEmpty() ? List.of() :
                m.genreIds().stream()
                        .map(id -> genreMap.getOrDefault(id, "Sconosciuto"))
                        .toList();

        return new FilmCardDto(
                m.id(), m.title(), year, m.voteAverage(),
                posterUrl,
                m.genreIds() == null ? List.of() : m.genreIds(),
                genreNames
        );
    }

    private String extractYear(String releaseDate) {
        if (releaseDate == null || releaseDate.length() < 4) return "";
        return releaseDate.substring(0, 4);
    }

    private String formatRuntime(Integer runtime) {
        if (runtime == null || runtime <= 0) return "";
        int h = runtime / 60;
        int m = runtime % 60;
        return h > 0 ? (h + "h " + m + "m") : (m + "m");
    }
}