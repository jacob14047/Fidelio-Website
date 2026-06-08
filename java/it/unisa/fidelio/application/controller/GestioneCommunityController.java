package it.unisa.fidelio.application.controller; // O semplicemente 'application' se non usi sottocartelle

import it.unisa.fidelio.application.CommunityService;
import it.unisa.fidelio.presentation.CommunityDTO;
import it.unisa.fidelio.presentation.ThreadDTO;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/community")
@CrossOrigin(origins = "*")
public class GestioneCommunityController {

    private final CommunityService communityService;

    @Autowired
    public GestioneCommunityController(CommunityService communityService) {
        this.communityService = communityService;
    }

    // --- ENDPOINTS CRUD ---

    /**
     * GET /api/community
     * Restituisce tutte le community
     */
    @GetMapping
    public ResponseEntity<List<CommunityDTO>> getAllCommunities() {
        List<CommunityDTO> communities = communityService.findAll();
        return ResponseEntity.ok(communities);
    }

    //TODO forse non scelta finale chiedere l'id in URL (token sicurezza?)
    /**
     * GET /api/community/{id}
     * Restituisce una singola community per ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<CommunityDTO> getCommunityById(@PathVariable Integer id) {
        try {
            CommunityDTO community = communityService.findById(id);
            return ResponseEntity.ok(community);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * POST /api/community
     * Crea una nuova community.
     * Richiede autenticazione.
     */
    @PostMapping
    public ResponseEntity<?> createCommunity(
            @RequestBody CommunityDTO communityDTO,
            @AuthenticationPrincipal UserDetails userDetails) {

        // 1. Controllo base autenticazione
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(java.util.Collections.singletonMap("error", "Devi essere autenticato."));
        }

        try {
            // 2. Chiamata al Service passando lo USERNAME
            CommunityDTO created = communityService.creaCommunity(communityDTO, userDetails.getUsername());

            return new ResponseEntity<>(created, HttpStatus.CREATED);

        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(java.util.Collections.singletonMap("error", e.getMessage()));

        } catch (SecurityException e) {
            // 3. Gestione ruolo non autorizzato (non è Fedele)
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(java.util.Collections.singletonMap("error", e.getMessage()));
        }
    }

    /**
     * PUT /api/community/{id}
     * Aggiorna i dati di una community esistente
     */
    @PutMapping("/{id}")
    public ResponseEntity<CommunityDTO> updateCommunity(
            @PathVariable Integer id,
            @RequestBody CommunityDTO communityDTO) {

        try {
            CommunityDTO updated = communityService.aggiornaCommunity(id, communityDTO);
            return ResponseEntity.ok(updated);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * DELETE /api/community/{id}
     * Elimina una community
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCommunity(@PathVariable Integer id) {
        try {
            communityService.eliminaCommunity(id);
            return ResponseEntity.noContent().build(); // 204 No Content
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
 * GET /api/community/{id}/iscrizione
 * Controlla se l'utente loggato è iscritto.
 * Restituisce JSON: { "iscritto": true/false }
 */
    @GetMapping("/{id}/iscrizione")
    public ResponseEntity<Map<String, Boolean>> checkIscrizione(
            @PathVariable Integer id,
            @AuthenticationPrincipal UserDetails userDetails) {

        // Se non è loggato, sicuramente non è iscritto
        if (userDetails == null) {
            return ResponseEntity.ok(Collections.singletonMap("iscritto", false));
        }

        try {
            boolean isIscritto = communityService.isUtenteIscritto(id, userDetails.getUsername());
            return ResponseEntity.ok(Collections.singletonMap("iscritto", isIscritto));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * POST /api/community/{id}/iscrizione
     * Iscrive un utente alla community.
     */
    @PostMapping("/{id}/iscrizione")
    public ResponseEntity<?> joinCommunity(
            @PathVariable Integer id,
            @AuthenticationPrincipal UserDetails userDetails) {

        // 1. Controllo di sicurezza: l'utente è loggato?
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(java.util.Collections.singletonMap("error", "Devi essere autenticato per iscriverti."));
        }

        try {
            // 2. Chiamiamo il service passando lo USERNAME (dal token), non l'ID
            communityService.iscriviUtente(id, userDetails.getUsername());

            // 3. Ritorniamo un JSON pulito
            return ResponseEntity.ok(java.util.Collections.singletonMap("message", "Iscrizione avvenuta con successo"));

        } catch (EntityNotFoundException e) {
            // Community o Utente non trovati nel DB
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(java.util.Collections.singletonMap("error", e.getMessage()));

        } catch (IllegalStateException e) {
            // Utente già iscritto (Evitiamo duplicati) -> 409 Conflict
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(java.util.Collections.singletonMap("error", e.getMessage()));
        }
    }

    /**
     * DELETE /api/community/{id}/iscrizione
     * Rimuove l'iscrizione dell'utente loggato.
     */
    @DeleteMapping("/{id}/iscrizione")
    public ResponseEntity<?> leaveCommunity(
            @PathVariable Integer id,
            @AuthenticationPrincipal UserDetails userDetails) {

        // 1. Sicurezza: Utente loggato?
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(java.util.Collections.singletonMap("error", "Devi essere autenticato."));
        }

        try {
            // 2. Chiamata al service con Username sicuro
            communityService.disiscriviUtente(id, userDetails.getUsername());

            // 3. Risposta JSON
            return ResponseEntity.ok(java.util.Collections.singletonMap("message", "Disiscrizione avvenuta con successo"));

        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(java.util.Collections.singletonMap("error", e.getMessage()));
        } catch (IllegalStateException e) {
            // Caso in cui provo a disiscrivermi ma non ero iscritto (opzionale)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(java.util.Collections.singletonMap("error", e.getMessage()));
        }
    }

        // ==========================================
    // ENDPOINTS PER I THREAD (Nested Resources)
    // ==========================================

    /**
     * GET /api/community/{communityId}/threads
     * Legge tutti i thread di quella community
     */
    @GetMapping("/{communityId}/threads")
    public ResponseEntity<List<ThreadDTO>> getThreads(@PathVariable Integer communityId) {
        try {
            return ResponseEntity.ok(communityService.findThreadsByCommunity(communityId));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * POST /api/community/{communityId}/threads
     * Crea un thread IN quella community.
     * L'autore viene recuperato dalla sessione di Spring Security.
     */
    @PostMapping("/{communityId}/threads")
    public ResponseEntity<?> createThread(
            @PathVariable Integer communityId,
            @RequestBody ThreadDTO threadDTO,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            if (userDetails == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Devi essere autenticato.");
            }
            
            ThreadDTO created = communityService.creaThread(communityId, threadDTO, userDetails.getUsername());
            return new ResponseEntity<>(created, HttpStatus.CREATED);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (SecurityException | IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    /**
     * DELETE /api/community/threads/{threadId}
     * Elimina un thread specifico.
     * Nota: Qui non serve l'ID della community nell'URL perché l'ID del thread è univoco.
     */
    @DeleteMapping("/threads/{threadId}")
    public ResponseEntity<Void> deleteThread(@PathVariable Integer threadId) {
        try {
            communityService.eliminaThread(threadId);
            return ResponseEntity.noContent().build();
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
}