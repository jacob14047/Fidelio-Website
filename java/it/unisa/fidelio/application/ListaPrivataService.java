package it.unisa.fidelio.application;

import it.unisa.fidelio.presentation.ListaPrivataFormDTO;
import it.unisa.fidelio.presentation.TmdbMovieDto;
import it.unisa.fidelio.storage.ListaPrivata;
import it.unisa.fidelio.storage.ListaPrivataRepository;
import it.unisa.fidelio.storage.Utente;
import it.unisa.fidelio.storage.UtenteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ListaPrivataService {

    private final ListaPrivataRepository repository;
    private final TmdbClient tmdbService;
    private final UtenteRepository utenteRepository;

    public List<ListaPrivata> getListeUtente(String email) {
        return repository.findByProprietarioEmail(email);
    }

    public List<TmdbMovieDto> getFilmLista(Integer listaId) {
        ListaPrivata lista = repository.findById(listaId)
                .orElseThrow();

        return tmdbService.getMoviesByIds(lista.getFilmTmdb().keySet());
    }

    @Transactional
    public void aggiungiFilm(Integer listaId, Long tmdbId, String email) {

        ListaPrivata lista = repository.findById(listaId)
                .orElseThrow();

        if (lista.getFilmTmdb().containsKey(tmdbId)) {
            throw new IllegalStateException("Film già presente nella lista");
           }

        lista.getFilmTmdb().put(tmdbId, LocalDate.now());
    }

    @Transactional
    public void rimuoviFilm(Integer listaId, Long tmdbId, String email) {
        ListaPrivata lista = repository.findById(listaId).orElseThrow();

        if (!lista.getProprietario().getEmail().equals(email)) {
            throw new AccessDeniedException("Non autorizzato");
        }

        lista.getFilmTmdb().remove(tmdbId);
    }


    public void segnaComeVisto(Integer listaId, Long tmdbId) {
        ListaPrivata lista = repository.findById(listaId).orElseThrow();
        lista.getFilmTmdb().put(tmdbId, LocalDate.now());
        repository.save(lista);
    }

    public List<TmdbMovieDto> getPreviewMovies(ListaPrivata lista) {
        return tmdbService.getMoviesByIds(
                lista.getFilmTmdb().keySet().stream()
                        .limit(4)
                        .toList()
        );
    }

    public void createLista(ListaPrivataFormDTO form, String email) {

        Utente proprietario = utenteRepository
                .findByEmail(email)
                .orElseThrow();

        ListaPrivata lista = new ListaPrivata();
        lista.setNome(form.getNome());
        lista.setDescrizione(form.getDescrizione());
        lista.setProprietario(proprietario);
        lista.setDataCreazione(Instant.now());

        repository.save(lista);
    }

    public boolean filmPresenteInLista(Integer listaId, Long tmdbId) {
        ListaPrivata lista = repository.findById(listaId)
                .orElseThrow();

        return lista.getFilmTmdb().containsKey(tmdbId);
    }

    public Map<Integer, Boolean> getFilmInListeMap(String email, Long tmdbId) {

        List<ListaPrivata> liste = repository.findByProprietarioEmail(email);

        Map<Integer, Boolean> result = new HashMap<>();

        for (ListaPrivata lista : liste) {
            result.put(
                    lista.getId(),
                    lista.getFilmTmdb().containsKey(tmdbId)
            );
        }

        return result;
    }



}
