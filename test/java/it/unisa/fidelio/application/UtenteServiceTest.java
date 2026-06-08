//package it.unisa.fidelio.application;
//
//import it.unisa.fidelio.presentation.UtenteDTO;
//import it.unisa.fidelio.storage.Utente;
//import it.unisa.fidelio.storage.UtenteRepository;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.security.core.userdetails.UsernameNotFoundException;
//import org.springframework.security.crypto.password.PasswordEncoder;
//
//import java.util.Base64;
//import java.util.Optional;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class UtenteServiceTest {
//
//    @Mock
//    private UtenteRepository utenteRepository;
//
//    @Mock
//    private PasswordEncoder passwordEncoder;
//
//    @InjectMocks
//    private UtenteService utenteService;
//
//    // ===================================================================================
//    // 1. TEST REGISTRAZIONE
//    // ===================================================================================
//
//    @Test
//    void testRegistrazione_Successo() {
//        Utente nuovoUtente = new Utente();
//        nuovoUtente.setUsername("CinefiloTest");
//        nuovoUtente.setPassword("passwordInChiaro");
//
//        // Mock: quando codifica, restituisce una stringa finta
//        when(passwordEncoder.encode("passwordInChiaro")).thenReturn("hashedPassword");
//        // Mock: quando salva, restituisce l'oggetto passato
//        when(utenteRepository.save(any(Utente.class))).thenAnswer(i -> i.getArgument(0));
//
//        Utente risultato = utenteService.registrazione(nuovoUtente);
//
//        assertNotNull(risultato);
//        assertEquals("hashedPassword", risultato.getPassword()); // Verifica encoding
//        verify(utenteRepository).save(nuovoUtente);
//    }
//
//    // ===================================================================================
//    // 2. TEST LOGIN (Logica funzionale .filter .map)
//    // ===================================================================================
//
//    @Test
//    void testLogin_Successo() {
//        String email = "test@email.com";
//        String rawPass = "password123";
//        String hashedPass = "hashed123";
//
//        Utente u = new Utente();
//        u.setId(1);
//        u.setEmail(email);
//        u.setPassword(hashedPass);
//        u.setUsername("TestUser");
//        u.setNome("Nome");
//        u.setCognome("Cognome");
//        u.setDtype("Standard");
//        u.setAmministratore(false);
//
//        // Simuliamo che l'utente esista e la password coincida
//        when(utenteRepository.findByEmail(email)).thenReturn(Optional.of(u));
//        when(passwordEncoder.matches(rawPass, hashedPass)).thenReturn(true);
//
//        UtenteDTO result = utenteService.login(email, rawPass);
//
//        assertNotNull(result);
//        assertEquals("TestUser", result.getUsername());
//        assertEquals(email, result.getEmail());
//    }
//
//    @Test
//    void testLogin_PasswordErrata() {
//        String email = "test@email.com";
//        String rawPass = "wrongPass";
//        String hashedPass = "hashed123";
//
//        Utente u = new Utente();
//        u.setEmail(email);
//        u.setPassword(hashedPass);
//
//        when(utenteRepository.findByEmail(email)).thenReturn(Optional.of(u));
//        // Simuliamo password errata
//        when(passwordEncoder.matches(rawPass, hashedPass)).thenReturn(false);
//
//        UtenteDTO result = utenteService.login(email, rawPass);
//
//        // Deve ritornare null perché il .filter(...) scarta l'utente
//        assertNull(result);
//    }
//
//    @Test
//    void testLogin_UtenteNonTrovato() {
//        String email = "missing@email.com";
//        when(utenteRepository.findByEmail(email)).thenReturn(Optional.empty());
//
//        UtenteDTO result = utenteService.login(email, "anyPass");
//
//        assertNull(result);
//    }
//
//    // ===================================================================================
//    // 3. TEST MAPPING (DTO e Base64)
//    // ===================================================================================
//
//    @Test
//    void testMapToDTO_ConImmagine() {
//        Utente u = new Utente();
//        u.setId(10);
//        u.setUsername("UserImg");
//        u.setEmail("a@b.c");
//        u.setNome("N"); u.setCognome("C"); u.setDtype("D"); u.setAmministratore(false);
//
//        byte[] fakeImg = "fakeImageBytes".getBytes();
//        u.setImmagineProfilo(fakeImg);
//
//        UtenteDTO dto = utenteService.mapToDTO(u);
//
//        assertNotNull(dto);
//        assertNotNull(dto.getImmagineProfilo());
//        // Verifica che sia Base64
//        assertEquals(Base64.getEncoder().encodeToString(fakeImg), dto.getImmagineProfilo());
//    }
//
//    @Test
//    void testMapToDTO_SenzaImmagine() {
//        Utente u = new Utente();
//        u.setId(11);
//        u.setUsername("UserNoImg");
//        u.setEmail("a@b.c");
//        u.setNome("N"); u.setCognome("C"); u.setDtype("D"); u.setAmministratore(true);
//        u.setImmagineProfilo(null);
//
//        UtenteDTO dto = utenteService.mapToDTO(u);
//
//        assertNotNull(dto);
//        assertNull(dto.getImmagineProfilo());
//        assertTrue(dto.isAmministratore()); // Verifica passaggio booleano
//    }
//
//    // ===================================================================================
//    // 4. TEST METODI STATICI E DI UTILITÀ
//    // ===================================================================================
//
//    @Test
//    void testEncodeToBase64_StaticMethod() {
//        // Caso Null
//        assertEquals("", UtenteService.encodeToBase64(null));
//
//        // Caso Empty
//        assertEquals("", UtenteService.encodeToBase64(new byte[0]));
//
//        // Caso Valido
//        byte[] data = "Hello".getBytes();
//        String expected = Base64.getEncoder().encodeToString(data);
//        assertEquals(expected, UtenteService.encodeToBase64(data));
//    }
//
//    @Test
//    void testFindById() {
//        Utente u = new Utente(); u.setId(55);
//        when(utenteRepository.findById(55)).thenReturn(Optional.of(u));
//
//        Utente res = utenteService.findById(55);
//        assertEquals(55, res.getId());
//    }
//
//    @Test
//    void testExistsByUsername() {
//        when(utenteRepository.existsByUsername("mario")).thenReturn(true);
//        assertTrue(utenteService.existsByUsername("mario"));
//    }
//
//    @Test
//    void testExistsByEmail() {
//        when(utenteRepository.existsByEmail("a@a.com")).thenReturn(false);
//        assertFalse(utenteService.existsByEmail("a@a.com"));
//    }
//
//    @Test
//    void testFindByEmail() {
//        String mail = "trovami@mail.com";
//        Utente u = new Utente(); u.setEmail(mail);
//        when(utenteRepository.findByEmail(mail)).thenReturn(Optional.of(u));
//
//        assertEquals(u, utenteService.findByEmail(mail));
//    }
//
//    // ===================================================================================
//    // 5. TEST SPRING SECURITY (UserDetailsService)
//    // ===================================================================================
//
//    @Test
//    void testLoadUserByUsername_Successo() {
//        String email = "auth@test.com";
//        Utente u = new Utente();
//        u.setEmail(email);
//        u.setPassword("encodedPwd");
//
//        when(utenteRepository.findByEmail(email)).thenReturn(Optional.of(u));
//
//        UserDetails details = utenteService.loadUserByUsername(email);
//
//        assertNotNull(details);
//        assertEquals(email, details.getUsername());
//        assertEquals("encodedPwd", details.getPassword());
//        assertTrue(details.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("USER")));
//    }
//
//    @Test
//    void testLoadUserByUsername_NonTrovato() {
//        String email = "ghost@test.com";
//        when(utenteRepository.findByEmail(email)).thenReturn(Optional.empty());
//
//        assertThrows(UsernameNotFoundException.class, () ->
//                utenteService.loadUserByUsername(email)
//        );
//    }
//}

package it.unisa.fidelio.application;

import it.unisa.fidelio.presentation.UtenteDTO;
import it.unisa.fidelio.storage.Utente;
import it.unisa.fidelio.storage.UtenteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Base64;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UtenteServiceTest {

    @Mock
    private UtenteRepository utenteRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UtenteService utenteService;

    @Test
    void testRegistrazione_Successo() {
        Utente nuovoUtente = new Utente();
        nuovoUtente.setUsername("CinefiloTest");
        nuovoUtente.setPassword("passwordInChiaro");

        when(passwordEncoder.encode("passwordInChiaro")).thenReturn("hashedPassword");
        when(utenteRepository.save(any(Utente.class))).thenAnswer(i -> i.getArgument(0));

        Utente risultato = utenteService.registrazione(nuovoUtente);

        assertNotNull(risultato);
        assertEquals("hashedPassword", risultato.getPassword());
        verify(utenteRepository).save(nuovoUtente);
    }

    @Test
    void testLogin_Successo() {
        String email = "test@email.com";
        String rawPass = "password123";
        String hashedPass = "hashed123";

        Utente u = new Utente();
        u.setId(1);
        u.setEmail(email);
        u.setPassword(hashedPass);
        u.setUsername("TestUser");
        u.setNome("Nome");
        u.setCognome("Cognome");
        u.setDtype("Standard");
        u.setAmministratore(false);

        when(utenteRepository.findByEmail(email)).thenReturn(Optional.of(u));
        when(passwordEncoder.matches(rawPass, hashedPass)).thenReturn(true);

        UtenteDTO result = utenteService.login(email, rawPass);

        assertNotNull(result);
        assertEquals("TestUser", result.getUsername());
        assertEquals(email, result.getEmail());
    }

    @Test
    void testLogin_PasswordErrata() {
        String email = "test@email.com";
        String rawPass = "wrongPass";
        String hashedPass = "hashed123";

        Utente u = new Utente();
        u.setEmail(email);
        u.setPassword(hashedPass);

        when(utenteRepository.findByEmail(email)).thenReturn(Optional.of(u));
        when(passwordEncoder.matches(rawPass, hashedPass)).thenReturn(false);

        UtenteDTO result = utenteService.login(email, rawPass);
        assertNull(result);
    }

    @Test
    void testLogin_UtenteNonTrovato() {
        String email = "missing@email.com";
        when(utenteRepository.findByEmail(email)).thenReturn(Optional.empty());

        UtenteDTO result = utenteService.login(email, "anyPass");
        assertNull(result);
    }

    @Test
    void testMapToDTO_ConImmagine() {
        Utente u = new Utente();
        u.setId(10);
        u.setUsername("UserImg");
        u.setEmail("a@b.c");
        u.setNome("N");
        u.setCognome("C");
        u.setDtype("D");
        u.setAmministratore(false);

        byte[] fakeImg = "fakeImageBytes".getBytes();
        u.setImmagineProfilo(fakeImg);

        UtenteDTO dto = utenteService.mapToDTO(u);

        assertNotNull(dto);
        assertNotNull(dto.getImmagineProfilo());
        assertEquals(Base64.getEncoder().encodeToString(fakeImg), dto.getImmagineProfilo());
    }

    @Test
    void testMapToDTO_SenzaImmagine() {
        Utente u = new Utente();
        u.setId(11);
        u.setUsername("UserNoImg");
        u.setEmail("a@b.c");
        u.setNome("N");
        u.setCognome("C");
        u.setDtype("D");
        u.setAmministratore(true);
        u.setImmagineProfilo(null);

        UtenteDTO dto = utenteService.mapToDTO(u);

        assertNotNull(dto);
        assertNull(dto.getImmagineProfilo());
        assertTrue(dto.isAmministratore());
    }

    @Test
    void testEncodeToBase64_StaticMethod() {
        assertEquals("", UtenteService.encodeToBase64(null));
        assertEquals("", UtenteService.encodeToBase64(new byte[0]));

        byte[] data = "Hello".getBytes();
        String expected = Base64.getEncoder().encodeToString(data);
        assertEquals(expected, UtenteService.encodeToBase64(data));
    }

    @Test
    void testFindById() {
        Utente u = new Utente();
        u.setId(55);
        when(utenteRepository.findById(55)).thenReturn(Optional.of(u));

        Utente res = utenteService.findById(55);
        assertEquals(55, res.getId());
    }

    @Test
    void testExistsByUsername() {
        when(utenteRepository.existsByUsername("mario")).thenReturn(true);
        assertTrue(utenteService.existsByUsername("mario"));
    }

    @Test
    void testExistsByEmail() {
        when(utenteRepository.existsByEmail("a@a.com")).thenReturn(false);
        assertFalse(utenteService.existsByEmail("a@a.com"));
    }

    @Test
    void testFindByEmail() {
        String mail = "trovami@mail.com";
        Utente u = new Utente();
        u.setEmail(mail);
        when(utenteRepository.findByEmail(mail)).thenReturn(Optional.of(u));

        assertEquals(u, utenteService.findByEmail(mail));
    }

    @Test
    void testLoadUserByUsername_Successo() {
        String email = "auth@test.com";
        Utente u = new Utente();
        u.setEmail(email);
        u.setPassword("encodedPwd");

        when(utenteRepository.findByEmail(email)).thenReturn(Optional.of(u));

        UserDetails details = utenteService.loadUserByUsername(email);

        assertNotNull(details);
        assertEquals(email, details.getUsername());
        assertEquals("encodedPwd", details.getPassword());
        assertTrue(details.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("USER")));
    }

    @Test
    void testLoadUserByUsername_NonTrovato() {
        String email = "ghost@test.com";
        when(utenteRepository.findByEmail(email)).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> utenteService.loadUserByUsername(email));
    }
}