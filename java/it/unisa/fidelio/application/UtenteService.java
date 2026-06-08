package it.unisa.fidelio.application;

import it.unisa.fidelio.presentation.UtenteDTO;
import it.unisa.fidelio.storage.Utente;
import it.unisa.fidelio.storage.UtenteRepository;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Base64;

@Service
public class UtenteService implements UserDetailsService {

    private final UtenteRepository utenteRepository;
    private final PasswordEncoder passwordEncoder;

    public UtenteService(UtenteRepository utenteRepository, PasswordEncoder passwordEncoder) {
        this.utenteRepository = utenteRepository;
        this.passwordEncoder = passwordEncoder;
    }


    public boolean existsByUsername(String username) {
        return utenteRepository.existsByUsername(username);
    }

    public boolean existsByEmail(String email) {
        return utenteRepository.existsByEmail(email);
    }

    public Utente findByEmail(String email) {
        return utenteRepository.findByEmail(email).orElse(null);
    }

    public Utente findById(Integer id) {
        return utenteRepository.findById(id).orElse(null);
    }

    public UtenteDTO login(String email, String password) {
        return utenteRepository.findByEmail(email)
                .filter(u -> passwordEncoder.matches(password, u.getPassword()))
                .map(this::mapToDTO)
                .orElse(null);
    }

    public Utente registrazione(Utente nuovo) {
        // Crittografa la password
        nuovo.setPassword(passwordEncoder.encode(nuovo.getPassword()));
        return utenteRepository.save(nuovo);
    }

    public UtenteDTO mapToDTO(Utente u) {
        String immagineBase64 = null;
        if (u.getImmagineProfilo() != null) {
            immagineBase64 = Base64.getEncoder().encodeToString(u.getImmagineProfilo());
        }
        return new UtenteDTO(
                u.getId(),
                u.getUsername(),
                u.getEmail(),
                u.getNome(),
                u.getCognome(),
                u.getDtype(), u.getBio(),
                immagineBase64,
                u.getAmministratore()
        );
    }

    public static String encodeToBase64(byte[] image) {
        if (image == null || image.length == 0) {
            return "";
        }
        return Base64.getEncoder().encodeToString(image);
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Utente utente = utenteRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Utente non trovato: " + email));

        return new User(
                utente.getEmail(),
                utente.getPassword(),
                true, true, true, true,
                AuthorityUtils.createAuthorityList("USER")
        ) {
            public Utente getUtente() {
                return utente;
            }
        };
    }
}