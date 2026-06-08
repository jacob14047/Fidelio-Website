//package it.unisa.fidelio.system;
//
//import io.github.bonigarcia.wdm.WebDriverManager;
//import it.unisa.fidelio.storage.Utente;
//import it.unisa.fidelio.storage.UtenteRepository;
//import org.junit.jupiter.api.AfterEach;
//import org.junit.jupiter.api.BeforeAll;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.params.ParameterizedTest;
//import org.junit.jupiter.params.provider.CsvSource;
//import org.junit.jupiter.params.provider.ValueSource;
//import org.openqa.selenium.*;
//import org.openqa.selenium.chrome.ChromeDriver;
//import org.openqa.selenium.chrome.ChromeOptions;
//import org.openqa.selenium.support.ui.ExpectedConditions;
//import org.openqa.selenium.support.ui.Select;
//import org.openqa.selenium.support.ui.WebDriverWait;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.security.crypto.password.PasswordEncoder;
//
//import java.time.Duration;
//import java.time.Instant;
//import java.util.HashSet;
//import java.util.List;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
//public class FidelioSystemTest {
//
//    @Autowired
//    private UtenteRepository utenteRepository;
//
//    @Autowired
//    private PasswordEncoder passwordEncoder;
//
//    private WebDriver driver;
//    private WebDriverWait wait;
//
//    @BeforeAll
//    public static void setupClass() {
//        WebDriverManager.chromedriver().setup();
//    }
//
//    @BeforeEach
//    public void setupTest() {
//        popolaDatabase();
//
//        ChromeOptions options = new ChromeOptions();
//        options.setBinary("/usr/bin/chromium");
//        options.addArguments("--headless=new");
//        options.addArguments("--no-sandbox");
//        options.addArguments("--disable-dev-shm-usage");
//        options.addArguments("--remote-allow-origins=*");
//        options.addArguments("--window-size=1920,1080");
//
//        driver = new ChromeDriver(options);
//        wait = new WebDriverWait(driver, Duration.ofSeconds(8)); // Timeout ridotto per velocizzare i 50 test
//    }
//
//    @AfterEach
//    public void tearDown() {
//        if (driver != null) driver.quit();
//        utenteRepository.deleteAll();
//    }
//
//    private void popolaDatabase() {
//        utenteRepository.deleteAll();
//
//        Utente user = new Utente();
//        user.setNome("Mario"); user.setCognome("Rossi"); user.setUsername("UserTest");
//        user.setEmail("user@test.it"); user.setPassword(passwordEncoder.encode("userpass"));
//        user.setDtype("Cinefilo"); user.setAmministratore(false);
//        user.setDataRegistrazione(Instant.now()); user.setRecensioni(new HashSet<>());
//        utenteRepository.save(user);
//
//        Utente admin = new Utente();
//        admin.setNome("Admin"); admin.setCognome("Super"); admin.setUsername("AdminTest");
//        admin.setEmail("admin@test.it"); admin.setPassword(passwordEncoder.encode("adminpass"));
//        admin.setDtype("Cinefilo"); admin.setAmministratore(true);
//        admin.setDataRegistrazione(Instant.now()); admin.setRecensioni(new HashSet<>());
//        utenteRepository.save(admin);
//    }
//
//    private void eseguiLogin(String email, String password) {
//        driver.get("http://localhost:8080/login");
//
//        WebElement userField = wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("username")));
//        WebElement passField = driver.findElement(By.name("password"));
//        WebElement submitBtn = driver.findElement(By.cssSelector("button[type='submit']"));
//
//        userField.clear(); userField.sendKeys(email);
//        passField.clear(); passField.sendKeys(password);
//        submitBtn.click();
//
//        if (driver.getCurrentUrl().contains("error")) {
//            throw new RuntimeException("Login fallito preventivo per " + email);
//        }
//
//        wait.until(ExpectedConditions.or(
//                ExpectedConditions.urlContains("home"),
//                ExpectedConditions.urlContains("profilo"),
//                ExpectedConditions.urlMatches("http://localhost:8080/$")
//        ));
//    }
//
//    // =================================================================================
//    // GRUPPO 1: LOGIN & AUTH (14 TEST TOTALI)
//    // =================================================================================
//
//    @Test
//    public void test01_LoginSuccesso() {
//        eseguiLogin("user@test.it", "userpass");
//        WebElement avatar = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".nav-actions--desktop .nav-avatar")));
//        assertTrue(avatar.isDisplayed());
//    }
//
//    @ParameterizedTest
//    @CsvSource({
//            "user@test.it, passwordSbagliata",    // Password errata
//            "emailinesistente@test.it, userpass", // Email non esiste
//            "user@test.it, ''",                   // Password vuota
//            "'', userpass",                       // Email vuota
//            "user@test.it, ' '",                  // Password spazio
//            "' or 1=1 --, userpass",              // SQL Injection user
//            "user@test.it, ' or 1=1 --",          // SQL Injection pass
//            "admin@test.it, wrong",               // Admin pass errata
//            "USER@TEST.IT, wrong",                // Case sensitivity check
//            "verylongemailaddress@thatdoesnotexistreallylong.com, a", // Lunghezza eccessiva
//            "<script>alert(1)</script>, pass",    // XSS attempt
//            "null, null"                          // Stringhe null
//    })
//    public void test02_LoginFalliti_Massivi(String email, String password) {
//        driver.get("http://localhost:8080/login");
//        driver.findElement(By.name("username")).sendKeys(email != null ? email : "");
//        driver.findElement(By.name("password")).sendKeys(password != null ? password : "");
//        driver.findElement(By.cssSelector("button[type='submit']")).click();
//
//        // Deve rimanere sul login o andare in errore
//        assertTrue(driver.getCurrentUrl().contains("login") || driver.getCurrentUrl().contains("error"));
//    }
//
//    @Test
//    public void test03_Logout_Desktop() {
//        eseguiLogin("user@test.it", "userpass");
//        try {
//            WebElement logoutBtn = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(".nav-actions--desktop .nav-logout-form button")));
//            logoutBtn.click();
//        } catch (Exception e) {
//            ((JavascriptExecutor) driver).executeScript("document.querySelector('.nav-actions--desktop .nav-logout-form button').click();");
//        }
//        wait.until(ExpectedConditions.or(
//                ExpectedConditions.urlContains("login"),
//                ExpectedConditions.urlContains("logout"),
//                ExpectedConditions.presenceOfElementLocated(By.cssSelector("a[href*='login']"))
//        ));
//    }
//
//    // =================================================================================
//    // GRUPPO 2: REGISTRAZIONE (10 TEST TOTALI)
//    // =================================================================================
//
//    @ParameterizedTest
//    @CsvSource({
//            "Mario, Rossi, Via1, User1, mail1@t.it, pass, pass",     // OK (Simuliamo successo ma verifichiamo solo navigazione)
//            " , Rossi, Via1, User2, mail2@t.it, pass, pass",         // No Nome
//            "Mario, , Via1, User3, mail3@t.it, pass, pass",          // No Cognome
//            "Mario, Rossi, , User4, mail4@t.it, pass, pass",         // No Via
//            "Mario, Rossi, Via1, , mail5@t.it, pass, pass",          // No Username
//            "Mario, Rossi, Via1, User6, , pass, pass",               // No Email
//            "Mario, Rossi, Via1, User7, mail7@t.it, , pass",         // No Pass
//            "Mario, Rossi, Via1, User8, mail8@t.it, pass, ",         // No Confirm
//            "Mario, Rossi, Via1, User9, mail9@t.it, pass, WRONG",    // Mismatch Pass
//            "Mario, Rossi, Via1, User10, bad-email, pass, pass"      // Bad Email Format
//    })
//    public void test04_Registrazione_Massiva(String n, String c, String v, String u, String e, String p, String cp) {
//        driver.get("http://localhost:8080/signup");
//
//        if(n != null && !n.isBlank()) driver.findElement(By.id("nome")).sendKeys(n);
//        if(c != null && !c.isBlank()) driver.findElement(By.id("cognome")).sendKeys(c);
//        if(v != null && !v.isBlank()) driver.findElement(By.id("viaEnumCivico")).sendKeys(v);
//        if(u != null && !u.isBlank()) driver.findElement(By.id("username")).sendKeys(u);
//        if(e != null && !e.isBlank()) driver.findElement(By.id("email")).sendKeys(e);
//        if(p != null && !p.isBlank()) driver.findElement(By.id("password")).sendKeys(p);
//        if(cp != null && !cp.isBlank()) driver.findElement(By.id("confermaPassword")).sendKeys(cp);
//
//        driver.findElement(By.cssSelector("button[type='submit']")).click();
//
//        // Verifica basica: se mancano dati, non deve andare in home
//        if (n == null || n.isBlank() || p == null || !p.equals(cp)) {
//            assertFalse(driver.getCurrentUrl().contains("/home"), "Non doveva registrare con dati invalidi: " + u);
//        }
//    }
//
//    // =================================================================================
//    // GRUPPO 3: NAVIGAZIONE & SICUREZZA (8 TEST TOTALI)
//    // =================================================================================
//
//    @ParameterizedTest
//    @ValueSource(strings = {"/", "/login", "/signup"})
//    public void test05_PaginePubbliche(String path) {
//        driver.get("http://localhost:8080" + path);
//        assertFalse(driver.getPageSource().contains("Whitelabel Error Page"));
//    }
//
//    @ParameterizedTest
//    @ValueSource(strings = {"/profilo", "/admin", "/diary", "/lists", "/scrivi-recensione"})
//    public void test06_RedirectLogin_SeNonAutenticato(String path) {
//        driver.get("http://localhost:8080" + path);
//        // Senza login, deve mandarci al login
//        wait.until(ExpectedConditions.urlContains("login"));
//    }
//
//    // =================================================================================
//    // GRUPPO 4: RICERCA E FILTRI (18 TEST TOTALI)
//    // =================================================================================
//
//    @ParameterizedTest
//    @ValueSource(strings = {
//            "Dune", "Avatar", "Godfather", "Matrix", "Titanic", "Inception",
//            "Joker", "Rocky", "Star Wars", "Avengers", "Spiderman", "Batman"
//    })
//    public void test07_Ricerca_Massiva(String query) {
//        eseguiLogin("user@test.it", "userpass");
//
//        WebElement searchInput = driver.findElement(By.cssSelector(".nav-search--desktop input[name='q']"));
//        searchInput.clear();
//        searchInput.sendKeys(query);
//        driver.findElement(By.cssSelector(".nav-search--desktop .nav-search-btn")).click();
//
//        // Controlliamo che la pagina di ricerca carichi senza errori 500
//        assertFalse(driver.getPageSource().contains("Internal Server Error"));
//        // Controlliamo che appaia il testo della query nella barra o nel risultato o URL
//        assertTrue(driver.getCurrentUrl().contains("search") || driver.getCurrentUrl().contains("q="));
//    }
//
//    @ParameterizedTest
//    @ValueSource(strings = {"Azione", "Commedia", "Horror"})
//    public void test08_FiltroGenere(String genere) {
//        eseguiLogin("user@test.it", "userpass");
//        Select selectGenere = new Select(driver.findElement(By.id("filter-genere")));
//        selectGenere.selectByValue(genere);
//        driver.findElement(By.cssSelector(".nav-search--desktop .nav-search-btn")).click();
//
//        assertTrue(driver.getCurrentUrl().contains("genere=" + genere) || driver.getPageSource().contains(genere));
//    }
//
//    @ParameterizedTest
//    @ValueSource(strings = {"2025", "2024", "2023"})
//    public void test09_FiltroAnno(String anno) {
//        eseguiLogin("user@test.it", "userpass");
//        Select selectAnno = new Select(driver.findElement(By.id("filter-anno")));
//        selectAnno.selectByValue(anno);
//        driver.findElement(By.cssSelector(".nav-search--desktop .nav-search-btn")).click();
//
//        assertTrue(driver.getCurrentUrl().contains("anno=" + anno) || driver.getPageSource().contains(anno));
//    }
//
//    // =================================================================================
//    // GRUPPO 5: UI & INTEGRITY (3 TEST TOTALI)
//    // =================================================================================
//
//    @Test
//    public void test10_LogoPresente() {
//        driver.get("http://localhost:8080/");
//        WebElement logo = driver.findElement(By.cssSelector(".nav-logo img"));
//        assertTrue(logo.isDisplayed());
//    }
//
//    @Test
//    public void test11_MenuMobile_EsistenzaDom() {
//        // Verifica che il codice HTML del menu mobile esista (anche se nascosto)
//        driver.get("http://localhost:8080/");
//        List<WebElement> menu = driver.findElements(By.id("mobile-menu"));
//        assertFalse(menu.isEmpty());
//    }
//
//    @Test
//    public void test12_Footer_O_StrutturaBase() {
//        driver.get("http://localhost:8080/");
//        // Verifica generica che il body non sia vuoto
//        assertTrue(driver.findElement(By.tagName("body")).getText().length() > 0);
//    }
//}