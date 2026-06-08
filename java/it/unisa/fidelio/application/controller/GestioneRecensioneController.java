package it.unisa.fidelio.application.controller;

import it.unisa.fidelio.application.RecensioneService;
import it.unisa.fidelio.application.TmdbClient;
import it.unisa.fidelio.application.UtenteService;
import it.unisa.fidelio.presentation.TmdbMovieDetailsDTO;
import it.unisa.fidelio.storage.Utente;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/film/{filmId}")
public class GestioneRecensioneController {

    private final RecensioneService recensioneService;
    private final UtenteService utenteService;
    private final TmdbClient tmdbClient;

    public GestioneRecensioneController(RecensioneService recensioneService,
                                        UtenteService utenteService,
                                        TmdbClient tmdbClient) {
        this.recensioneService = recensioneService;
        this.utenteService = utenteService;
        this.tmdbClient = tmdbClient;
    }


    @GetMapping("/recensioni")
    public String elencoRecensioni(@PathVariable Long filmId, Model model) {
        TmdbMovieDetailsDTO movieDetails = tmdbClient.getMovieDetails(filmId);
        if (movieDetails == null) {
            return "redirect:/errorPage";
        }

        model.addAttribute("recensioni", recensioneService.getTutteLeRecensioni(filmId));
        model.addAttribute("movieDetails", movieDetails);
        return "recensioni/lista";
    }

    @PostMapping("/recensioni/nuova")
    public String salvaRecensione(@PathVariable Long filmId,
                                  @RequestParam String testo,
                                  @RequestParam Double voto,
                                  @RequestParam(required = false, defaultValue = "false") boolean spoilerAlert,
                                  @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {

        try {
            Utente utenteLoggato = utenteService.findByEmail(userDetails.getUsername());
            if (utenteLoggato == null) {
                throw new IllegalStateException("Utente non trovato");
            }
            recensioneService.scriviRecensione(utenteLoggato, filmId, testo, voto, spoilerAlert);
        } catch (Exception e) {
            return "redirect:/errorPage";
        }

        return "redirect:/film/" + filmId;
    }

    @PostMapping("/recensioni/{recensioneId}/like")
    public String like(@PathVariable Long filmId,
                       @PathVariable Integer recensioneId,
                       @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {

        try {
            Utente utenteLoggato = utenteService.findByEmail(userDetails.getUsername());
            if (utenteLoggato == null) {
                throw new IllegalStateException("Utente non trovato");
            }
            recensioneService.aggiungiLike(recensioneId, utenteLoggato.getId());
        } catch (Exception e) {
            return "redirect:/errorPage";
        }

        return "redirect:/film/" + filmId;
    }

    @PostMapping("/recensioni/{recensioneId}/dislike")
    public String dislike(@PathVariable Long filmId,
                          @PathVariable Integer recensioneId,
                          @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {

        try {
            Utente utenteLoggato = utenteService.findByEmail(userDetails.getUsername());
            if (utenteLoggato == null) {
                throw new IllegalStateException("Utente non trovato");
            }
            recensioneService.aggiungiDislike(recensioneId, utenteLoggato.getId());
        } catch (Exception e) {
            return "redirect:/errorPage";
        }

        return "redirect:/film/" + filmId;
    }

    @PostMapping("/recensioni/{recensioneId}/commenta")
    public String commenta(@PathVariable Long filmId,
                           @PathVariable Integer recensioneId,
                           @RequestParam String testo,
                           @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {

        try {
            Utente utenteLoggato = utenteService.findByEmail(userDetails.getUsername());
            if (utenteLoggato == null) {
                throw new IllegalStateException("Utente non trovato");
            }
            recensioneService.aggiungiCommento(recensioneId, utenteLoggato, testo);
        } catch (Exception e) {
            return "redirect:/errorPage";
        }

        return "redirect:/film/" + filmId;
    }

    @PostMapping("/recensioni/{recensioneId}/elimina")
    public String eliminaRecensione(@PathVariable Long filmId,
                                    @PathVariable Integer recensioneId,
                                    @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {

        try {
            Utente utenteLoggato = utenteService.findByEmail(userDetails.getUsername());
            if (utenteLoggato == null) {
                throw new IllegalStateException("Utente non trovato");
            }
            recensioneService.eliminaRecensione(recensioneId, utenteLoggato);
        } catch (Exception e) {
            // SecurityException, IllegalArgumentException ecc.
            return "redirect:/errorPage";
        }

        return "redirect:/film/" + filmId;
    }

    @PostMapping("/recensioni/{recensioneId}/segnala")
    public String segnalaRecensione(@PathVariable Long filmId,
                                    @PathVariable Integer recensioneId,
                                    @RequestParam(required = false) String motivo,
                                    @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {

        try {
            Utente utenteLoggato = utenteService.findByEmail(userDetails.getUsername());
            if (utenteLoggato == null) {
                throw new IllegalStateException("Utente non trovato");
            }
            recensioneService.segnalaRecensione(recensioneId, utenteLoggato.getId(), motivo);
        } catch (IllegalArgumentException e) {
            return "redirect:/errorPage";
        } catch (Exception e) {
            return "redirect:/errorPage";
        }

        return "redirect:/film/" + filmId;
    }
}