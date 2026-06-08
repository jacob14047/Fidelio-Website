package it.unisa.fidelio.application;

import it.unisa.fidelio.presentation.TmdbMovieCreditsDTO;
import it.unisa.fidelio.presentation.TmdbMovieDetailsDTO;
import it.unisa.fidelio.presentation.TmdbMovieDto;
import it.unisa.fidelio.presentation.TmdbReviewResponseDTO;
import it.unisa.fidelio.storage.api_data.TmdbGenreListResponse;
import it.unisa.fidelio.storage.api_data.TmdbMovieListResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Collection;
import java.util.List;

@Component
public class TmdbClient {

    private final RestClient restClient;
    private final String apiKey;
    private final String language;
    private final String region;

    public TmdbClient(
            @Value("${tmdb.base-url}") String baseUrl,
            @Value("${tmdb.api-key}") String apiKey,
            @Value("${tmdb.language}") String language,
            @Value("${tmdb.region}") String region
    ) {
        this.apiKey = apiKey;
        this.language = language;
        this.region = region;

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public TmdbMovieListResponse searchMovies(String query, int page) {
        return restClient.get()
                .uri(uri -> uri.path("/search/movie")
                        .queryParam("api_key", apiKey)
                        .queryParam("query", query)
                        .queryParam("language", language)
                        .queryParam("region", region)
                        .queryParam("page", page)
                        .build())
                .retrieve()
                .body(TmdbMovieListResponse.class);
    }

    public TmdbMovieDetailsDTO getMovieDetails(Long tmdbId) {
        return restClient.get()
                .uri(uri -> uri.path("/movie/" + tmdbId)
                        .queryParam("api_key", apiKey)
                        .queryParam("language", language)
                        .build())
                .retrieve()
                .body(TmdbMovieDetailsDTO.class);
    }

    public List<TmdbMovieDto> getMoviesByIds(Collection<Long> tmdbIds) {
        return tmdbIds.stream()
                .map(this::getMovieSummary)
                .toList();
    }


    public TmdbMovieDto getMovieSummary(Long tmdbId) {
        TmdbMovieDetailsDTO details = getMovieDetails(tmdbId);

        List<Integer> genreIds =
                details.genres() == null
                        ? List.of()
                        : details.genres().stream()
                        .map(g -> g.id())
                        .toList();

        return new TmdbMovieDto(
                details.id(),
                details.title(),
                details.posterPath(),
                details.releaseDate(),
                details.voteAverage(),
                genreIds
        );

    }


    public TmdbMovieListResponse getPopular(int page) {
        return restClient.get()
                .uri(uri -> uri.path("/movie/popular")
                        .queryParam("api_key", apiKey)
                        .queryParam("language", language)
                        .queryParam("region", region)
                        .queryParam("page", page)
                        .build())
                .retrieve()
                .body(TmdbMovieListResponse.class);
    }

    public TmdbMovieListResponse getNowPlaying(int page) {
        return restClient.get()
                .uri(uri -> uri.path("/movie/now_playing")
                        .queryParam("api_key", apiKey)
                        .queryParam("language", language)
                        .queryParam("region", region)
                        .queryParam("page", page)
                        .build())
                .retrieve()
                .body(TmdbMovieListResponse.class);
    }

    public TmdbGenreListResponse getMovieGenres() {
        return restClient.get()
                .uri(uri -> uri.path("/genre/movie/list")
                        .queryParam("api_key", apiKey)
                        .queryParam("language", language)
                        .build())
                .retrieve()
                .body(TmdbGenreListResponse.class);
    }

    public TmdbMovieCreditsDTO getMovieCredits(Long tmdbId) {
        return restClient.get()
                .uri(uri -> uri.path("/movie/" + tmdbId + "/credits")
                        .queryParam("api_key", apiKey)
                        .queryParam("language", language)
                        .build())
                .retrieve()
                .body(TmdbMovieCreditsDTO.class);
    }

    public TmdbReviewResponseDTO getMovieReviews(Long tmdbId, int page) {
        return restClient.get()
                .uri(uri -> uri.path("/movie/{id}/reviews")
                        .queryParam("api_key", apiKey)
                        .queryParam("language", language)
                        .queryParam("page", page)
                        .build(tmdbId))
                .retrieve()
                .body(TmdbReviewResponseDTO.class);
    }

    public TmdbMovieListResponse discoverMovies(Integer genreId, String year, int page) {
        return restClient.get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder.path("/discover/movie")
                            .queryParam("api_key", apiKey)
                            .queryParam("language", language)
                            .queryParam("region", region)
                            .queryParam("sort_by", "popularity.desc") // Ordina per popolarità
                            .queryParam("page", page);

                    if (genreId != null) {
                        builder.queryParam("with_genres", genreId);
                    }

                    if (year != null && !year.isBlank()) {
                        builder.queryParam("primary_release_year", year);
                    }

                    return builder.build();
                })
                .retrieve()
                .body(TmdbMovieListResponse.class);
    }
}