package it.unisa.fidelio.application.controller;

import it.unisa.fidelio.application.*;
import it.unisa.fidelio.presentation.FilmCardDto;
import it.unisa.fidelio.presentation.MovieDetailsView;
import it.unisa.fidelio.storage.Utente;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GestioneFilmController.class)
@AutoConfigureMockMvc(addFilters = false)
class GestioneFilmControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FilmService filmService;

    @MockBean
    private RecensioneService recensioneService;

    @MockBean
    private UtenteService utenteService;

    @MockBean
    private TmdbClient tmdbClient;

    // --- AGGIUNTO QUESTO MOCK ---
    @MockBean
    private ListaPrivataService listaService;

    private Utente mockUtente;

    private final MovieDetailsView.PersonView mockPerson = new MovieDetailsView.PersonView(1, "Mock Director", "Director", "/profile.jpg");
    private final List<MovieDetailsView.PersonView> filledPersonList = List.of(mockPerson);
    private final List<String> filledGenreList = List.of("Azione", "Drammatico");

    @BeforeEach
    void setUp() {
        mockUtente = new Utente();
        mockUtente.setId(1);
        mockUtente.setEmail("test@user.it");
        mockUtente.setAmministratore(false);

        when(utenteService.findByEmail(any())).thenReturn(mockUtente);

        // Mock base per le recensioni
        when(recensioneService.getLikeGiaFatti(anyInt())).thenReturn(new HashSet<>());
        when(recensioneService.getDislikeGiaFatti(anyInt())).thenReturn(new HashSet<>());
        when(recensioneService.getSegnalazioniGiaFatte(anyInt())).thenReturn(new HashSet<>());
        when(recensioneService.getTutteLeRecensioni(anyLong())).thenReturn(Collections.emptyList());

        // --- AGGIUNTO MOCK DELLE LISTE ---
        // Altrimenti il controller lancia NullPointerException quando prova a chiamare questi metodi
        when(listaService.getListeUtente(anyString())).thenReturn(Collections.emptyList());
        when(listaService.getFilmInListeMap(anyString(), anyLong())).thenReturn(Collections.emptyMap());
    }

    private MovieDetailsView createMockDetailsView(Long filmId) {
        return new MovieDetailsView(
                filmId, "Titolo di Prova", "Titolo Org", "2023", "Tagline di Prova",
                "Overview minima", "01-01-2023", "2h 0m", 8.0, 120,
                filledGenreList, "/poster.jpg", "/backdrop.jpg",
                filledPersonList, filledPersonList
        );
    }

    @Test
    void testRicercaFilm_Successo() throws Exception {
        FilmCardDto mockFilm = new FilmCardDto(
                1L, "Test Film", 2020, 7.5, "/poster.jpg", Collections.emptyList(), List.of("Azione")
        );
        List<FilmCardDto> mockRisultati = Arrays.asList(mockFilm);

        when(filmService.ricercaFilm(eq("Matrix"), eq(1))).thenReturn(mockRisultati);

        mockMvc.perform(get("/film/api/search")
                        .param("query", "Matrix")
                        .param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is("Test Film")));
    }

    @Test
    void testRicercaFilm_QueryNull() throws Exception {
        mockMvc.perform(get("/film/api/search"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testRicercaFilm_FormatoNonValido() throws Exception {
        mockMvc.perform(get("/film/api/search")
                        .param("query", "Matrix$"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Errato: Nome film errato"));
    }

    @Test
    void testRicercaFilm_PaginaNegativa() throws Exception {
        mockMvc.perform(get("/film/api/search")
                        .param("query", "Test")
                        .param("page", "-1"))
                .andExpect(status().isOk());
    }

    @Test
    void testRicercaFiltrata_Successo() throws Exception {
        List<FilmCardDto> mockRisultati = Collections.emptyList();
        when(filmService.ricercaFiltrata(eq(""), eq("Azione"), eq("2020"))).thenReturn(mockRisultati);

        mockMvc.perform(get("/film/api/search/filter")
                        .param("genere", "Azione")
                        .param("anno", "2020"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", is(Collections.emptyList())));

        verify(filmService, times(1)).ricercaFiltrata(eq(""), eq("Azione"), eq("2020"));
    }

    @Test
    void testRicercaFiltrata_NessunParametro() throws Exception {
        mockMvc.perform(get("/film/api/search/filter"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testRicercaFiltrata_AnnoErrato() throws Exception {
        mockMvc.perform(get("/film/api/search/filter")
                        .param("genere", "Azione")
                        .param("anno", "202x"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Errato: Anno errato"));
    }

    @Test
    @WithMockUser(username = "test@user.it")
    void testCreaLista_TitoloValido_Successo() throws Exception {
        String titolo = "La mia lista";
        mockMvc.perform(post("/film/api/lista")
                        .with(csrf())
                        .param("titolo", titolo)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string("Lista creata con successo"));

        verify(filmService, times(1)).creaLista(eq(titolo), eq(1));
    }

    @Test
    void testCreaLista_NonAutenticato_Bloccato() throws Exception {
        mockMvc.perform(post("/film/api/lista")
                        .with(csrf())
                        .param("titolo", "Lista test"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "test@user.it")
    void testCreaLista_TroppoCorto() throws Exception {
        mockMvc.perform(post("/film/api/lista")
                        .with(csrf())
                        .param("titolo", "ab"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Errato: Titolo troppo corto o troppo lungo"));
    }

    @Test
    @WithMockUser(username = "test@user.it")
    void testCreaLista_TroppoLungo() throws Exception {
        mockMvc.perform(post("/film/api/lista")
                        .with(csrf())
                        .param("titolo", "Questo Titolo è Veramente Troppo Lungo Per Essere Accettato"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Errato: Titolo troppo corto o troppo lungo"));
    }

    @Test
    @WithMockUser(username = "test@user.it")
    void testCreaLista_TitoloVuotoStringa() throws Exception {
        mockMvc.perform(post("/film/api/lista")
                        .with(csrf())
                        .param("titolo", " "))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testVisualizzaDettagli_Successo_UtenteAnonimo() throws Exception {
        Long filmId = 123L;
        MovieDetailsView realMovieDetailsView = createMockDetailsView(filmId);
        when(filmService.getMovieDetailsView(eq(filmId))).thenReturn(realMovieDetailsView);

        mockMvc.perform(get("/film/" + filmId))
                .andExpect(status().isOk())
                .andExpect(view().name("film/details"))
                .andExpect(model().attribute("utenteLoggato", is(nullValue())));
    }

    @Test
    void testVisualizzaDettagli_FilmNonTrovato() throws Exception {
        when(filmService.getMovieDetailsView(eq(999L))).thenReturn(null);

        mockMvc.perform(get("/film/999"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void testRicercaFilm_NonTrovato() throws Exception {
        when(filmService.ricercaFilm(eq("FilmCheNonEsiste"), eq(1)))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/film/api/search")
                        .param("query", "FilmCheNonEsiste")
                        .param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)))
                .andExpect(content().string("[]"));
    }
}