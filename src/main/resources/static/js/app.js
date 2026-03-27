(function () {
    const storageKey = "contabpj_session";
    let session = null;

    const loginCard = document.getElementById("login-card");
    const contadorView = document.getElementById("contador-view");
    const clienteView = document.getElementById("cliente-view");
    const sessionInfo = document.getElementById("session-info");
    const sessionUser = document.getElementById("session-user");
    const logoutBtn = document.getElementById("btn-logout");

    const loginForm = document.getElementById("form-login");
    const loginFeedback = document.getElementById("login-feedback");

    const healthEl = document.getElementById("health-status");
    const empresaForm = document.getElementById("form-empresa");
    const empresaFeedback = document.getElementById("form-feedback");
    const listaEmpresas = document.getElementById("lista-empresas");
    const empresasEmpty = document.getElementById("empresas-empty");

    const templateForm = document.getElementById("form-template");
    const templateEmpresaEl = document.getElementById("template-empresa");
    const templateFeedback = document.getElementById("template-feedback");

    const gerarPendenciasForm = document.getElementById("form-gerar-pendencias");
    const pendenciasFeedback = document.getElementById("pendencias-feedback");
    const listaPendencias = document.getElementById("lista-pendencias");
    const pendenciasEmpty = document.getElementById("pendencias-empty");

    const formStatus = document.getElementById("form-status");
    const selectStatusPendencia = document.getElementById("status-pendencia");
    const statusNovo = document.getElementById("status-novo");
    const statusFeedback = document.getElementById("status-feedback");
    const btnRecarregarIa = document.getElementById("btn-recarregar-ia");
    const listaIaRevisao = document.getElementById("lista-ia-revisao");
    const iaEmpty = document.getElementById("ia-empty");
    const modalRejeicao = document.getElementById("modal-rejeicao");
    const formRejeicaoModal = document.getElementById("form-rejeicao-modal");
    const modalRejeicaoMotivo = document.getElementById("modal-rejeicao-motivo");
    const btnCancelarRejeicao = document.getElementById("btn-cancelar-rejeicao");
    const modalHistorico = document.getElementById("modal-historico");
    const modalHistoricoContent = document.getElementById("modal-historico-content");
    const btnFecharHistorico = document.getElementById("btn-fechar-historico");
    let processamentoRejeicaoAtual = null;

    const formFiltroCliente = document.getElementById("form-filtro-cliente");
    const listaPendenciasCliente = document.getElementById("lista-pendencias-cliente");
    const clienteEmpty = document.getElementById("cliente-empty");

    const uploadForm = document.getElementById("form-upload");
    const uploadPendenciaEl = document.getElementById("upload-pendencia");
    const uploadFeedback = document.getElementById("upload-feedback");

    function headers(extra = {}) {
        const h = { ...extra };
        if (session && session.token) {
            h.Authorization = `Bearer ${session.token}`;
        }
        return h;
    }

    async function fetchJson(url, options = {}) {
        const res = await fetch(url, options);
        const text = await res.text();
        let data = null;
        try {
            data = text ? JSON.parse(text) : null;
        } catch {
            throw new Error("Resposta inválida do servidor.");
        }
        if (!res.ok) {
            const msg = data && data.message ? data.message : res.statusText;
            throw new Error(msg || "Falha na requisição.");
        }
        return data;
    }

    function competenciaAtual() {
        const now = new Date();
        return { ano: now.getFullYear(), mes: now.getMonth() + 1 };
    }

    function salvarSessao(sess) {
        session = sess;
        localStorage.setItem(storageKey, JSON.stringify(sess));
    }

    function limparSessao() {
        session = null;
        localStorage.removeItem(storageKey);
    }

    function setFeedback(el, text, tipo) {
        el.textContent = text || "";
        el.className = "feedback";
        if (tipo) el.classList.add(tipo);
    }

    function setViewByPerfil() {
        if (!session) {
            loginCard.classList.remove("hidden");
            contadorView.classList.add("hidden");
            clienteView.classList.add("hidden");
            sessionInfo.classList.add("hidden");
            return;
        }
        loginCard.classList.add("hidden");
        sessionInfo.classList.remove("hidden");
        sessionUser.textContent = `${session.nome} (${session.perfil})`;
        contadorView.classList.toggle("hidden", session.perfil !== "CONTADOR");
        clienteView.classList.toggle("hidden", session.perfil !== "CLIENTE");
    }

    function renderEmpresas(empresas) {
        listaEmpresas.innerHTML = "";
        templateEmpresaEl.innerHTML = "";
        if (!empresas.length) {
            empresasEmpty.classList.remove("hidden");
            return;
        }
        empresasEmpty.classList.add("hidden");
        empresas.forEach((e) => {
            const li = document.createElement("li");
            li.textContent = `${e.cnpj} — ${e.razaoSocial}`;
            listaEmpresas.appendChild(li);
            const op = document.createElement("option");
            op.value = e.id;
            op.textContent = `${e.razaoSocial} (${e.cnpj})`;
            templateEmpresaEl.appendChild(op);
        });
    }

    function renderPendenciasContador(pendencias) {
        listaPendencias.innerHTML = "";
        selectStatusPendencia.innerHTML = "";
        if (!pendencias.length) {
            pendenciasEmpty.classList.remove("hidden");
            return;
        }
        pendenciasEmpty.classList.add("hidden");
        pendencias.forEach((p) => {
            const li = document.createElement("li");
            li.textContent = `#${p.id} ${p.empresaRazaoSocial} — ${p.templateDocumentoNome} — ${p.status} — vence ${p.vencimento}`;
            listaPendencias.appendChild(li);
            const op = document.createElement("option");
            op.value = p.id;
            op.textContent = `#${p.id} ${p.templateDocumentoNome} (${p.empresaRazaoSocial})`;
            selectStatusPendencia.appendChild(op);
        });
    }

    function renderPendenciasCliente(pendencias) {
        listaPendenciasCliente.innerHTML = "";
        uploadPendenciaEl.innerHTML = "";
        if (!pendencias.length) {
            clienteEmpty.classList.remove("hidden");
            return;
        }
        clienteEmpty.classList.add("hidden");
        pendencias.forEach((p) => {
            const li = document.createElement("li");
            const observacao = p.observacaoAnalise ? ` — observacao: ${p.observacaoAnalise}` : "";
            li.textContent = `#${p.id} ${p.templateDocumentoNome} — ${p.status} — vence ${p.vencimento}${observacao}`;
            listaPendenciasCliente.appendChild(li);
            const op = document.createElement("option");
            op.value = p.id;
            op.textContent = `#${p.id} ${p.templateDocumentoNome} (${p.status})`;
            uploadPendenciaEl.appendChild(op);
        });
    }

    function renderIaRevisao(itens) {
        listaIaRevisao.innerHTML = "";
        if (!itens.length) {
            iaEmpty.classList.remove("hidden");
            return;
        }
        iaEmpty.classList.add("hidden");
        itens.forEach((item) => {
            const li = document.createElement("li");
            const confianca = Number(item.confianca || 0).toFixed(2);
            const sevClass = (item.severidade || "MEDIA").toLowerCase();
            const motivo = item.observacaoProcessamento || "Sem detalhe.";
            const jsonPretty = formatJson(item.dadosExtraidosJson);
            li.innerHTML = `#${item.id} - ${item.nomeArquivoOriginal} - ${item.tipoDocumento} - conf. ${confianca} - ${item.status} <span class="severity severity--${sevClass}">${item.severidade}</span><br><span class="muted">${motivo}</span><pre class="ia-json">${jsonPretty}</pre>`;
            const btn = document.createElement("button");
            btn.type = "button";
            btn.className = "btn btn--ghost";
            btn.textContent = "Aprovar revisão";
            btn.style.marginLeft = "8px";
            btn.addEventListener("click", async () => {
                try {
                    await fetchJson(`/api/inteligencia/documentos/${item.id}/aprovar`, {
                        method: "POST",
                        headers: headers(),
                    });
                    await carregarFilaIa();
                } catch (e) {
                    setFeedback(statusFeedback, e.message || "Falha ao aprovar revisão IA.", "err");
                }
            });
            li.appendChild(btn);

            const rejectBtn = document.createElement("button");
            rejectBtn.type = "button";
            rejectBtn.className = "btn btn--ghost";
            rejectBtn.textContent = "Rejeitar (com motivo)";
            rejectBtn.style.marginLeft = "8px";
            rejectBtn.addEventListener("click", () => abrirModalRejeicao(item.id));
            li.appendChild(rejectBtn);

            const histBtn = document.createElement("button");
            histBtn.type = "button";
            histBtn.className = "btn btn--ghost";
            histBtn.textContent = "Ver histórico";
            histBtn.style.marginLeft = "8px";
            histBtn.addEventListener("click", async () => abrirModalHistorico(item.id));
            li.appendChild(histBtn);
            listaIaRevisao.appendChild(li);
        });
    }

    function abrirModalRejeicao(processamentoId) {
        processamentoRejeicaoAtual = processamentoId;
        formRejeicaoModal.reset();
        modalRejeicao.classList.remove("hidden");
        modalRejeicaoMotivo.focus();
    }

    function fecharModalRejeicao() {
        processamentoRejeicaoAtual = null;
        modalRejeicao.classList.add("hidden");
    }

    async function abrirModalHistorico(processamentoId) {
        modalHistoricoContent.innerHTML = "<span class='muted'>Carregando histórico...</span>";
        modalHistorico.classList.remove("hidden");
        try {
            const historico = await fetchJson(`/api/inteligencia/documentos/${processamentoId}/historico`, {
                headers: headers(),
            });
            if (!historico.length) {
                modalHistoricoContent.innerHTML = "<span class='muted'>Sem histórico ainda.</span>";
                return;
            }
            modalHistoricoContent.innerHTML = historico
                .map((h) => `<div class="timeline-item"><strong>${h.acao}</strong><br><span class="muted">${h.criadoEm} — ${h.usuarioNome}</span><br>${h.motivo || ""}</div>`)
                .join("");
        } catch (e) {
            modalHistoricoContent.innerHTML = `<span class='feedback err'>${e.message || "Falha ao carregar histórico."}</span>`;
        }
    }

    function fecharModalHistorico() {
        modalHistorico.classList.add("hidden");
    }

    function formatJson(jsonStr) {
        if (!jsonStr) return "{}";
        try {
            return JSON.stringify(JSON.parse(jsonStr), null, 2);
        } catch {
            return jsonStr;
        }
    }

    async function carregarHealth() {
        try {
            const data = await fetchJson("/api/health");
            healthEl.textContent = `API: ${data.status} — ${data.app}`;
        } catch {
            healthEl.textContent = "API indisponível.";
        }
    }

    async function carregarEmpresas() {
        const empresas = await fetchJson("/api/empresas", { headers: headers() });
        renderEmpresas(empresas);
    }

    async function carregarPendenciasContador() {
        const ano = Number(document.getElementById("pend-ano").value);
        const mes = Number(document.getElementById("pend-mes").value);
        const pendencias = await fetchJson(`/api/pendencias?ano=${ano}&mes=${mes}`, { headers: headers() });
        renderPendenciasContador(pendencias);
    }

    async function carregarFilaIa() {
        const itens = await fetchJson("/api/inteligencia/documentos?somenteRevisar=true", { headers: headers() });
        renderIaRevisao(itens);
    }

    async function carregarPendenciasCliente() {
        const ano = Number(document.getElementById("cliente-ano").value);
        const mes = Number(document.getElementById("cliente-mes").value);
        const pendencias = await fetchJson(`/api/pendencias?ano=${ano}&mes=${mes}`, { headers: headers() });
        renderPendenciasCliente(pendencias);
    }

    async function initAfterLogin() {
        setViewByPerfil();
        const comp = competenciaAtual();
        if (session.perfil === "CONTADOR") {
            document.getElementById("pend-ano").value = comp.ano;
            document.getElementById("pend-mes").value = comp.mes;
            await carregarHealth();
            await carregarEmpresas();
            await carregarPendenciasContador();
            await carregarFilaIa();
        } else {
            document.getElementById("cliente-ano").value = comp.ano;
            document.getElementById("cliente-mes").value = comp.mes;
            await carregarPendenciasCliente();
        }
    }

    loginForm.addEventListener("submit", async (ev) => {
        ev.preventDefault();
        setFeedback(loginFeedback, "", null);
        const email = document.getElementById("login-email").value.trim();
        const senha = document.getElementById("login-senha").value;
        try {
            const data = await fetchJson("/api/auth/login", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ email, senha }),
            });
            salvarSessao(data);
            await initAfterLogin();
        } catch (e) {
            setFeedback(loginFeedback, e.message || "Falha no login.", "err");
        }
    });

    logoutBtn.addEventListener("click", () => {
        limparSessao();
        setViewByPerfil();
    });

    empresaForm.addEventListener("submit", async (ev) => {
        ev.preventDefault();
        setFeedback(empresaFeedback, "", null);
        try {
            await fetchJson("/api/empresas", {
                method: "POST",
                headers: headers({ "Content-Type": "application/json" }),
                body: JSON.stringify({
                    cnpj: document.getElementById("cnpj").value.trim(),
                    razaoSocial: document.getElementById("razaoSocial").value.trim(),
                }),
            });
            empresaForm.reset();
            setFeedback(empresaFeedback, "Empresa cadastrada.", "ok");
            await carregarEmpresas();
        } catch (e) {
            setFeedback(empresaFeedback, e.message || "Erro ao cadastrar empresa.", "err");
        }
    });

    templateForm.addEventListener("submit", async (ev) => {
        ev.preventDefault();
        setFeedback(templateFeedback, "", null);
        try {
            await fetchJson("/api/templates-documentos", {
                method: "POST",
                headers: headers({ "Content-Type": "application/json" }),
                body: JSON.stringify({
                    empresaId: Number(templateEmpresaEl.value),
                    nome: document.getElementById("template-nome").value.trim(),
                    obrigatorio: document.getElementById("template-obrigatorio").checked,
                }),
            });
            templateForm.reset();
            document.getElementById("template-obrigatorio").checked = true;
            setFeedback(templateFeedback, "Template adicionado.", "ok");
        } catch (e) {
            setFeedback(templateFeedback, e.message || "Erro ao adicionar template.", "err");
        }
    });

    gerarPendenciasForm.addEventListener("submit", async (ev) => {
        ev.preventDefault();
        setFeedback(pendenciasFeedback, "", null);
        const ano = Number(document.getElementById("pend-ano").value);
        const mes = Number(document.getElementById("pend-mes").value);
        const diaVencimento = Number(document.getElementById("pend-dia").value);
        try {
            const result = await fetchJson("/api/pendencias/gerar", {
                method: "POST",
                headers: headers({ "Content-Type": "application/json" }),
                body: JSON.stringify({ ano, mes, diaVencimento }),
            });
            setFeedback(pendenciasFeedback, `${result.pendenciasCriadas} pendência(s) criada(s).`, "ok");
            await carregarPendenciasContador();
        } catch (e) {
            setFeedback(pendenciasFeedback, e.message || "Erro ao gerar pendências.", "err");
        }
    });

    formStatus.addEventListener("submit", async (ev) => {
        ev.preventDefault();
        setFeedback(statusFeedback, "", null);
        try {
            await fetchJson(`/api/pendencias/${selectStatusPendencia.value}/status`, {
                method: "POST",
                headers: headers({ "Content-Type": "application/json" }),
                body: JSON.stringify({ status: statusNovo.value }),
            });
            setFeedback(statusFeedback, "Status atualizado.", "ok");
            await carregarPendenciasContador();
        } catch (e) {
            setFeedback(statusFeedback, e.message || "Erro ao atualizar status.", "err");
        }
    });

    btnRecarregarIa.addEventListener("click", async () => {
        try {
            await carregarFilaIa();
        } catch (e) {
            setFeedback(statusFeedback, e.message || "Erro ao carregar fila IA.", "err");
        }
    });

    formRejeicaoModal.addEventListener("submit", async (ev) => {
        ev.preventDefault();
        if (!processamentoRejeicaoAtual) return;
        const motivoRejeicao = modalRejeicaoMotivo.value.trim();
        if (!motivoRejeicao) return;
        try {
            await fetchJson(`/api/inteligencia/documentos/${processamentoRejeicaoAtual}/rejeitar`, {
                method: "POST",
                headers: headers({ "Content-Type": "application/json" }),
                body: JSON.stringify({ motivo: motivoRejeicao }),
            });
            fecharModalRejeicao();
            await carregarFilaIa();
        } catch (e) {
            setFeedback(statusFeedback, e.message || "Falha ao rejeitar revisão IA.", "err");
        }
    });

    btnCancelarRejeicao.addEventListener("click", fecharModalRejeicao);
    btnFecharHistorico.addEventListener("click", fecharModalHistorico);
    modalRejeicao.addEventListener("click", (ev) => {
        if (ev.target === modalRejeicao) fecharModalRejeicao();
    });
    modalHistorico.addEventListener("click", (ev) => {
        if (ev.target === modalHistorico) fecharModalHistorico();
    });

    formFiltroCliente.addEventListener("submit", async (ev) => {
        ev.preventDefault();
        try {
            await carregarPendenciasCliente();
        } catch (e) {
            setFeedback(uploadFeedback, e.message || "Erro ao listar pendências.", "err");
        }
    });

    uploadForm.addEventListener("submit", async (ev) => {
        ev.preventDefault();
        setFeedback(uploadFeedback, "", null);

        const arquivo = document.getElementById("upload-arquivo").files[0];
        if (!arquivo) {
            setFeedback(uploadFeedback, "Selecione um arquivo.", "err");
            return;
        }

        const fd = new FormData();
        fd.append("arquivo", arquivo);
        const obs = document.getElementById("upload-observacao").value.trim();
        if (obs) fd.append("observacao", obs);

        try {
            const res = await fetch(`/api/pendencias/${uploadPendenciaEl.value}/entregas`, {
                method: "POST",
                headers: headers(),
                body: fd,
            });
            const text = await res.text();
            let data = null;
            try { data = text ? JSON.parse(text) : null; } catch {}
            if (!res.ok) throw new Error((data && data.message) || "Erro no upload.");

            uploadForm.reset();
            setFeedback(uploadFeedback, "Documento enviado e pendência marcada como ENVIADO.", "ok");
            await carregarPendenciasCliente();
        } catch (e) {
            setFeedback(uploadFeedback, e.message || "Erro no upload.", "err");
        }
    });

    try {
        const raw = localStorage.getItem(storageKey);
        if (raw) {
            session = JSON.parse(raw);
            initAfterLogin().catch(() => {
                limparSessao();
                setViewByPerfil();
            });
        } else {
            setViewByPerfil();
        }
    } catch {
        limparSessao();
        setViewByPerfil();
    }
})();
