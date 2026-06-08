package it.unisa.fidelio;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Forziamo esplicitamente la classe di configurazione.
 * Questo risolve l'errore "Unable to find a @SpringBootConfiguration".
 */
@SpringBootTest(classes = FidelioApplication.class)
class FidelioApplicationTests {

    @Test
    void contextLoads() {
        // Se il test passa, il sistema di package e il DB H2 sono configurati bene
    }

}