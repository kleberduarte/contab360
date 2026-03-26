(function () {
    const healthEl = document.getElementById("health-status");
    const listaEl = document.getElementById("lista-empresas");
    const emptyEl = document.getElementById("empresas-empty");
    const form = document.getElementById("form-empresa");
    const feedback = document.getElementById("form-feedback");

    async function fetchJson(url, options) {
        const res = await fetch(url, options);
        const text = await res.text();
        let data = null;
        try {
            data = text ? JSON.parse(text) : null;
        } catch {
            throw new Error("Resposta inválida do servidor.");
        }
        if (!res.ok) {
            const msg =
                data && typeof data === "object" && data.message
                    ? data.message
                    : res.statusText;
            throw new Error(msg || "Erro na requisição.");
        }
        return data;
    }

    async function carregarHealth() {
        try {
            const data = await fetchJson("/api/health");
            healthEl.textContent = `API: ${data.status} — ${data.app}`;
            healthEl.classList.remove("muted");
        } catch (e) {
            healthEl.textContent = "Não foi possível conectar à API.";
            healthEl.classList.add("err");
        }
    }

    function renderEmpresas(empresas) {
        listaEl.innerHTML = "";
        if (!empresas.length) {
            emptyEl.classList.remove("hidden");
            return;
        }
        emptyEl.classList.add("hidden");
        empresas.forEach((e) => {
            const li = document.createElement("li");
            li.textContent = `${e.cnpj} — ${e.razaoSocial}`;
            listaEl.appendChild(li);
        });
    }

    async function carregarEmpresas() {
        try {
            const empresas = await fetchJson("/api/empresas");
            renderEmpresas(empresas);
        } catch {
            listaEl.innerHTML = "";
            emptyEl.textContent = "Erro ao listar empresas.";
            emptyEl.classList.remove("hidden");
        }
    }

    form.addEventListener("submit", async (ev) => {
        ev.preventDefault();
        feedback.textContent = "";
        feedback.className = "feedback";

        const cnpj = document.getElementById("cnpj").value.trim();
        const razaoSocial = document.getElementById("razaoSocial").value.trim();

        try {
            await fetchJson("/api/empresas", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ cnpj, razaoSocial }),
            });
            feedback.textContent = "Empresa cadastrada.";
            feedback.classList.add("ok");
            form.reset();
            await carregarEmpresas();
        } catch (e) {
            feedback.textContent = e.message || "Falha ao cadastrar.";
            feedback.classList.add("err");
        }
    });

    carregarHealth();
    carregarEmpresas();
})();
