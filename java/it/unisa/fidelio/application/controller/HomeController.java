package it.unisa.fidelio.application.controller;

import it.unisa.fidelio.application.HomeService;
import it.unisa.fidelio.application.UtenteService; // Servirà questo
import it.unisa.fidelio.presentation.UtenteDTO;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final HomeService homeService;
    private final UtenteService utenteService;

    public HomeController(HomeService homeService, UtenteService utenteService) {
        this.homeService = homeService;
        this.utenteService = utenteService;
    }

    @GetMapping({"/", "/home"})
    public String home(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        model.addAttribute("popularFilms", homeService.getPopularCards(4));
        model.addAttribute("newReleases", homeService.getNewReleaseCards(4));
        model.addAttribute("mlPreview", homeService.getMlPreview(6));

        if (userDetails != null) {
            UtenteDTO userDTO = utenteService.mapToDTO(utenteService.findByEmail(userDetails.getUsername()));
            model.addAttribute("userDTO", userDTO);
        } else {
            model.addAttribute("userDTO", null);
        }

        return "home";
    }
}