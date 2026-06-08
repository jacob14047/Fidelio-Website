package it.unisa.fidelio.application;

import it.unisa.fidelio.presentation.CommentoDTO;
import it.unisa.fidelio.presentation.PopularReviewViewDTO;
import it.unisa.fidelio.presentation.TmdbReviewResponseDTO;
import it.unisa.fidelio.storage.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RecensioneService {

    private final RecensioneRepository recensioneRepo;
    private final CommentoRepository commentoRepo;
    private final TmdbClient tmdbClient;
    private final RecensioneInterazioneRepository recensioneInterazioneRepository;
    private final UtenteRepository utenteRepository;
    private final SegnalazioneRepository segnalazioneRepository;

    @Autowired
    public RecensioneService(RecensioneRepository recensioneRepo,
                             CommentoRepository commentoRepo,
                             TmdbClient tmdbClient,
                             RecensioneInterazioneRepository recensioneInterazioneRepository,
                             UtenteRepository utenteRepository,
                             SegnalazioneRepository segnalazioneRepository) {
        this.recensioneRepo = recensioneRepo;
        this.commentoRepo = commentoRepo;
        this.tmdbClient = tmdbClient;
        this.recensioneInterazioneRepository = recensioneInterazioneRepository;
        this.utenteRepository = utenteRepository;
        this.segnalazioneRepository = segnalazioneRepository;
    }

    public Set<Integer> getLikeGiaFatti(int utenteId) {
        return recensioneInterazioneRepository
                .findByUtenteIdAndTipo(utenteId, RecensioneInterazione.TipoInterazione.LIKE)
                .stream()
                .map(inter -> inter.getRecensione().getId())
                .collect(Collectors.toSet());
    }

    public Set<Integer> getDislikeGiaFatti(int utenteId) {
        return recensioneInterazioneRepository
                .findByUtenteIdAndTipo(utenteId, RecensioneInterazione.TipoInterazione.DISLIKE)
                .stream()
                .map(inter -> inter.getRecensione().getId())
                .collect(Collectors.toSet());
    }

    public Set<Integer> getSegnalazioniGiaFatte(int utenteId) {
        return segnalazioneRepository
                .findByAutoreId(utenteId)
                .stream()
                .map(seg -> seg.getRecensione().getId())
                .collect(Collectors.toSet());
    }

    public Set<Integer> getTutteLeRecensioniSegnalate(Long filmId) {
        return segnalazioneRepository.findAll().stream()
                .filter(s -> s.getRecensione().getFilmTmdbId().equals(filmId))
                .map(s -> s.getRecensione().getId())
                .collect(Collectors.toSet());
    }

    public List<PopularReviewViewDTO> getTutteLeRecensioni(Long filmId) {
        List<PopularReviewViewDTO> listaFinale = new ArrayList<>();
        List<Recensione> locali = recensioneRepo.findByFilmTmdbIdOrderByNumLikeDesc(filmId);
        for (Recensione r : locali) {
            List<CommentoDTO> commentiLocali = commentoRepo
                    .findByRecensioneIdOrderByDataCreazioneAsc(r.getId())
                    .stream()
                    .map(this::mapCommentoToDto)
                    .collect(Collectors.toList());

            listaFinale.add(new PopularReviewViewDTO(
                    r.getAutore().getUsername(),
                    r.getAutore().getUsername(),
                    r.getAutore().getUsername().substring(0, 1).toUpperCase(),
                    generateStarsText(r.getVoto()),
                    r.getDataCreazione().toString().substring(0, 10),
                    r.getTesto(),
                    null,
                    true,
                    r.getId(),
                    null,
                    r.getNumLike(),
                    r.getNumDislike(),
                    commentiLocali,
                    r.getAutore().getDtype()
            ));
        }

        TmdbReviewResponseDTO tmdbRes = tmdbClient.getMovieReviews(filmId, 1);
        if (tmdbRes != null && tmdbRes.results() != null) {
            tmdbRes.results().stream().limit(10).forEach(tr -> {
                listaFinale.add(new PopularReviewViewDTO(
                        tr.author(),
                        tr.author(),
                        "T",
                        ratingToStars(tr.authorDetails() != null ? tr.authorDetails().rating() : null),
                        tr.createdAt().length() >= 10 ? tr.createdAt().substring(0, 10) : tr.createdAt(),
                        tr.content(),
                        tr.url(),
                        false,
                        null,
                        tr.id(),
                        0,
                        0,
                        List.of(),
                        null
                ));
            });
        }
        return listaFinale;
    }

    @Transactional
    public void eliminaRecensione(Integer recensioneId, Utente utenteCorrente) {
        Recensione recensione = recensioneRepo.findById(recensioneId)
                .orElseThrow(() -> new IllegalArgumentException("Recensione non trovata"));

        // L'admin può eliminare qualsiasi recensione locale
        if (!recensione.getAutore().getId().equals(utenteCorrente.getId()) && !utenteCorrente.getAmministratore()) {
            throw new SecurityException("Non autorizzato");
        }

        recensioneRepo.delete(recensione);
    }

    @Transactional
    public Recensione scriviRecensione(Utente autore, Long filmTmdbId, String testo, double voto, boolean spoiler) {
        Recensione r = new Recensione();
        r.setAutore(autore);
        r.setFilmTmdbId(filmTmdbId);
        r.setTesto(testo);
        r.setVoto(voto);
        r.setSpoilerAlert(spoiler);
        r.setDataCreazione(Instant.now());
        r.setNumLike(0);
        r.setNumDislike(0);
        return recensioneRepo.save(r);
    }

    @Transactional
    public void aggiungiLike(Integer recensioneId, int utenteId) {
        gestisciInterazione(recensioneId, utenteId, RecensioneInterazione.TipoInterazione.LIKE);
    }

    @Transactional
    public void aggiungiDislike(Integer recensioneId, int utenteId) {
        gestisciInterazione(recensioneId, utenteId, RecensioneInterazione.TipoInterazione.DISLIKE);
    }

    @Transactional
    public void gestisciInterazione(Integer recensioneId, int utenteId, RecensioneInterazione.TipoInterazione nuovoTipo) {
        Recensione r = recensioneRepo.findById(recensioneId)
                .orElseThrow(() -> new IllegalArgumentException("Recensione non trovata"));

        // Impedisci l'autovoto
        if (r.getAutore().getId().equals(utenteId)) return;

        // Cerchiamo se esistono già interazioni di questo utente per questa recensione
        var existingLike = recensioneInterazioneRepository
                .findByUtenteIdAndRecensioneIdAndTipo(utenteId, recensioneId, RecensioneInterazione.TipoInterazione.LIKE);

        var existingDislike = recensioneInterazioneRepository
                .findByUtenteIdAndRecensioneIdAndTipo(utenteId, recensioneId, RecensioneInterazione.TipoInterazione.DISLIKE);

        // LOGICA PER IL LIKE
        if (nuovoTipo == RecensioneInterazione.TipoInterazione.LIKE) {
            if (existingLike.isPresent()) {
                // TOGGLE OFF: L'utente ha già messo like e clicca di nuovo -> Rimuovi Like
                recensioneInterazioneRepository.delete(existingLike.get());
                r.setNumLike(Math.max(0, r.getNumLike() - 1));
            } else {
                // Se c'era un DISLIKE, lo rimuoviamo (SWITCH)
                if (existingDislike.isPresent()) {
                    recensioneInterazioneRepository.delete(existingDislike.get());
                    r.setNumDislike(Math.max(0, r.getNumDislike() - 1));
                }
                // Aggiungi il NUOVO LIKE
                salvaNuovaInterazione(r, utenteId, RecensioneInterazione.TipoInterazione.LIKE);
                r.setNumLike(r.getNumLike() + 1);
            }
        }
        // LOGICA PER IL DISLIKE
        else {
            if (existingDislike.isPresent()) {
                // TOGGLE OFF: L'utente ha già messo dislike e clicca di nuovo -> Rimuovi Dislike
                recensioneInterazioneRepository.delete(existingDislike.get());
                r.setNumDislike(Math.max(0, r.getNumDislike() - 1));
            } else {
                // Se c'era un LIKE, lo rimuoviamo (SWITCH)
                if (existingLike.isPresent()) {
                    recensioneInterazioneRepository.delete(existingLike.get());
                    r.setNumLike(Math.max(0, r.getNumLike() - 1));
                }
                // Aggiungi il NUOVO DISLIKE
                salvaNuovaInterazione(r, utenteId, RecensioneInterazione.TipoInterazione.DISLIKE);
                r.setNumDislike(r.getNumDislike() + 1);
            }
        }

        recensioneRepo.save(r);
    }

    // Metodo helper per evitare duplicazione di codice nel salvataggio
    private void salvaNuovaInterazione(Recensione r, int utenteId, RecensioneInterazione.TipoInterazione tipo) {
        RecensioneInterazione inter = new RecensioneInterazione();
        inter.setUtente(utenteRepository.getReferenceById(utenteId));
        inter.setRecensione(r);
        inter.setTipo(tipo);
        recensioneInterazioneRepository.save(inter);
    }

    @Transactional
    public void aggiungiCommento(Integer recensioneId, Utente autore, String testo) {
        Recensione r = recensioneRepo.findById(recensioneId)
                .orElseThrow(() -> new IllegalArgumentException("Recensione non trovata"));

        Commento c = new Commento();
        c.setRecensione(r);
        c.setAutore(autore);
        c.setTesto(testo);
        c.setDataCreazione(Instant.now());
        commentoRepo.save(c);
    }

    @Transactional
    public void segnalaRecensione(Integer recensioneId, int autoreId, String motivo) {
        Recensione recensione = recensioneRepo.findById(recensioneId)
                .orElseThrow(() -> new IllegalArgumentException("Recensione non trovata"));

        if (recensione.getAutore().getId().equals(autoreId)) {
            throw new IllegalArgumentException("Non puoi segnalare la tua recensione.");
        }
        if (segnalazioneRepository.existsByRecensioneIdAndAutoreId(recensioneId, autoreId)) {
            throw new IllegalArgumentException("Hai già segnalato questa recensione.");
        }

        Segnalazione segnalazione = new Segnalazione();
        segnalazione.setRecensione(recensione);
        segnalazione.setAutore(utenteRepository.getReferenceById(autoreId));
        segnalazione.setMotivo(motivo != null && !motivo.trim().isEmpty() ? motivo.trim() : "Contenuto inappropriato");
        segnalazione.setDataSegnalazione(Instant.now());
        segnalazione.setStato("APERTA");
        segnalazioneRepository.save(segnalazione);
    }

    private CommentoDTO mapCommentoToDto(Commento c) {
        String username = c.getAutore().getUsername();
        return new CommentoDTO(
                c.getId(),
                c.getTesto(),
                username,
                c.getDataCreazione().toString().substring(0, 10),
                username.substring(0, 1).toUpperCase(),
                c.getAutore().getDtype()
        );
    }

    private String generateStarsText(Double voto) {
        if (voto == null) return "☆☆☆☆☆";
        int v = voto.intValue();
        return "★".repeat(Math.min(5, v)) + "☆".repeat(Math.max(0, 5 - v));
    }

    private String ratingToStars(Double rating10) {
        if (rating10 == null) return "—";
        int stars = (int) Math.round(rating10 / 2.0);
        stars = Math.max(0, Math.min(5, stars));
        return "★".repeat(stars) + "☆".repeat(5 - stars);
    }
}