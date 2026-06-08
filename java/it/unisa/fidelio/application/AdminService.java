package it.unisa.fidelio.application;

import it.unisa.fidelio.storage.Segnalazione;
import it.unisa.fidelio.storage.SegnalazioneRepository;
import it.unisa.fidelio.storage.Utente;
import it.unisa.fidelio.storage.UtenteRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminService {

    private final SegnalazioneRepository segnalazioneRepo;
    private final UtenteRepository utenteRepo;

    public AdminService(SegnalazioneRepository segnalazioneRepo, UtenteRepository utenteRepo) {
        this.segnalazioneRepo = segnalazioneRepo;
        this.utenteRepo = utenteRepo;
    }

    public List<Segnalazione> getSegnalazioniAperte() {
        return segnalazioneRepo.findByStato("APERTA");
    }

    public void chiudiSegnalazione(int idSegnalazione, String esito, int idAdmin) {
        Segnalazione s = segnalazioneRepo.findById(idSegnalazione).orElseThrow();
        Utente admin = utenteRepo.findById(idAdmin).orElseThrow();

        s.setStato("CHIUSA");
        s.setEsito(esito);
        s.setGestore(admin); // Associa l'admin che ha risolto
        segnalazioneRepo.save(s);
    }
}
