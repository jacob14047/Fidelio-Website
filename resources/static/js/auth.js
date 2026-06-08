document.addEventListener("DOMContentLoaded", () => {
    const storedUser = localStorage.getItem("user");
    const usernameDisplay = document.getElementById("nav-username-placeholder");
    const avatarImg = document.getElementById("nav-avatar");
    const logoutBtn = document.getElementById("logoutBtn");

    if (storedUser) {
        const user = JSON.parse(storedUser);
        document.body.classList.add("is-logged");

        if (usernameDisplay) usernameDisplay.innerText = user.username;

        // MOSTRA AVATAR SE PRESENTE
        if (avatarImg && user.immagineProfilo) {
            avatarImg.src = user.immagineProfilo;
            avatarImg.style.display = "block";
        }

        if (logoutBtn) {
            logoutBtn.addEventListener("click", (e) => {
                e.preventDefault();
                if (confirm("Vuoi uscire?")) {
                    localStorage.removeItem("user");
                    window.location.href = "/";
                }
            });
        }
    }
});