package it.unisa.fidelio.application.controller;

import it.unisa.fidelio.application.UtenteService;
import it.unisa.fidelio.presentation.RegistrazioneRequestDTO;
import it.unisa.fidelio.storage.Utente;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Base64;

@RestController
@RequestMapping("/api/registrazione")
@CrossOrigin(origins = "*")
public class RegistrazioneController {

    private final UtenteService utenteService;
    private final AuthenticationManager authenticationManager;

    private static final String REGEX_EMAIL = "^[A-z0-9._%+-]+@[A-z0-9.-]+\\.[A-z]{2,10}$";
    private static final String REGEX_PASSWORD_LEN = "^.{8,}$";
    private static final String REGEX_NOME_COGNOME = "^[A-zÀ-ù ‘-]{2,30}$";
    private static final String REGEX_TELEFONO = "^\\d{10}$";
    private static final String REGEX_INDIRIZZO = "^[0-9A-zÀ-ù ‘-]{2,30}$";
    private static final String REGEX_TESTATA = "^[\\p{L}0-9 .'&!?-]{2,100}$";
    private static final String REGEX_GENERICA_FEDELE = "^[A-zÀ-ù ‘-]{2,30}$";

    public RegistrazioneController(UtenteService utenteService, AuthenticationManager authenticationManager) {
        this.utenteService = utenteService;
        this.authenticationManager = authenticationManager;
    }

    @PostMapping
    public ResponseEntity<?> registra(@RequestBody RegistrazioneRequestDTO dto) {
        try {
            String username = dto.getUsername() != null ? dto.getUsername().trim() : null;
            String email = dto.getEmail() != null ? dto.getEmail().trim().toLowerCase() : null;
            String nome = dto.getNome() != null ? dto.getNome().trim() : null;
            String cognome = dto.getCognome() != null ? dto.getCognome().trim() : null;
            String indirizzo = dto.getViaEnumCivico() != null ? dto.getViaEnumCivico().trim() : null;

            String password = dto.getPassword();
            String confermaPassword = dto.getConfermaPassword();


            if (username == null || username.isEmpty() || username.length() > 30) {
                return ResponseEntity.badRequest().body("Errato: Nome utente troppo lungo o nullo");
            }

            if (email == null || !email.matches(REGEX_EMAIL)) {
                return ResponseEntity.badRequest().body("Errato: E-Mail non corretta");
            }

            if (password == null || !password.matches(REGEX_PASSWORD_LEN)) {
                return ResponseEntity.badRequest().body("Errato: lunghezza password non corretta (min 8)");
            }

            if (confermaPassword == null || !password.equals(confermaPassword)) {
                return ResponseEntity.badRequest().body("Errato: conferma password errata");
            }

            if (nome == null || !nome.matches(REGEX_NOME_COGNOME)) {
                return ResponseEntity.badRequest().body("Errato: nome non corretto");
            }

            if (cognome == null || !cognome.matches(REGEX_NOME_COGNOME)) {
                return ResponseEntity.badRequest().body("Errato: cognome non corretto");
            }


            if (indirizzo != null && !indirizzo.isEmpty() && !indirizzo.matches(REGEX_INDIRIZZO)) {
                return ResponseEntity.badRequest().body("Errato: Via e numero civico non corretto");
            }

            // Validazioni specifiche per Dtype
            String dtype = dto.getDtype() != null ? dto.getDtype() : "Cinefilo";

            if ("Critico".equals(dtype)) {
                String testata = dto.getTestataGiornalistica();
                if (testata == null || testata.trim().isEmpty() || !testata.matches(REGEX_TESTATA)) {
                    return ResponseEntity.badRequest().body("Errore: Testata giornalistica non trovata o formato errato");
                }
            } else if ("Fedele".equals(dtype)) {
                String casa = dto.getCasaProduzione();
                String credit = dto.getCreditReference();

                if (casa == null || casa.trim().isEmpty() || !casa.matches(REGEX_GENERICA_FEDELE)) {
                    return ResponseEntity.badRequest().body("Errore: Casa di produzione non trovata o formato errato");
                }

                if (credit == null || credit.trim().isEmpty() || !credit.matches(REGEX_GENERICA_FEDELE)) {
                    return ResponseEntity.badRequest().body("Errore: credit reference non valida");
                }
            }

            if (utenteService.existsByUsername(username)) {
                return ResponseEntity.badRequest().body("Username già in uso.");
            }
            if (utenteService.existsByEmail(email)) {
                return ResponseEntity.badRequest().body("Email già registrata.");
            }

            Utente nuovoUtente = new Utente();
            nuovoUtente.setNome(nome);
            nuovoUtente.setCognome(cognome);
            nuovoUtente.setUsername(username);
            nuovoUtente.setEmail(email);
            nuovoUtente.setPassword(password); // criptata nel service
            nuovoUtente.setDtype(dtype);
            nuovoUtente.setDataRegistrazione(java.time.Instant.now());
            nuovoUtente.setNumFilmVisti(0);
            nuovoUtente.setAmministratore(false);

            nuovoUtente.setViaENumCivico(indirizzo);

            if ("Critico".equals(dtype)) {
                nuovoUtente.setTestataGiornalistica(dto.getTestataGiornalistica());
            } else if ("Fedele".equals(dtype)) {
                nuovoUtente.setCasaProduzione(dto.getCasaProduzione());
                nuovoUtente.setCreditReference(dto.getCreditReference());
            }

            if (dto.getImmagineBase64() != null && dto.getImmagineBase64().contains(",")) {
                String base64Image = dto.getImmagineBase64().split(",")[1];
                byte[] imageBytes = Base64.getDecoder().decode(base64Image);
                nuovoUtente.setImmagineProfilo(imageBytes);
            }

            Utente salvato = utenteService.registrazione(nuovoUtente);

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password)
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpServletRequest request = attr.getRequest();
            HttpSession session = request.getSession(true);
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, SecurityContextHolder.getContext());

            return ResponseEntity.status(HttpStatus.CREATED).body(utenteService.mapToDTO(salvato));

        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.badRequest().body("Dati duplicati nel sistema.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Errore durante la registrazione.");
        }
    }
}