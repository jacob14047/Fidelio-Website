// Micro: evidenzia link attivo in base al path (se vuoi)
(() => {
    const path = window.location.pathname;
    document.querySelectorAll(".nav-link").forEach(a => {
        const href = a.getAttribute("href");
        if (href && href !== "/" && path.startsWith(href)) {
            document.querySelectorAll(".nav-link").forEach(x => x.classList.remove("is-active"));
            a.classList.add("is-active");
        }
    });
})();

document.addEventListener("DOMContentLoaded", () => {
    const cards = document.querySelectorAll(".plan-card");

    const setActive = (card) => {
        cards.forEach(c => c.classList.toggle("is-active", c === card));
    };

    cards.forEach(card => {
        card.addEventListener("click", () => setActive(card));
        card.addEventListener("keydown", (e) => {
            if (e.key === "Enter" || e.key === " ") {
                e.preventDefault();
                setActive(card);
            }
        });
    });
});


