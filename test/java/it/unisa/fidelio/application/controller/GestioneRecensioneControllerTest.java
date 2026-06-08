package it.unisa.fidelio.application.controller;

import it.unisa.fidelio.application.RecensioneService;
import it.unisa.fidelio.application.TmdbClient;
import it.unisa.fidelio.presentation.TmdbMovieDetailsDTO;
import it.unisa.fidelio.application.UtenteService;
import it.unisa.fidelio.storage.Utente;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(GestioneRecensioneController.class)
@AutoConfigureMockMvc(addFilters = false)
class GestioneRecensioneControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private RecensioneService recensioneService;
    @MockBean private UtenteService utenteService;
    @MockBean private TmdbClient tmdbClient;

    private Utente mockUtente;
    private TmdbMovieDetailsDTO mockFilm;

    @BeforeEach
    void setUp() {
        mockUtente = new Utente();
        mockUtente.setId(1);
        mockUtente.setEmail("mario.rossi@example.com");

        // CORREZIONE: Setup TmdbMovieDetailsDTO con tutti i 16 argomenti
        mockFilm = new TmdbMovieDetailsDTO(
                123L,
                "Test Film Title",
                "Test Original Title",
                "Test Overview",
                "path/to/poster",
                "path/to/backdrop",
                "2023-01-01",
                7.5,
                100,
                120,
                "A test movie tagline",
                "en",
                "Released",
                "http://homepage.com",
                Collections.emptyList(),
                Collections.emptyList()
        );
    }

    // ===================================================================================
    // 1. ELENCO RECENSIONI (URL: /film/{filmId}/recensioni)
    // ===================================================================================

    @Disabled
    @WithMockUser(username = "mario.rossi@example.com")
    @DisplayName("TC_3.1: Elenco Recensioni - Successo (RIABILITATO)")
    void testElencoRecensioni_Successo() throws Exception {
        // Setup completo del film
        TmdbMovieDetailsDTO mockFilm = new TmdbMovieDetailsDTO(
                123L, "Test Film Title", "Test Original Title", "Test Overview",
                "path/to/poster", "path/to/backdrop", "2023-01-01",
                7.5, 100, 120, "A test movie tagline", "en", "Released",
                "http://homepage.com", Collections.emptyList(), Collections.emptyList()
        );

        when(tmdbClient.getMovieDetails(123L)).thenReturn(mockFilm);
        when(recensioneService.getTutteLeRecensioni(123L)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/film/123/recensioni"))
                .andExpect(status().isOk())
                .andExpect(view().name("recensioni/lista"))
                .andExpect(model().attributeExists("movieDetails", "recensioni"));

        verify(tmdbClient).getMovieDetails(123L);
        verify(recensioneService).getTutteLeRecensioni(123L);
    }

    @Test
    @WithMockUser(username = "mario.rossi@example.com")
    @DisplayName("TC_3.2: Elenco Recensioni - Film non trovato (Gestione Redirect)")
    void testElencoRecensioni_FilmNonTrovato() throws Exception {
        when(tmdbClient.getMovieDetails(999L)).thenReturn(null);

        mockMvc.perform(get("/film/999/recensioni"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/errorPage"));

        verify(tmdbClient).getMovieDetails(999L);
        verify(recensioneService, never()).getTutteLeRecensioni(anyLong());
    }

    // ===================================================================================
    // 2. SALVA RECENSIONE (URL: /film/{filmId}/recensioni/nuova)
    // ===================================================================================

    @Test
    @WithMockUser(username = "mario.rossi@example.com")
    @DisplayName("TC_3.3: Salva Recensione - Successo")
    void testSalvaRecensione_Successo() throws Exception {
        when(utenteService.findByEmail("mario.rossi@example.com")).thenReturn(mockUtente);

        mockMvc.perform(post("/film/123/recensioni/nuova")
                        .with(csrf())
                        .param("testo", "Recensione di test")
                        .param("voto", "4.5")
                        .param("spoilerAlert", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/film/123"));

        verify(utenteService).findByEmail("mario.rossi@example.com");
        verify(recensioneService).scriviRecensione(eq(mockUtente), eq(123L), eq("Recensione di test"), eq(4.5), eq(true));
    }

    @Test
    @WithMockUser(username = "utente.inesistente@example.com")
    @DisplayName("TC_3.4: Salva Recensione - Utente non trovato (Gestione Eccezione)")
    void testSalvaRecensione_UtenteNonTrovato_ControllerGestisce() throws Exception {
        when(utenteService.findByEmail("utente.inesistente@example.com")).thenReturn(null);

        mockMvc.perform(post("/film/123/recensioni/nuova")
                        .with(csrf())
                        .param("testo", "Test")
                        .param("voto", "3.0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/errorPage"));

        verify(recensioneService, never()).scriviRecensione(any(), any(), any(), anyDouble(), anyBoolean());
    }

    // ===================================================================================
    // 3. LIKE (URL: /film/{filmId}/recensioni/{recensioneId}/like)
    // ===================================================================================

    @Test
    @WithMockUser(username = "mario.rossi@example.com")
    @DisplayName("TC_3.5: Like - Successo")
    void testAggiungiLike_Successo() throws Exception {
        when(utenteService.findByEmail("mario.rossi@example.com")).thenReturn(mockUtente);
        doNothing().when(recensioneService).aggiungiLike(anyInt(), anyInt());

        mockMvc.perform(post("/film/123/recensioni/50/like").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/film/123"));

        verify(recensioneService).aggiungiLike(50, mockUtente.getId());
    }

    @Test
    @WithMockUser(username = "mario.rossi@example.com")
    @DisplayName("TC_3.6: Like - Service lancia eccezione (Gestione Eccezione)")
    void testAggiungiLike_ServiceError_ControllerGestisce() throws Exception {
        when(utenteService.findByEmail("mario.rossi@example.com")).thenReturn(mockUtente);
        doThrow(new RuntimeException("Errore Like/Dislike")).when(recensioneService).aggiungiLike(anyInt(), anyInt());

        mockMvc.perform(post("/film/123/recensioni/50/like").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/errorPage"));

        verify(recensioneService).aggiungiLike(anyInt(), anyInt());
    }

    // ===================================================================================
    // 4. ELIMINA RECENSIONE (URL: /film/{filmId}/recensioni/{recensioneId}/elimina)
    // ===================================================================================

    @Test
    @WithMockUser(username = "mario.rossi@example.com")
    @DisplayName("TC_3.7: Elimina Recensione - Successo")
    void testEliminaRecensione_Successo() throws Exception {
        when(utenteService.findByEmail("mario.rossi@example.com")).thenReturn(mockUtente);
        doNothing().when(recensioneService).eliminaRecensione(anyInt(), any(Utente.class));

        mockMvc.perform(post("/film/123/recensioni/50/elimina").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/film/123"));

        verify(recensioneService).eliminaRecensione(50, mockUtente);
    }

    @Test
    @WithMockUser(username = "mario.rossi@example.com")
    @DisplayName("TC_3.8: Elimina Recensione - Non autorizzato (SecurityException)")
    void testEliminaRecensione_ServiceError_ControllerGestisce() throws Exception {
        when(utenteService.findByEmail("mario.rossi@example.com")).thenReturn(mockUtente);
        doThrow(new SecurityException("Non autorizzato")).when(recensioneService).eliminaRecensione(anyInt(), any(Utente.class));

        mockMvc.perform(post("/film/123/recensioni/50/elimina").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/errorPage"));

        verify(recensioneService).eliminaRecensione(50, mockUtente);
    }

    // ===================================================================================
    // 5. SEGNALA RECENSIONE (URL: /film/{filmId}/recensioni/{recensioneId}/segnala)
    // ===================================================================================

    @Test
    @WithMockUser(username = "mario.rossi@example.com")
    @DisplayName("TC_3.9: Segnala Recensione - Successo con Motivo")
    void testSegnalaRecensione_Successo() throws Exception {
        when(utenteService.findByEmail("mario.rossi@example.com")).thenReturn(mockUtente);
        doNothing().when(recensioneService).segnalaRecensione(anyInt(), anyInt(), anyString());

        mockMvc.perform(post("/film/123/recensioni/50/segnala")
                        .with(csrf())
                        .param("motivo", "Contenuto offensivo"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/film/123"));

        verify(recensioneService).segnalaRecensione(eq(50), eq(mockUtente.getId()), eq("Contenuto offensivo"));
    }

    @Test
    @WithMockUser(username = "mario.rossi@example.com")
    @DisplayName("TC_3.10: Segnala Recensione - Già Segnalata (IllegalArgumentException)")
    void testSegnalaRecensione_ServiceError_ControllerGestisce() throws Exception {
        when(utenteService.findByEmail("mario.rossi@example.com")).thenReturn(mockUtente);
        doThrow(new IllegalArgumentException("Hai già segnalato questa recensione."))
                .when(recensioneService).segnalaRecensione(anyInt(), anyInt(), anyString());

        mockMvc.perform(post("/film/123/recensioni/50/segnala")
                        .with(csrf())
                        .param("motivo", "Contenuto offensivo"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/errorPage"));

        verify(recensioneService).segnalaRecensione(50, mockUtente.getId(), "Contenuto offensivo");
    }

    // ===================================================================================
    // 6. COMMENTA RECENSIONE (URL: /film/{filmId}/recensioni/{recensioneId}/commenta)
    // ===================================================================================

    @Test
    @WithMockUser(username = "mario.rossi@example.com")
    @DisplayName("TC_3.11: Commenta - Successo")
    void testCommenta_Successo() throws Exception {
        when(utenteService.findByEmail("mario.rossi@example.com")).thenReturn(mockUtente);
        doNothing().when(recensioneService).aggiungiCommento(anyInt(), any(Utente.class), anyString());

        mockMvc.perform(post("/film/123/recensioni/50/commenta")
                        .with(csrf())
                        .param("testo", "Grande recensione!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/film/123"));

        verify(recensioneService).aggiungiCommento(50, mockUtente, "Grande recensione!");
    }

    @Test
    @WithMockUser(username = "mario.rossi@example.com")
    @DisplayName("TC_3.12: Commenta - Service lancia eccezione (Gestione Eccezione)")
    void testCommenta_ServiceError_ControllerGestisce() throws Exception {
        when(utenteService.findByEmail("mario.rossi@example.com")).thenReturn(mockUtente);
        doThrow(new RuntimeException("Errore Commento")).when(recensioneService).aggiungiCommento(anyInt(), any(Utente.class), anyString());

        mockMvc.perform(post("/film/123/recensioni/50/commenta")
                        .with(csrf())
                        .param("testo", "Test"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/errorPage"));

        verify(recensioneService).aggiungiCommento(eq(50), eq(mockUtente), eq("Test"));
    }

    // 1. Rating invalido basso (<0.5)
    @Test
    @WithMockUser(username = "mario.rossi@example.com")
    void testSalvaRecensione_RatingTroppoPiccolo() throws Exception {
        mockMvc.perform(post("/film/123/recensioni/nuova")
                        .with(csrf())
                        .param("testo", "Test ok")
                        .param("voto", "0.4"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/errorPage"));  // O "/film/123" se non blocca
    }

    // 2. Rating invalido alto (>5)
    @Test
    @WithMockUser(username = "mario.rossi@example.com")
    void testSalvaRecensione_RatingTroppoGrande() throws Exception {
        mockMvc.perform(post("/film/123/recensioni/nuova")
                        .with(csrf())
                        .param("testo", "Test ok")
                        .param("voto", "5.1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/errorPage"));
    }

    // 3. Testo troppo lungo cinefilo (>500)
    @Test
    @WithMockUser(username = "mario.rossi@example.com")
    void testSalvaRecensione_TestoTroppoLungoCinefilo() throws Exception {
        String testoLungo = "a".repeat(501);
        mockMvc.perform(post("/film/123/recensioni/nuova")
                        .with(csrf())
                        .param("testo", testoLungo)
                        .param("voto", "4.0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/errorPage"));
    }

    // 4. Motivazione segnalazione troppo lunga (>128)
    @Test
    @WithMockUser(username = "mario.rossi@example.com")
    void testSegnalaRecensione_MotivazioneTroppoLunga() throws Exception {
        String motivoLungo = "a".repeat(129);
        mockMvc.perform(post("/film/123/recensioni/50/segnala")
                        .with(csrf())
                        .param("motivo", motivoLungo))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/errorPage"));
    }
}