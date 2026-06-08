package it.unisa.fidelio.application;

import it.unisa.fidelio.presentation.TmdbGenre;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
public class GenreService {

    private final TmdbClient tmdbClient;

    private static final long TTL_SECONDS = 60 * 60 * 12; // 12 ore
    private final AtomicReference<Cache> cache = new AtomicReference<>(new Cache(Map.of(), Instant.EPOCH));

    public GenreService(TmdbClient tmdbClient) {
        this.tmdbClient = tmdbClient;
    }

    public Map<Integer, String> getGenreMap() {
        Cache current = cache.get();
        if (Instant.now().isBefore(current.expiresAt())) {
            return current.map();
        }

        var resp = tmdbClient.getMovieGenres();
        Map<Integer, String> map = resp.genres().stream()
                .collect(Collectors.toMap(TmdbGenre::id, TmdbGenre::name));

        cache.set(new Cache(map, Instant.now().plusSeconds(TTL_SECONDS)));
        return map;
    }

    // --- METODO AGGIUNTO PER RECUPERARE ID DA NOME ---
    public Integer getGenreIdByName(String genreName) {
        if (genreName == null || genreName.isBlank()) {
            return null;
        }

        // Recuperiamo la mappa (usando la cache se valida)
        Map<Integer, String> genres = getGenreMap();

        // Cerchiamo l'ID corrispondente al nome (case-insensitive)
        for (Map.Entry<Integer, String> entry : genres.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(genreName)) {
                return entry.getKey();
            }
        }

        return null; // Nessun match trovato
    }

    private record Cache(Map<Integer, String> map, Instant expiresAt) {}
}