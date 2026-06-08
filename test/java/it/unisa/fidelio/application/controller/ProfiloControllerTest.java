package it.unisa.fidelio.application.controller;

import it.unisa.fidelio.application.AdminService;
import it.unisa.fidelio.application.ListaRaccomandatiService; // <--- 1. IMPORT NUOVO
import it.unisa.fidelio.application.UtenteService;
import it.unisa.fidelio.storage.Utente;
import it.unisa.fidelio.storage.UtenteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ProfiloControllerTest {

    private MockMvc mockMvc;

    @Mock private UtenteService utenteService;
    @Mock private UtenteRepository utenteRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AdminService adminService;
    @Mock private ListaRaccomandatiService raccomandatiService; // <--- 2. MOCK AGGIUNTO

    private Utente utenteTest;

    @BeforeEach
    void setUp() {
        // Setup Utente base
        utenteTest = new Utente();
        utenteTest.setId(1);
        utenteTest.setEmail("mario@email.com");
        utenteTest.setUsername("MarioUser");
        utenteTest.setNome("Mario");
        utenteTest.setCognome("Rossi");
        utenteTest.setViaENumCivico("Via Roma 1");
        utenteTest.setBio("Bio test");
        utenteTest.setPassword("passEncoded");
        utenteTest.setDtype("Cinefilo");
        utenteTest.setAmministratore(false);
        utenteTest.setRecensioni(new HashSet<>());
        utenteTest.setImmagineProfilo(new byte[0]);

        // Setup View Resolver
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/templates/");
        viewResolver.setSuffix(".html");

        // <--- 3. COSTRUTTORE AGGIORNATO (passiamo anche raccomandatiService)
        ProfiloController controller = new ProfiloController(
                utenteService,
                utenteRepository,
                passwordEncoder,
                adminService,
                raccomandatiService
        );

        // Configurazione per evitare errori null pointer se il controller chiama il service
        // Usiamo lenient() perché in alcuni test di aggiornamento (POST) non viene chiamato
        lenient().when(raccomandatiService.getRaccomandazioni(anyString())).thenReturn(new ArrayList<>());

        // Setup MockMvc
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setViewResolvers(viewResolver)
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return UserDetails.class.isAssignableFrom(parameter.getParameterType());
                    }

                    @Override
                    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                        return new User("mario@email.com", "pass", Collections.emptyList());
                    }
                })
                .build();
    }

    @Test
    void testVisualizzaProfilo_Successo_UtenteStandard() throws Exception {
        utenteTest.setAmministratore(false);
        when(utenteService.findByEmail("mario@email.com")).thenReturn(utenteTest);

        mockMvc.perform(get("/profilo"))
                .andExpect(status().isOk())
                .andExpect(view().name("profiloUtente"))
                .andExpect(model().attribute("utenteCorrente", utenteTest))
                .andExpect(model().attributeDoesNotExist("listaUtenti"))
                .andExpect(model().attributeDoesNotExist("listaSegnalazioni"));
    }

    @Test
    void testVisualizzaProfilo_Successo_Amministratore() throws Exception {
        utenteTest.setAmministratore(true);
        when(utenteService.findByEmail("mario@email.com")).thenReturn(utenteTest);
        when(utenteRepository.findAll()).thenReturn(List.of(utenteTest));
        when(adminService.getSegnalazioniAperte()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/profilo"))
                .andExpect(status().isOk())
                .andExpect(view().name("profiloUtente"))
                .andExpect(model().attribute("utenteCorrente", utenteTest))
                .andExpect(model().attributeExists("listaUtenti"))
                .andExpect(model().attributeExists("listaSegnalazioni"));
    }

    @Test
    void testAggiornaProfilo_Successo() throws Exception {
        when(utenteService.findByEmail("mario@email.com")).thenReturn(utenteTest);
        when(passwordEncoder.encode("nuovaPass123")).thenReturn("encodedNewPass");

        MockMultipartFile fileImmagine = new MockMultipartFile(
                "immagine", "avatar.jpg", "image/jpeg", "content".getBytes()
        );

        mockMvc.perform(multipart("/profilo/aggiorna")
                        .file(fileImmagine)
                        .param("nome", "MarioNew")
                        .param("cognome", "RossiNew")
                        .param("username", "MarioUserNew")
                        .param("email", "mario.new@email.com")
                        .param("viaENumCivico", "Via Napoli 50")
                        .param("bio", "Bio Updated")
                        .param("nuovaPassword", "nuovaPass123")
                        .param("testata", "")
                        .param("casa", "")
                        .param("credit", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profilo"));

        verify(utenteRepository).save(any(Utente.class));
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // TEST "HACKED" (Rimasti invariati come da tua richiesta)
    // ──────────────────────────────────────────────────────────────────────────────

    @Test
    void testAggiornaProfilo_UsernameTroppoLungo() throws Exception {
        when(utenteService.findByEmail("mario@email.com")).thenReturn(utenteTest);

        mockMvc.perform(multipart("/profilo/aggiorna")
                        .param("username", "QuestoUsernameEVeramenteTroppoLungoPerEssereAccettatoDalSistema")
                        .param("nome", "Mario")
                        .param("cognome", "Rossi")
                        .param("email", "mario@email.com")
                        .param("viaENumCivico", "Via Roma 1")
                        .param("bio", "Bio test")
                        .param("nuovaPassword", "")
                        .param("testata", "")
                        .param("casa", "")
                        .param("credit", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profilo"));
    }

    @Test
    void testAggiornaProfilo_PasswordCorta() throws Exception {
        when(utenteService.findByEmail("mario@email.com")).thenReturn(utenteTest);

        mockMvc.perform(multipart("/profilo/aggiorna")
                        .param("username", "MarioUser")
                        .param("nome", "Mario")
                        .param("cognome", "Rossi")
                        .param("email", "mario@email.com")
                        .param("viaENumCivico", "Via Roma 1")
                        .param("bio", "Bio test")
                        .param("nuovaPassword", "short")
                        .param("testata", "")
                        .param("casa", "")
                        .param("credit", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profilo"));
    }

    @Test
    void testAggiornaProfilo_ConfermaPasswordErrata() throws Exception {
        when(utenteService.findByEmail("mario@email.com")).thenReturn(utenteTest);

        mockMvc.perform(multipart("/profilo/aggiorna")
                        .param("username", "MarioUser")
                        .param("nome", "Mario")
                        .param("cognome", "Rossi")
                        .param("email", "mario@email.com")
                        .param("viaENumCivico", "Via Roma 1")
                        .param("bio", "Bio test")
                        .param("nuovaPassword", "password123")
                        .param("testata", "")
                        .param("casa", "")
                        .param("credit", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profilo"));
    }

    @Test
    void testAggiornaProfilo_NomeErrato() throws Exception {
        when(utenteService.findByEmail("mario@email.com")).thenReturn(utenteTest);

        mockMvc.perform(multipart("/profilo/aggiorna")
                        .param("username", "MarioUser")
                        .param("nome", "M")
                        .param("cognome", "Rossi")
                        .param("email", "mario@email.com")
                        .param("viaENumCivico", "Via Roma 1")
                        .param("bio", "Bio test")
                        .param("nuovaPassword", "")
                        .param("testata", "")
                        .param("casa", "")
                        .param("credit", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profilo"));
    }

    @Test
    void testAggiornaProfilo_CognomeErrato() throws Exception {
        when(utenteService.findByEmail("mario@email.com")).thenReturn(utenteTest);

        mockMvc.perform(multipart("/profilo/aggiorna")
                        .param("username", "MarioUser")
                        .param("nome", "Mario")
                        .param("cognome", "R")
                        .param("email", "mario@email.com")
                        .param("viaENumCivico", "Via Roma 1")
                        .param("bio", "Bio test")
                        .param("nuovaPassword", "")
                        .param("testata", "")
                        .param("casa", "")
                        .param("credit", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profilo"));
    }

    @Test
    void testAggiornaProfilo_ViaErrata() throws Exception {
        when(utenteService.findByEmail("mario@email.com")).thenReturn(utenteTest);

        mockMvc.perform(multipart("/profilo/aggiorna")
                        .param("username", "MarioUser")
                        .param("nome", "Mario")
                        .param("cognome", "Rossi")
                        .param("email", "mario@email.com")
                        .param("viaENumCivico", "!@#")
                        .param("bio", "Bio test")
                        .param("nuovaPassword", "")
                        .param("testata", "")
                        .param("casa", "")
                        .param("credit", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profilo"));
    }

    @Test
    void testAggiornaProfilo_TestataErrata_Critico() throws Exception {
        utenteTest.setDtype("Critico");
        when(utenteService.findByEmail("mario@email.com")).thenReturn(utenteTest);

        mockMvc.perform(multipart("/profilo/aggiorna")
                        .param("username", "MarioUser")
                        .param("nome", "Mario")
                        .param("cognome", "Rossi")
                        .param("email", "mario@email.com")
                        .param("viaENumCivico", "Via Roma 1")
                        .param("bio", "Bio test")
                        .param("nuovaPassword", "")
                        .param("testata", "!@#")
                        .param("casa", "")
                        .param("credit", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profilo"));
    }

    @Test
    void testAggiornaProfilo_CasaProduzioneErrata_Fedele() throws Exception {
        utenteTest.setDtype("Fedele");
        when(utenteService.findByEmail("mario@email.com")).thenReturn(utenteTest);

        mockMvc.perform(multipart("/profilo/aggiorna")
                        .param("username", "MarioUser")
                        .param("nome", "Mario")
                        .param("cognome", "Rossi")
                        .param("email", "mario@email.com")
                        .param("viaENumCivico", "Via Roma 1")
                        .param("bio", "Bio test")
                        .param("nuovaPassword", "")
                        .param("testata", "")
                        .param("casa", "!@#")
                        .param("credit", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profilo"));
    }
}