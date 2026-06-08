package it.unisa.fidelio.application;

import it.unisa.fidelio.presentation.CommunityDTO;
import it.unisa.fidelio.storage.Community;
import it.unisa.fidelio.storage.CommunityRepository;
import it.unisa.fidelio.storage.Thread;
import it.unisa.fidelio.storage.ThreadRepository;
import it.unisa.fidelio.presentation.ThreadDTO;
import it.unisa.fidelio.storage.Utente;
import it.unisa.fidelio.storage.UtenteRepository;
import jakarta.persistence.EntityNotFoundException;

import it.unisa.fidelio.storage.CommunityMembriRepository;
import it.unisa.fidelio.storage.CommunityMembriId;
import it.unisa.fidelio.storage.CommunityMembri;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class CommunityService {

    private final CommunityRepository communityRepo;
    private final UtenteRepository utenteRepo;
    private final ThreadRepository threadRepo;
    private final CommunityMembriRepository communityMembriRepo;

    @Autowired
    public CommunityService(CommunityRepository communityRepo, 
                            UtenteRepository utenteRepo, 
                            ThreadRepository threadRepo,
                            CommunityMembriRepository communityMembriRepo) {
        this.communityRepo = communityRepo;
        this.utenteRepo = utenteRepo;
        this.threadRepo = threadRepo;
        this.communityMembriRepo = communityMembriRepo;
    }


    public List<CommunityDTO> findAll() {
        return communityRepo.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public CommunityDTO findById(Integer id) {
        Community community = communityRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Community non trovata con ID: " + id));
        return mapToDTO(community);
    }

    public CommunityDTO creaCommunity(CommunityDTO communityDTO, String emailCreatore) {

        // 1. Recupera l'utente dallo email
        Utente creatore = utenteRepo.findByEmail(emailCreatore)
                .orElseThrow(() -> new EntityNotFoundException("Utente creatore non trovato"));

        // 2. Controllo Ruolo: Solo i "Fedele" possono creare
        // Usa il metodo isFedele() che abbiamo messo nell'Entity, o controlla la stringa
        if (!creatore.isFedele()) {
            throw new SecurityException("Operazione negata: Solo gli utenti 'Fedele' possono creare community.");
        }

        // 3. Creazione Entità
        Community community = new Community();
        community.setNome(communityDTO.getNome());
        community.setDescrizione(communityDTO.getDescrizione());
        community.setCreatore(creatore);
        community.setDataCreazione(Instant.now());

        // Il creatore diventa automaticamente il primo membro
        community.setNumMembri(1);

        // Gestione relazione bidirezionale (importante!)
        community.getUtentiIscritti().add(creatore);
        creatore.getCommunitiesIscritte().add(community);

        // 4. Salvataggio
        // Salviamo prima la community, poi aggiorniamo l'utente se necessario
        Community saved = communityRepo.save(community);
        utenteRepo.save(creatore); // Salva l'associazione lato utente

        return mapToDTO(saved);
    }

    public CommunityDTO aggiornaCommunity(Integer communityId, CommunityDTO datiAggiornati) {
        Community community = communityRepo.findById(communityId)
                .orElseThrow(() -> new EntityNotFoundException("Community non trovata"));

        community.setNome(datiAggiornati.getNome());
        community.setDescrizione(datiAggiornati.getDescrizione());

        return mapToDTO(communityRepo.save(community));
    }

    public void eliminaCommunity(Integer communityId) {
        if (!communityRepo.existsById(communityId)) {
            throw new EntityNotFoundException("Impossibile eliminare: Community non trovata");
        }
        communityRepo.deleteById(communityId);
    }


    public void iscriviUtente(Integer communityId, String email) {

        Community community = communityRepo.findById(communityId).orElseThrow(() -> new EntityNotFoundException("Community non trovata"));

        // Recuperiamo l'utente sicuro tramite email
        Utente utente = utenteRepo.findByEmail(email).orElseThrow(() -> new EntityNotFoundException("Utente non trovato nel sistema" + email));

        // Controllo: è già iscritto?
        if (utente.getCommunitiesIscritte().contains(community)) {
            throw new IllegalStateException("Sei già iscritto a questa community.");
        }

        // Logica di iscrizione
        utente.getCommunitiesIscritte().add(community);

        // Aggiorna contatore
        community.setNumMembri(community.getNumMembri() + 1);

        utenteRepo.save(utente);
        communityRepo.save(community);
    }

    public void disiscriviUtente(Integer communityId, String email) {

        Community community = communityRepo.findById(communityId)
                .orElseThrow(() -> new EntityNotFoundException("Community non trovata"));

        Utente utente = utenteRepo.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Utente non trovato"));

        // Grazie al tuo equals() corretto, questo funziona perfettamente:
        if (utente.getCommunitiesIscritte().contains(community)) {

            // 1. Rimuovi dalla relazione (Lato proprietario: Utente)
            utente.getCommunitiesIscritte().remove(community);

            // 2. Aggiorna contatore (evitando numeri negativi)
            int nuoviMembri = Math.max(0, community.getNumMembri() - 1);
            community.setNumMembri(nuoviMembri);

            // 3. Salva i cambiamenti
            utenteRepo.save(utente);
            communityRepo.save(community);

        } else {
            // Opzionale: lanciare errore se l'utente non era iscritto
            throw new IllegalStateException("Non sei iscritto a questa community.");
        }
    }

    public boolean isUtenteIscritto(Integer communityId, String username) {
    Community community = communityRepo.findById(communityId)
            .orElseThrow(() -> new EntityNotFoundException("Community non trovata"));

    Utente utente = utenteRepo.findByEmail(username)
            .orElseThrow(() -> new EntityNotFoundException("Utente non trovato"));

    // Funziona grazie ai tuoi equals/hashCode corretti
    return utente.getCommunitiesIscritte().contains(community);
}


    private CommunityDTO mapToDTO(Community entity) {
        CommunityDTO dto = new CommunityDTO();
        dto.setId(entity.getId());
        dto.setNome(entity.getNome());
        dto.setDescrizione(entity.getDescrizione());
        dto.setDataCreazione(entity.getDataCreazione());
        dto.setNumMembri(entity.getNumMembri());

        if (entity.getCreatore() != null) {
            dto.setCreatoreUsername(entity.getCreatore().getUsername());
        }

        return dto;
    }

    /**
     * Crea un thread - versione sicura che accetta l'email dell'utente autenticato
     */
    public ThreadDTO creaThread(Integer communityId, ThreadDTO threadDTO, String emailAutore) {
        Community community = communityRepo.findById(communityId)
                .orElseThrow(() -> new EntityNotFoundException("Community non trovata"));

        Utente autore = utenteRepo.findByEmail(emailAutore)
                .orElseThrow(() -> new EntityNotFoundException("Utente non trovato"));

        // BUSINESS LOGIC - dtype viene dal database, non dal client!
        if (!autore.isCritico()) {
            throw new SecurityException("Operazione negata: Solo i 'Critici' possono aprire nuove discussioni.");
        }
        if (!autore.getCommunitiesIscritte().contains(community)) {
            throw new IllegalStateException("L'utente non è iscritto e non può pubblicare.");
        }

        Thread thread = new Thread();
        thread.setTitolo(threadDTO.getTitolo());
        thread.setContenuto(threadDTO.getContenuto());
        thread.setDataCreazione(Instant.now());
        thread.setNumRisposte(0);
        thread.setCommunity(community);
        thread.setAutore(autore);

        return mapThreadToDTO(threadRepo.save(thread));
    }

    public void eliminaThread(Integer threadId) {
        if (!threadRepo.existsById(threadId)) {
            throw new EntityNotFoundException("Thread non trovato");
        }
        threadRepo.deleteById(threadId);
    }

    private ThreadDTO mapThreadToDTO(Thread entity) {
        ThreadDTO dto = new ThreadDTO();
        dto.setId(entity.getId());
        dto.setTitolo(entity.getTitolo());
        dto.setContenuto(entity.getContenuto());
        dto.setDataCreazione(entity.getDataCreazione());
        dto.setNumRisposte(entity.getNumRisposte());
        dto.setAutoreUsername(entity.getAutore().getUsername());
        dto.setCommunityId(entity.getCommunity().getId());
        dto.setCommunityNome(entity.getCommunity().getNome());
        return dto;
    }

    public List<ThreadDTO> findThreadsByCommunity(Integer communityId) {
        if (!communityRepo.existsById(communityId)) {
            throw new EntityNotFoundException("Community non trovata");
        }
        return threadRepo.findByCommunityIdOrderByDataCreazioneDesc(communityId).stream()
                .map(this::mapThreadToDTO)
                .collect(Collectors.toList());
    }
}