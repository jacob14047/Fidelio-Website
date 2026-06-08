package it.unisa.fidelio.application;

import it.unisa.fidelio.application.TmdbClient;
import it.unisa.fidelio.presentation.TmdbMovieDto;
import it.unisa.fidelio.storage.ListaPrivata;
import it.unisa.fidelio.storage.ListaPrivataRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.*;

@Service
@RequiredArgsConstructor
public class ListaRaccomandatiService {

    private final ListaPrivataRepository listaRepository;
    private final TmdbClient tmdbClient;


    private final String PYTHON_API_URL = "http://127.0.0.1:8000/recommend";

    public List<TmdbMovieDto> getRaccomandazioni(String email) {

        List<ListaPrivata> listeUtente = listaRepository.findByProprietarioEmail(email);

        Set<Long> filmVistiIds = new HashSet<>();
        for (ListaPrivata lista : listeUtente) {
            filmVistiIds.addAll(lista.getFilmTmdb().keySet());
        }

        if (filmVistiIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> recommendedIds = callPythonRecommender(new ArrayList<>(filmVistiIds));

        if (recommendedIds.isEmpty()) {
            return new ArrayList<>();
        }


        return tmdbClient.getMoviesByIds(recommendedIds);
    }

    private List<Long> callPythonRecommender(List<Long> inputIds) {
        RestTemplate restTemplate = new RestTemplate();

        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("movie_ids", inputIds);
            requestBody.put("top_n", 10);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            PythonResponse response = restTemplate.postForObject(PYTHON_API_URL, request, PythonResponse.class);

            if (response != null && response.getRecommended_movie_ids() != null) {
                return response.getRecommended_movie_ids();
            }

        } catch (Exception e) {
            System.err.println("⚠️ ERRORE CONNESSIONE AI (Check se uvicorn è attivo): " + e.getMessage());
        }

        return new ArrayList<>();
    }

    @Data
    public static class PythonResponse {
        private List<Long> recommended_movie_ids;
    }
}