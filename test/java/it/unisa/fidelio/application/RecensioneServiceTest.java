package it.unisa.fidelio.application;

import it.unisa.fidelio.presentation.CommentoDTO;
import it.unisa.fidelio.presentation.PopularReviewViewDTO;
import it.unisa.fidelio.presentation.TmdbReviewResponseDTO;
import it.unisa.fidelio.storage.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecensioneServiceTest {

    @Mock
    private RecensioneRepository recensioneRepo;
    @Mock
    private CommentoRepository commentoRepo;
    @Mock
    private TmdbClient tmdbClient;
    @Mock
    private RecensioneInterazioneRepository recensioneInterazioneRepository;
    @Mock
    private UtenteRepository utenteRepository;
    @Mock
    private SegnalazioneRepository segnalazioneRepository;

    @InjectMocks
    private RecensioneService recensioneService;

    // ===================================================================================
    // 1. TEST DEI METODI "GETTER"
    // ===================================================================================

    @Test
    void testGetLikeGiaFatti() {
        int userId = 1;
        Recensione r1 = new Recensione(); r1.setId(10);
        RecensioneInterazione i1 = new RecensioneInterazione(); i1.setRecensione(r1);

        when(recensioneInterazioneRepository.findByUtenteIdAndTipo(userId, RecensioneInterazione.TipoInterazione.LIKE))
                .thenReturn(Set.of(i1));

        Set<Integer> result = recensioneService.getLikeGiaFatti(userId);

        assertEquals(1, result.size());
        assertTrue(result.contains(10));
    }

    @Test
    void testGetDislikeGiaFatti() {
        int userId = 1;
        Recensione r1 = new Recensione(); r1.setId(5);
        RecensioneInterazione i1 = new RecensioneInterazione(); i1.setRecensione(r1);

        when(recensioneInterazioneRepository.findByUtenteIdAndTipo(userId, RecensioneInterazione.TipoInterazione.DISLIKE))
                .thenReturn(Set.of(i1));

        Set<Integer> result = recensioneService.getDislikeGiaFatti(userId);
        assertTrue(result.contains(5));
    }

    @Test
    void testGetSegnalazioniGiaFatte() {
        int userId = 99;
        Recensione r = new Recensione(); r.setId(100);
        Segnalazione s = new Segnalazione(); s.setRecensione(r);

        when(segnalazioneRepository.findByAutoreId(userId)).thenReturn(List.of(s));

        Set<Integer> result = recensioneService.getSegnalazioniGiaFatte(userId);
        assertTrue(result.contains(100));
    }

    @Test
    void testGetTutteLeRecensioniSegnalate() {
        Long targetFilmId = 555L;
        Long otherFilmId = 999L;

        Recensione r1 = new Recensione(); r1.setId(1); r1.setFilmTmdbId(targetFilmId);
        Recensione r2 = new Recensione(); r2.setId(2); r2.setFilmTmdbId(otherFilmId);

        Segnalazione s1 = new Segnalazione(); s1.setRecensione(r1);
        Segnalazione s2 = new Segnalazione(); s2.setRecensione(r2);

        when(segnalazioneRepository.findAll()).thenReturn(List.of(s1, s2));

        Set<Integer> result = recensioneService.getTutteLeRecensioniSegnalate(targetFilmId);

        assertEquals(1, result.size());
        assertTrue(result.contains(1));
    }

    // ===================================================================================
    // 2. TEST LOGICA COMPLESSA (TMDB + Mapping)
    // ===================================================================================

    @Test
    void testGetTutteLeRecensioni_Completo() {
        Long filmId = 123L;

        // --- Setup Recensione Locale ---
        Utente u = new Utente(); u.setUsername("Mario"); u.setDtype("Standard");
        Recensione rLoc = new Recensione();
        rLoc.setId(1);
        rLoc.setAutore(u);
        rLoc.setVoto(4.0);
        rLoc.setDataCreazione(Instant.now());
        rLoc.setTesto("Locale");
        rLoc.setNumLike(0); rLoc.setNumDislike(0);
        // rLoc ha spoilerAlert nell'entità, ma non nel DTO finale.

        when(recensioneRepo.findByFilmTmdbIdOrderByNumLikeDesc(filmId)).thenReturn(List.of(rLoc));
        when(commentoRepo.findByRecensioneIdOrderByDataCreazioneAsc(1)).thenReturn(Collections.emptyList());

        // --- Setup TMDB Response ---
        TmdbReviewResponseDTO.AuthorDetails detailsWithRating = new TmdbReviewResponseDTO.AuthorDetails(
                "Name1", "User1", null, 8.0);
        TmdbReviewResponseDTO.TmdbReviewDTO reviewWithRating = new TmdbReviewResponseDTO.TmdbReviewDTO(
                "id1", "Author1", detailsWithRating, "Content1", "2023-01-01T10:00:00Z", "url1");

        TmdbReviewResponseDTO.AuthorDetails detailsNullRating = new TmdbReviewResponseDTO.AuthorDetails(
                "Name2", "User2", null, null);
        TmdbReviewResponseDTO.TmdbReviewDTO reviewNullRating = new TmdbReviewResponseDTO.TmdbReviewDTO(
                "id2", "Author2", detailsNullRating, "Content2", "2023-01-01", "url2");

        TmdbReviewResponseDTO tmdbRes = new TmdbReviewResponseDTO(
                123, 1, List.of(reviewWithRating, reviewNullRating), 1, 2
        );

        when(tmdbClient.getMovieReviews(filmId, 1)).thenReturn(tmdbRes);

        List<PopularReviewViewDTO> result = recensioneService.getTutteLeRecensioni(filmId);

        assertEquals(3, result.size());
        assertEquals("★★★★☆", result.get(0).starsText());
        assertEquals("★★★★☆", result.get(1).starsText());
        assertEquals("—", result.get(2).starsText());

        // RIMOSSO il controllo su result.get(1).spoilerAlert() poiché il campo non esiste più nel DTO.
    }

    @Test
    void testGetTutteLeRecensioni_VerificaMappingCommenti() {
        Long filmId = 100L;
        Utente autoreRec = new Utente(); autoreRec.setUsername("Reviewer"); autoreRec.setDtype("S");
        Recensione r = new Recensione();
        r.setId(1); r.setAutore(autoreRec); r.setVoto(3.0);
        r.setDataCreazione(Instant.now()); r.setTesto("T");
        r.setNumLike(0); r.setNumDislike(0);

        // Setup Commento
        Utente autoreComm = new Utente(); autoreComm.setUsername("Commentator"); autoreComm.setDtype("Standard");
        Commento c = new Commento();
        c.setId(50); c.setTesto("Mio Commento"); c.setAutore(autoreComm);
        c.setDataCreazione(Instant.parse("2023-01-01T12:00:00Z"));

        when(recensioneRepo.findByFilmTmdbIdOrderByNumLikeDesc(filmId)).thenReturn(List.of(r));
        when(commentoRepo.findByRecensioneIdOrderByDataCreazioneAsc(r.getId())).thenReturn(List.of(c));
        when(tmdbClient.getMovieReviews(anyLong(), anyInt())).thenReturn(null);

        List<PopularReviewViewDTO> result = recensioneService.getTutteLeRecensioni(filmId);

        assertFalse(result.isEmpty());
        List<CommentoDTO> commentiDto = result.get(0).comments();
        assertEquals(1, commentiDto.size());
        assertEquals("Commentator", commentiDto.get(0).username());
    }

    // ===================================================================================
    // 3. TEST SCRITTURA E CANCELLAZIONE
    // ===================================================================================

    @Test
    void testScriviRecensione() {
        Utente autore = new Utente();
        Long filmId = 555L;
        String testo = "Recensione test";
        double voto = 4.5;
        boolean spoiler = true;

        when(recensioneRepo.save(any(Recensione.class))).thenAnswer(i -> i.getArgument(0));

        Recensione result = recensioneService.scriviRecensione(autore, filmId, testo, voto, spoiler);

        assertNotNull(result);
        assertEquals(filmId, result.getFilmTmdbId());

        // Questo controllo rimane valido perché stiamo controllando l'Entità Recensione (che ha ancora il campo nel DB),
        // non il DTO PopularReviewViewDTO.
        assertTrue(result.getSpoilerAlert());
    }

    @Test
    void testEliminaRecensione_AdminPuoCancellareAltrui() {
        Integer recId = 10;
        Utente admin = new Utente(); admin.setId(1); admin.setAmministratore(true);
        Utente altroUser = new Utente(); altroUser.setId(2);

        Recensione r = new Recensione(); r.setId(recId); r.setAutore(altroUser);

        when(recensioneRepo.findById(recId)).thenReturn(Optional.of(r));

        recensioneService.eliminaRecensione(recId, admin);

        verify(recensioneRepo).delete(r);
    }

    @Test
    void testEliminaRecensione_UtenteNonAutorizzato() {
        Integer recId = 10;
        Utente user = new Utente(); user.setId(1); user.setAmministratore(false);
        Utente altro = new Utente(); altro.setId(2);
        Recensione r = new Recensione(); r.setId(recId); r.setAutore(altro);

        when(recensioneRepo.findById(recId)).thenReturn(Optional.of(r));

        assertThrows(SecurityException.class, () -> recensioneService.eliminaRecensione(recId, user));
        verify(recensioneRepo, never()).delete(any());
    }

    // ===================================================================================
    // 4. TEST INTERAZIONI (LIKE / DISLIKE)
    // ===================================================================================

    @Test
    void testAggiungiLike_Successo() {
        int userId = 1;
        int recId = 10;

        Utente autore = new Utente(); autore.setId(2);
        Recensione r = new Recensione();
        r.setId(recId);
        r.setAutore(autore);
        r.setNumLike(0);

        when(recensioneRepo.findById(recId)).thenReturn(Optional.of(r));
        when(recensioneInterazioneRepository.findByUtenteIdAndRecensioneIdAndTipo(userId, recId, RecensioneInterazione.TipoInterazione.LIKE))
                .thenReturn(Optional.empty());
        when(recensioneInterazioneRepository.findByUtenteIdAndRecensioneIdAndTipo(userId, recId, RecensioneInterazione.TipoInterazione.DISLIKE))
                .thenReturn(Optional.empty());

        when(utenteRepository.getReferenceById(userId)).thenReturn(new Utente());

        recensioneService.aggiungiLike(recId, userId);

        assertEquals(1, r.getNumLike());
        verify(recensioneRepo).save(r);
        verify(recensioneInterazioneRepository).save(any(RecensioneInterazione.class));
    }

    @Test
    void testAggiungiDislike_Successo() {
        int recId = 20;
        int userId = 2;
        Utente autore = new Utente(); autore.setId(99);
        Recensione r = new Recensione();
        r.setId(recId);
        r.setAutore(autore);
        r.setNumDislike(5);

        when(recensioneRepo.findById(recId)).thenReturn(Optional.of(r));
        when(recensioneInterazioneRepository.findByUtenteIdAndRecensioneIdAndTipo(userId, recId, RecensioneInterazione.TipoInterazione.LIKE))
                .thenReturn(Optional.empty());
        when(recensioneInterazioneRepository.findByUtenteIdAndRecensioneIdAndTipo(userId, recId, RecensioneInterazione.TipoInterazione.DISLIKE))
                .thenReturn(Optional.empty());

        when(utenteRepository.getReferenceById(userId)).thenReturn(new Utente());

        recensioneService.aggiungiDislike(recId, userId);

        assertEquals(6, r.getNumDislike());
        verify(recensioneRepo).save(r);
        verify(recensioneInterazioneRepository).save(argThat(i -> i.getTipo() == RecensioneInterazione.TipoInterazione.DISLIKE));
    }

    @Test
    void testAggiungiLike_GiaPresente_RimuoveLike() {
        int userId = 1;
        int recId = 10;
        Utente autore = new Utente(); autore.setId(99);
        Recensione r = new Recensione();
        r.setId(recId);
        r.setAutore(autore);
        r.setNumLike(1);

        RecensioneInterazione existingLike = new RecensioneInterazione();
        existingLike.setTipo(RecensioneInterazione.TipoInterazione.LIKE);

        when(recensioneRepo.findById(recId)).thenReturn(Optional.of(r));
        when(recensioneInterazioneRepository.findByUtenteIdAndRecensioneIdAndTipo(userId, recId, RecensioneInterazione.TipoInterazione.LIKE))
                .thenReturn(Optional.of(existingLike));

        when(recensioneInterazioneRepository.findByUtenteIdAndRecensioneIdAndTipo(userId, recId, RecensioneInterazione.TipoInterazione.DISLIKE))
                .thenReturn(Optional.empty());

        recensioneService.aggiungiLike(recId, userId);

        assertEquals(0, r.getNumLike());
        verify(recensioneInterazioneRepository).delete(existingLike);
        verify(recensioneRepo).save(r);
    }

    @Test
    void testAggiungiLike_AutoreVotaSeStesso_NonFaNulla() {
        int userId = 1;
        int recId = 10;
        Utente autore = new Utente(); autore.setId(userId);
        Recensione r = new Recensione(); r.setId(recId); r.setAutore(autore);

        when(recensioneRepo.findById(recId)).thenReturn(Optional.of(r));

        recensioneService.aggiungiLike(recId, userId);

        verify(recensioneRepo, never()).save(any());
        verify(recensioneInterazioneRepository, never()).save(any());
    }

    @Test
    void testAggiungiLike_RecensioneNonTrovata() {
        when(recensioneRepo.findById(999)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                recensioneService.aggiungiLike(999, 1)
        );
    }

    // ===================================================================================
    // 5. TEST COMMENTI E SEGNALAZIONI
    // ===================================================================================

    @Test
    void testAggiungiCommento() {
        int recId = 30;
        Recensione r = new Recensione(); r.setId(recId);
        when(recensioneRepo.findById(recId)).thenReturn(Optional.of(r));

        recensioneService.aggiungiCommento(recId, new Utente(), "Commento");

        verify(commentoRepo).save(any(Commento.class));
    }

    @Test
    void testSegnalaRecensione_MotivoNullo() {
        int recId = 10;
        int autoreSegId = 5;
        Recensione r = new Recensione();
        Utente autoreRec = new Utente(); autoreRec.setId(99);
        r.setId(recId); r.setAutore(autoreRec);

        when(recensioneRepo.findById(recId)).thenReturn(Optional.of(r));
        when(segnalazioneRepository.existsByRecensioneIdAndAutoreId(recId, autoreSegId)).thenReturn(false);
        when(utenteRepository.getReferenceById(autoreSegId)).thenReturn(new Utente());

        recensioneService.segnalaRecensione(recId, autoreSegId, null);

        verify(segnalazioneRepository).save(argThat(s -> s.getMotivo().equals("Contenuto inappropriato")));
    }

    @Test
    void testSegnalaRecensione_GiaSegnalata() {
        int recId = 10;
        int autoreId = 5;
        Recensione r = new Recensione();
        Utente autoreRec = new Utente(); autoreRec.setId(99);
        r.setAutore(autoreRec);

        when(recensioneRepo.findById(recId)).thenReturn(Optional.of(r));
        when(segnalazioneRepository.existsByRecensioneIdAndAutoreId(recId, autoreId)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () ->
                recensioneService.segnalaRecensione(recId, autoreId, "Spam")
        );
    }

    @Test
    void testEliminaRecensione_NonTrovata() {
        when(recensioneRepo.findById(999)).thenReturn(Optional.empty());
        Utente admin = new Utente(); admin.setAmministratore(true);
        assertThrows(IllegalArgumentException.class, () -> recensioneService.eliminaRecensione(999, admin));
    }
}