const signupForm = document.getElementById("signupForm");
const errorMsg = document.getElementById("signup-error");

// Funzione per leggere parametri URL
function getQueryParam(param) {
    const urlParams = new URLSearchParams(window.location.search);
    return urlParams.get(param);
}

// Funzione per attivare una card specifica
function selectTier(tierName) {
    if (!tierName) return;

    // Cerca la card con data-tier uguale al nome passato (case-insensitive)
    const targetCard = Array.from(document.querySelectorAll(".tier-card")).find(
        (card) => card.dataset.tier.toLowerCase() === tierName.toLowerCase(),
    );

    if (targetCard) {
        // Rimuovi selezione precedente
        document
            .querySelectorAll(".tier-card")
            .forEach((c) => c.classList.remove("selected"));

        // Attiva nuova selezione
        targetCard.classList.add("selected");
        document.getElementById("dtype").value = targetCard.dataset.tier;

        // Mostra/Nascondi campi extra
        const tier = targetCard.dataset.tier;
        document
            .getElementById("critico-fields")
            .classList.toggle("visible", tier === "Critico");
        document
            .getElementById("fedele-fields")
            .classList.toggle("visible", tier === "Fedele");
        errorMsg.style.display = "none";
    }
}

// --- INIT ---
// Leggi URL all'avvio (es: /signup?tier=Critico)
document.addEventListener("DOMContentLoaded", () => {
    const urlTier = getQueryParam("tier");
    if (urlTier) {
        selectTier(urlTier);
    }
});

// --- EVENT LISTENERS CLICK ---
document.querySelectorAll(".tier-card").forEach((card) => {
    card.addEventListener("click", () => {
        selectTier(card.dataset.tier);
    });
});

const toBase64 = (file) =>
    new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.readAsDataURL(file);
        reader.onload = () => resolve(reader.result);
        reader.onerror = (error) => reject(error);
    });

document.querySelectorAll(".tier-card").forEach((card) => {
    card.addEventListener("click", () => {
        document
            .querySelectorAll(".tier-card")
            .forEach((c) => c.classList.remove("selected"));
        card.classList.add("selected");
        const tier = card.dataset.tier;
        document.getElementById("dtype").value = tier;
        document
            .getElementById("critico-fields")
            .classList.toggle("visible", tier === "Critico");
        document
            .getElementById("fedele-fields")
            .classList.toggle("visible", tier === "Fedele");
        errorMsg.style.display = "none";
    });
});

signupForm.addEventListener("submit", async (e) => {
    e.preventDefault();
    errorMsg.style.display = "none";

    let fotoBase64 = null;
    const fotoFile = document.getElementById("fotoInput").files[0];
    if (fotoFile) {
        try {
            fotoBase64 = await toBase64(fotoFile);
        } catch (err) {}
    }

    const tier = document.getElementById("dtype").value;

    // PAYLOAD AGGIORNATO CON I NUOVI CAMPI
    const payload = {
        nome: document.getElementById("nome").value.trim(),
        cognome: document.getElementById("cognome").value.trim(),
        viaEnumCivico: document.getElementById("viaEnumCivico").value.trim(),
        username: document.getElementById("username").value.trim(),
        email: document.getElementById("email").value.trim(),
        password: document.getElementById("password").value,
        confermaPassword: document.getElementById("confermaPassword").value,

        dtype: tier,
        testataGiornalistica:
            tier === "Critico"
                ? document.getElementById("testata").value.trim()
                : null,
        casaProduzione:
            tier === "Fedele"
                ? document.getElementById("casa").value.trim()
                : null,
        creditReference:
            tier === "Fedele"
                ? document.getElementById("credit").value.trim()
                : null,
        immagineBase64: fotoBase64,
    };

    try {
        const response = await fetch("/api/registrazione", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload),
        });

        if (response.ok) {
            const userDTO = await response.json();
            localStorage.setItem("user", JSON.stringify(userDTO));
            window.location.href = "/home";
        } else {
            const text = await response.text();
            errorMsg.innerText = text || "Errore durante la registrazione.";
            errorMsg.style.display = "block";
        }
    } catch (err) {
        errorMsg.innerText = "Errore di connessione.";
        errorMsg.style.display = "block";
    }
});
