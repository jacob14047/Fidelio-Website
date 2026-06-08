const params = new URLSearchParams(window.location.search);
const commId = params.get("id");
// Recuperiamo l'utente salvato al login
const userDTO = JSON.parse(localStorage.getItem("user"));
let isMember = false;

// --- INIZIALIZZAZIONE ---

async function init() {
    if (!commId) return;

    // Stato Loading Bottone
    const btn = document.getElementById("joinBtn");
    if(btn) {
        btn.innerText = "...";
        btn.disabled = true;
        btn.style.opacity = "0.6";
    }

    try {
        // 1. Info Community
        const resC = await fetch(`/api/community/${commId}`);
        if (resC.ok) {
            const comm = await resC.json();
            document.getElementById("cName").innerText = comm.nome;
            document.getElementById("cDesc").innerText = comm.descrizione;
            document.getElementById("cMembers").innerText = `${comm.numMembri} Membri`;

            // 2. Check Iscrizione (usa timestamp per evitare cache)
            const checkRes = await fetch(`/api/community/${commId}/iscrizione`, {
                method: "GET",
                credentials: "include",
                headers: { "Cache-Control": "no-cache" }
            });

            if (checkRes.ok) {
                const data = await checkRes.json();
                updateUI(data.iscritto);
            } else {
                updateUI(false);
            }
        }
    } catch (e) {
        updateUI(false);
    }

    loadThreads();
}

// --- GESTIONE UI E PERMESSI ---

function updateUI(joined) {
    isMember = joined;
    const btn = document.getElementById("joinBtn");
    const inputArea = document.getElementById("inputArea");

    // Reset bottone
    btn.disabled = false;
    btn.style.opacity = "1";

    if (joined) {
        btn.innerText = "Lascia Community";
        btn.classList.remove("primary"); // Stile grigio/secondario

        if (userDTO && userDTO.dtype === "Critico")
        {
            inputArea.style.display = "block";
        } else {
            inputArea.style.display = "none";
        }

    } else {
        btn.innerText = "Unisciti";
        btn.classList.add("primary");
        inputArea.style.display = "none";
    }
}

// --- API THREAD ---

async function loadThreads() {
    const resT = await fetch(`/api/community/${commId}/threads`);
    const list = document.getElementById("threadList");
    list.innerHTML = "";
    if (resT.ok) {
        const threads = await resT.json();
        if (threads.length === 0) {
            list.innerHTML = '<div style="text-align:center; padding:40px; color:var(--muted);">Nessuna discussione.</div>';
            return;
        }
        threads.forEach((t) => {
            const el = document.createElement("a");
            el.href = "#";
            el.className = "thread-card";
            el.innerHTML = `
                <div class="tc-head">
                    <div class="tc-user">
                        <span class="tc-username">${t.autoreUsername}</span>
                    </div>
                    <span class="tc-date">${new Date(t.dataCreazione).toLocaleDateString()}</span>
                </div>
                <div class="tc-title">${t.titolo}</div>
                <div class="tc-preview">${t.contenuto}</div>
                <div class="tc-footer">💬 ${t.numRisposte} commenti</div>
            `;
            list.appendChild(el);
        });
    }
}

async function postThread() {
    const title = document.getElementById("tTitle").value;
    const content = document.getElementById("tContent").value;
    if (!title) return;

    try {
        const res = await fetch(`/api/community/${commId}/threads`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            credentials: "include",
            body: JSON.stringify({ titolo: title, contenuto: content }),
        });

        if (res.ok) {
            document.getElementById("tTitle").value = "";
            document.getElementById("tContent").value = "";
            loadThreads();
            showAlert("Discussione pubblicata!", "success");
        } else if (res.status === 401) {
            showAlert("Sessione scaduta. Rifai il login.", "error");
        } else if (res.status === 403) {
            showAlert("Permesso negato: Solo i Critici possono pubblicare.", "error");
        } else {
            const txt = await res.text();
            showAlert("Errore: " + txt, "error");
        }
    } catch (e) {
        console.error(e);
    }
}

async function toggleJoin() {
    const method = isMember ? "DELETE" : "POST";

    try {
        const res = await fetch(`/api/community/${commId}/iscrizione`, {
            method: method,
            credentials: "include",
        });

        if (res.ok) {
            updateUI(!isMember);
            const counter = document.getElementById("cMembers");
            let count = parseInt(counter.innerText);
            counter.innerText = `${isMember ? count + 1 : count - 1} Membri`;
            showAlert(isMember ? "Ti sei iscritto!" : "Hai lasciato la community.", "success");
        } else if (res.status === 401) {
            showAlert("Devi effettuare il login.", "warning");
        } else if (res.status === 409) {
            updateUI(true);
            showAlert("Eri già iscritto. Stato aggiornato.", "info");
        } else {
            showAlert("Errore operazione.", "error");
        }
    } catch (e) {
        showAlert("Errore di connessione.", "error");
    }
}

// --- ALERT SYSTEM ---

function showAlert(message, type = "info") {
    const existingAlert = document.querySelector(".custom-alert");
    if (existingAlert) existingAlert.remove();

    const alertDiv = document.createElement("div");
    alertDiv.className = `custom-alert alert-${type}`;

    // Fallback CSS
    if (!document.querySelector("link[href*='home.css']")) {
         alertDiv.style.cssText = "position:fixed; top:20px; right:20px; padding:15px; background:#333; color:white; border-radius:8px; z-index:9999; border:1px solid #555;";
         if(type==='success') alertDiv.style.borderColor = '#00C851';
         if(type==='error') alertDiv.style.borderColor = '#ff4444';
    }

    alertDiv.innerHTML = `
        <span class="alert-message">${message}</span>
        <button class="alert-close" onclick="this.parentElement.remove()" style="background:none; border:none; color:white; font-size:20px; margin-left:10px; cursor:pointer;">×</button>
    `;
    document.body.prepend(alertDiv);
    setTimeout(() => alertDiv.remove(), 4000);
}

init();