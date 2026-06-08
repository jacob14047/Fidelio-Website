package it.unisa.fidelio.application.controller;

import it.unisa.fidelio.application.FilmService;
import it.unisa.fidelio.presentation.FilmCardDto;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class FilmListController {

    private final FilmService filmService;

    public FilmListController(FilmService filmService) {
        this.filmService = filmService;
    }

    @GetMapping("/filmlist")
    public String filmListPage(Model model) {

        List<FilmCardDto> films = filmService.getFilmPopolari(1);

        model.addAttribute("filmsList", films);

        // La vista si trova in templates/film/filmlist.html
        return "film/filmlist";
    }
}