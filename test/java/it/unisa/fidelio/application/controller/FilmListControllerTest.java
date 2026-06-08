package it.unisa.fidelio.application.controller;

import it.unisa.fidelio.application.FilmService;
import it.unisa.fidelio.presentation.FilmCardDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FilmListController.class)
@AutoConfigureMockMvc(addFilters = false)
class FilmListControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FilmService filmService;

    // ==========================================
    // TEST LISTA FILM POPOLARI (/filmlist)
    // ==========================================

    @Test
    void testFilmListPage_Successo() throws Exception {
        FilmCardDto mockFilm = new FilmCardDto(
                1L, "Popolare Test", 2024, 9.0, "/pop.jpg", Collections.emptyList(), Collections.emptyList()
        );
        List<FilmCardDto> mockFilms = Collections.singletonList(mockFilm);

        when(filmService.getFilmPopolari(1)).thenReturn(mockFilms);

        mockMvc.perform(get("/filmlist"))
                .andExpect(status().isOk())
                .andExpect(view().name("film/filmlist"))
                .andExpect(model().attributeExists("filmsList"))
                .andExpect(model().attribute("filmsList", hasSize(1)));
    }

    @Test
    void testFilmListPage_ListaVuota() throws Exception {
        when(filmService.getFilmPopolari(1)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/filmlist"))
                .andExpect(status().isOk())
                .andExpect(view().name("film/filmlist"))
                .andExpect(model().attribute("filmsList", hasSize(0)));
    }
}