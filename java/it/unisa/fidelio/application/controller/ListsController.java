package it.unisa.fidelio.application.controller;

import it.unisa.fidelio.application.ListaPrivataService;
import it.unisa.fidelio.presentation.ListaPreviewViewDTO;
import it.unisa.fidelio.presentation.ListaPrivataFormDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/lists")
@RequiredArgsConstructor
public class ListsController {

    private final ListaPrivataService listaService;

    @GetMapping
    public String lists(Model model, Principal principal) {

        model.addAttribute("listaForm", new ListaPrivataFormDTO());

        List<ListaPreviewViewDTO> views =
                listaService.getListeUtente(principal.getName())
                        .stream()
                        .map(lista -> new ListaPreviewViewDTO(
                                lista.getId(),
                                lista.getNome(),
                                lista.getDescrizione(),
                                lista.getProprietario().getUsername(),
                                lista.getFilmTmdb().size(),
                                listaService.getPreviewMovies(lista)
                        ))
                        .toList();

        model.addAttribute("lists", views);
        return "lists/index";
    }



    @GetMapping("/{id}")
    public String listDetail(@PathVariable Integer id, Model model) {
        model.addAttribute("movies", listaService.getFilmLista(id));
        model.addAttribute("listId", id);
        return "lists/detail";
    }

    @PostMapping
    public String createList(
            @ModelAttribute("listaForm") ListaPrivataFormDTO form,
            Principal principal
    ) {
        listaService.createLista(form, principal.getName());
        return "redirect:/lists";
    }

    @PostMapping("/{listId}/add")
    public String addMovieToList(
            @PathVariable Integer listId,
            @RequestParam Long tmdbId,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        listaService.aggiungiFilm(listId, tmdbId, principal.getName());

        redirectAttributes.addFlashAttribute(
                "listSuccessMessage",
                "Film aggiunto correttamente alla lista!"
        );

        return "redirect:/movies/" + tmdbId;
    }

    @PostMapping("/{listId}/remove")
    public String removeMovie(
            @PathVariable Integer listId,
            @RequestParam Long tmdbId,
            Principal principal
    ) {
        listaService.rimuoviFilm(listId, tmdbId, principal.getName());
        return "redirect:/lists/" + listId;
    }


}

