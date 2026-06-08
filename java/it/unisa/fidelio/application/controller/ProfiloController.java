package it.unisa.fidelio.application.controller;

import it.unisa.fidelio.application.AdminService;
import it.unisa.fidelio.application.ListaRaccomandatiService; // <--- IMPORT NUOVO
import it.unisa.fidelio.application.UtenteService;
import it.unisa.fidelio.presentation.TmdbMovieDto; // <--- IMPORT NUOVO
import it.unisa.fidelio.storage.Utente;
import it.unisa.fidelio.storage.UtenteRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Controller
public class ProfiloController {

    private final UtenteService utenteService;
    private final UtenteRepository utenteRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminService adminService;
    private final ListaRaccomandatiService raccomandatiService;

    public ProfiloController(UtenteService utenteService,
                             UtenteRepository utenteRepository,
                             PasswordEncoder passwordEncoder,
                             AdminService adminService,
                             ListaRaccomandatiService raccomandatiService) {
        this.utenteService = utenteService;
        this.utenteRepository = utenteRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminService = adminService;
        this.raccomandatiService = raccomandatiService;
    }

    @GetMapping("/profilo")
    public String visualizzaProfilo(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        Utente utente = utenteService.findByEmail(userDetails.getUsername());

        model.addAttribute("utenteCorrente", utente);
        model.addAttribute("recensioni", utente.getRecensioni());


        try {
            List<TmdbMovieDto> raccomandati = raccomandatiService.getRaccomandazioni(utente.getEmail());
            model.addAttribute("filmRaccomandati", raccomandati);
        } catch (Exception e) {
            System.err.println("Errore nel recupero raccomandazioni: " + e.getMessage());
            model.addAttribute("filmRaccomandati", new ArrayList<>());
        }

        if (Boolean.TRUE.equals(utente.getAmministratore())) {
            model.addAttribute("listaUtenti", utenteRepository.findAll());
            model.addAttribute("listaSegnalazioni", adminService.getSegnalazioniAperte());
        }

        return "profiloUtente";
    }

    @PostMapping("/profilo/aggiorna")
    public String aggiornaProfilo(@AuthenticationPrincipal UserDetails userDetails,
                                  @RequestParam("nome") String nome,
                                  @RequestParam("cognome") String cognome,
                                  @RequestParam("username") String username,
                                  @RequestParam("email") String email,
                                  @RequestParam("viaENumCivico") String viaENumCivico,
                                  @RequestParam(value = "nuovaPassword", required = false) String nuovaPassword,
                                  @RequestParam("bio") String bio,
                                  @RequestParam(value = "testata", required = false) String testata,
                                  @RequestParam(value = "casa", required = false) String casa,
                                  @RequestParam(value = "credit", required = false) String credit,
                                  @RequestParam(value = "immagine", required = false) MultipartFile immagine) {

        Utente utente = utenteService.findByEmail(userDetails.getUsername());

        utente.setNome(nome);
        utente.setCognome(cognome);
        utente.setBio(bio);
        utente.setViaENumCivico(viaENumCivico);
        utente.setUsername(username);
        utente.setEmail(email);

        if (nuovaPassword != null && !nuovaPassword.isBlank()) {
            utente.setPassword(passwordEncoder.encode(nuovaPassword));
        }

        if ("Critico".equals(utente.getDtype())) {
            utente.setTestataGiornalistica(testata);
        } else if ("Fedele".equals(utente.getDtype())) {
            utente.setCasaProduzione(casa);
            utente.setCreditReference(credit);
        }

        if (immagine != null && !immagine.isEmpty()) {
            try {
                utente.setImmagineProfilo(immagine.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        utenteRepository.save(utente);
        return "redirect:/profilo";
    }
}