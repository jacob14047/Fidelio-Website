const userDTO = JSON.parse(localStorage.getItem("user"));

// --- FUNZIONI DI UTILITÀ ---

function showAlert(message, type = "info") {
    const existingAlert = document.querySelector('.custom-alert');
    if (existingAlert) existingAlert.remove();

    const alertDiv = document.createElement('div');
    alertDiv.className = `custom-alert alert-${type}`;

    // Fallback CSS se manca
    if (!document.querySelector('style') || !document.querySelector('style').innerHTML.includes('.custom-alert')) {
         alertDiv.style.cssText = "position:fixed; top:20px; right:20px; padding:15px; border-radius:8px; z-index:9999; color:white; background:#333; border:1px solid #555; font-family: sans-serif;";
         if(type==='error') alertDiv.style.borderColor = '#ff4444';
         if(type==='success') alertDiv.style.borderColor = '#00C851';
    }

    alertDiv.innerHTML = `<span class="alert-message">${message}</span><button class="alert-close" onclick="this.parentElement.remove()" style="background:none; border:none; color:inherit; font-size:20px; margin-left:15px; cursor:pointer;">×</button>`;
    document.body.prepend(alertDiv);
    setTimeout(() => alertDiv.remove(), 4000);
}

// --- LOGICA UI ---

function setupUI() {
    const createBtn = document.getElementById('createBtn');
    if (!createBtn) return;

    if (userDTO && userDTO.dtype === "Fedele") {
        createBtn.style.display = "inline-block";
    } else {
        createBtn.style.display = "none";
    }
}

// --- API ---

async function loadCommunities() {
    try {
        const res = await fetch(`/api/community`);
        if (res.ok) {
            const communities = await res.json();
            const container = document.getElementById('communityContainer');
            container.innerHTML = '';

            if (communities.length === 0) {
                container.innerHTML = '<div style="grid-column: 1/-1; text-align: center; color: #666;">Nessuna community trovata.</div>';
                return;
            }

            communities.forEach(c => {
                const card = document.createElement('a');
                card.href = `/community-details?id=${c.id}`;
                card.className = 'comm-list-card';
                card.innerHTML = `
                    <div class="clc-icon">🎬</div>
                    <h3 class="clc-name">${c.nome}</h3>
                    <p class="clc-desc">${c.descrizione}</p>
                    <div class="clc-footer">
                        <span>${c.numMembri} Membri</span>
                        <span class="clc-link">Entra →</span>
                    </div>
                `;
                container.appendChild(card);
            });
        }
    } catch (err) { console.error(err); }
}

// --- MODALE E CREAZIONE ---

function openModal() {
    document.getElementById('createModal').classList.add('active');
}
function closeModal() { document.getElementById('createModal').classList.remove('active'); }

document.getElementById('createModal').addEventListener('click', (e) => {
    if (e.target === document.getElementById('createModal')) closeModal();
});

document.getElementById('createCommunityForm').addEventListener('submit', async (e) => {
    e.preventDefault();

    const nome = document.getElementById('cName').value.trim();
    const descrizione = document.getElementById('cDesc').value.trim();

    if (!nome) {
        showAlert("Inserisci un nome.", "warning");
        return;
    }

    try {
        const res = await fetch(`/api/community`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body: JSON.stringify({ nome, descrizione })
        });

        if (res.ok) {
            closeModal();
            document.getElementById('cName').value = '';
            document.getElementById('cDesc').value = '';
            loadCommunities();
            showAlert("Community creata!", "success");
        } else if (res.status === 403) {
            // Caso in cui un hacker prova a forzare la chiamata
            showAlert("Solo gli utenti Fedele possono creare community.", "error");
        } else {
            const json = await res.json();
            showAlert("Errore: " + (json.error || "Sconosciuto"), "error");
        }
    } catch (err) {
        showAlert("Errore di connessione.", "error");
    }
});

// Inizializzazione
loadCommunities();
setupUI(); // Applica i permessi al bottone