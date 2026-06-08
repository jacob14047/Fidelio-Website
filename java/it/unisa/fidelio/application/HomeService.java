package it.unisa.fidelio.application;

import it.unisa.fidelio.presentation.FilmCardDto;
import it.unisa.fidelio.presentation.TmdbMovieDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class HomeService {

    private final TmdbClient tmdbClient;
    private final GenreService genreService;
    private final String posterBase;

    public HomeService(
            TmdbClient tmdbClient,
            GenreService genreService,
            @Value("${tmdb.poster-base}") String posterBase
    ) {
        this.tmdbClient = tmdbClient;
        this.genreService = genreService;
        this.posterBase = posterBase;
    }

    public List<FilmCardDto> getMlPreview(int limit) {
        Map<Integer, String> genreMap = genreService.getGenreMap();

        var resp = tmdbClient.discoverMovies(
                null,   // nessun genere
                null,   // nessun anno
                3       // pagina diversa
        );

        return resp.results().stream()
                .limit(limit)
                .map(m -> toCard(m, genreMap))
                .toList();
    }


    public List<FilmCardDto> getPopularCards(int limit) {
        Map<Integer, String> genreMap = genreService.getGenreMap();
        var resp = tmdbClient.getPopular(1);

        return resp.results().stream()
                .limit(limit)
                .map(m -> toCard(m, genreMap))
                .toList();
    }

    public List<FilmCardDto> getNewReleaseCards(int limit) {
        Map<Integer, String> genreMap = genreService.getGenreMap();
        var resp = tmdbClient.getNowPlaying(1);

        return resp.results().stream()
                .limit(limit)
                .map(m -> toCard(m, genreMap))
                .toList();
    }

    private FilmCardDto toCard(TmdbMovieDto m, Map<Integer, String> genreMap) {
        Integer year = null;
        if (m.releaseDate() != null && m.releaseDate().length() >= 4) {
            year = Integer.parseInt(m.releaseDate().substring(0, 4));
        }

        String posterUrl = (m.posterPath() == null) ? null : (posterBase + m.posterPath());

        List<Integer> ids = (m.genreIds() == null) ? List.of() : m.genreIds();
        List<String> names = ids.stream()
                .map(id -> genreMap.getOrDefault(id, "Unknown"))
                .toList();

        return new FilmCardDto(
                m.id(),
                m.title(),
                year,
                m.voteAverage(),
                posterUrl,
                ids,
                names
        );
    }
}

