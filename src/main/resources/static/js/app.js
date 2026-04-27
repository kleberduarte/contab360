(function () {
    const storageKey = "contabpj_session";
    let session = null;
    let features = { certificadoDigital: false };

    const loginCard = document.getElementById("login-card");
    const appHeader = document.getElementById("app-header");
    const appMain = document.getElementById("app-main");
    const appShell = document.getElementById("app-shell");
    const sessionInfo = document.getElementById("session-info");
    const userMenuWrap = document.querySelector("[data-user-menu]");
    const btnUserMenu = document.getElementById("btn-user-menu");
    const userMenuDropdown = document.getElementById("header-user-dropdown");
    const sessionAvatarInitials = document.getElementById("session-avatar-initials");
    const sessionAvatarInitialsCard = document.getElementById("session-avatar-initials-card");
    const sessionDropdownName = document.getElementById("session-dropdown-name");
    const sessionDropdownMeta = document.getElementById("session-dropdown-meta");
    const btnMinhaConta = document.getElementById("btn-minha-conta");
    const logoutBtn = document.getElementById("btn-logout");
    const sidebarItems = Array.from(document.querySelectorAll(".sidebar__item"));
    const appScreens = Array.from(document.querySelectorAll(".app-screen"));

    const loginFeedback = document.getElementById("login-feedback");

    const empresaForm = document.getElementById("form-empresa");
    const empresaFeedback = document.getElementById("form-feedback");
    const listaEmpresas = document.getElementById("lista-empresas");
    const empresasEmpty = document.getElementById("empresas-empty");
    const empresasTableWrap = document.getElementById("empresas-table-wrap");
    const empresasCount = document.getElementById("empresas-count");
    const empresasBuscaInput = document.getElementById("empresas-busca");
    const empresasListaHint = document.getElementById("empresas-lista-hint");
    const empresasBuscaVazio = document.getElementById("empresas-busca-vazio");
    const btnEmpresaSubmit = document.getElementById("btn-empresa-submit");
    const btnEmpresaCancelar = document.getElementById("btn-empresa-cancelar-edicao");
    const btnEmpresaExcluir = document.getElementById("btn-empresa-excluir");
    const empresasFormPanelTitle = document.getElementById("empresas-form-panel-title");
    const empresasIncluirInativasEl = document.getElementById("empresas-incluir-inativas");
    let empresasCache = [];
    let empresaEditandoId = null;

    const templateForm = document.getElementById("form-template");
    const templateEmpresaEl = document.getElementById("template-empresa");
    const templateFeedback = document.getElementById("template-feedback");

    const gerarPendenciasForm = document.getElementById("form-gerar-pendencias");
    const pendenciasFeedback = document.getElementById("pendencias-feedback");
    const listaPendencias = document.getElementById("lista-pendencias");
    const pendenciasEmpty = document.getElementById("pendencias-empty");

    const iaFeedback = document.getElementById("ia-feedback");
    const btnRecarregarIa = document.getElementById("btn-recarregar-ia");
    const listaIaRevisao = document.getElementById("lista-ia-revisao");
    const iaEmpty = document.getElementById("ia-empty");
    const modalHistorico = document.getElementById("modal-historico");
    const modalHistoricoContent = document.getElementById("modal-historico-content");
    const btnFecharHistorico = document.getElementById("btn-fechar-historico");
    const modalDadosExtraidos = document.getElementById("modal-dados-extraidos");
    const modalDadosExtraidosMeta = document.getElementById("modal-dados-extraidos-meta");
    const modalDadosExtraidosContent = document.getElementById("modal-dados-extraidos-content");
    const modalDadosToolbar = document.getElementById("modal-dados-toolbar");
    const modalDadosFeedback = document.getElementById("modal-dados-extraidos-feedback");
    const btnExportDadosCsv = document.getElementById("btn-export-dados-csv");
    const btnExportDadosPdf = document.getElementById("btn-export-dados-pdf");
    const btnSalvarDadosExtraidos = document.getElementById("btn-salvar-dados-extraidos");
    let modalDadosContext = { pendenciaId: null, processamentoId: null };
    const btnFecharDadosExtraidos = document.getElementById("btn-fechar-dados-extraidos");

    const formFiltroCliente = document.getElementById("form-filtro-cliente");
    const listaPendenciasCliente = document.getElementById("lista-pendencias-cliente");
    const clienteEmpty = document.getElementById("cliente-empty");

    const uploadForm = document.getElementById("form-upload");
    const uploadPendenciaEl = document.getElementById("upload-pendencia");
    const uploadFeedback = document.getElementById("upload-feedback");
    const docsValidadosFeedback = document.getElementById("docs-validados-feedback");
    const docsValidadosTabs = document.getElementById("docs-validados-tabs");
    const docsValidadosPanels = document.getElementById("docs-validados-panels");
    const btnAtualizarDocsValidados = document.getElementById("btn-atualizar-docs-validados");
    const docsValidadosEmpresaWrap = document.getElementById("docs-validados-empresa-wrap");
    const docsValidadosEmpresaEl = document.getElementById("docs-validados-empresa");
    const docsValidadosClienteEmpresa = document.getElementById("docs-validados-cliente-empresa");
    const docsValidadosPager = document.getElementById("docs-validados-tabs-pager");
    const docsValidadosPagerPrev = document.getElementById("docs-validados-pager-prev");
    const docsValidadosPagerNext = document.getElementById("docs-validados-pager-next");
    const docsValidadosPagerCurrent = document.getElementById("docs-validados-pager-current");
    const docsValidadosPagerTotal = document.getElementById("docs-validados-pager-total");
    const cnpjInput = document.getElementById("cnpj");
    const cpfResponsavelInput = document.getElementById("cpfResponsavel");

    const fiscalNotaForm = document.getElementById("form-fiscal-nota");
    const notaFeedback = document.getElementById("nota-feedback");
    const notaPosEmitir = document.getElementById("nota-pos-emitir");
    const screenFiscalNotas = document.getElementById("screen-fiscal-notas");
    const btnRadarNotas = document.getElementById("btn-radar-notas");
    const radarFeedback = document.getElementById("radar-feedback");
    const radarJson = document.getElementById("radar-json");

    const fiscalCadastroForm = document.getElementById("form-fiscal-cadastro");
    const cadastroFeedback = document.getElementById("cadastro-feedback");

    const fiscalCobrancaForm = document.getElementById("form-fiscal-cobranca");
    const cobrancaFeedback = document.getElementById("cobranca-feedback");

    const fiscalCertificadoForm = document.getElementById("form-fiscal-certificado");
    const certFeedback = document.getElementById("cert-feedback");

    const btnGerarAlertas = document.getElementById("btn-gerar-alertas");
    const btnRelatorioEstrategico = document.getElementById("btn-relatorio-estrategico");
    const btnPrefeituras = document.getElementById("btn-prefeituras");
    const alertaFeedback = document.getElementById("alerta-feedback");
    const relatorioJson = document.getElementById("relatorio-json");

    const iaObsFeedback = document.getElementById("ia-obs-feedback");
    const iaObsResumo = document.getElementById("ia-obs-resumo");
    const iaObsSugestoes = document.getElementById("ia-obs-sugestoes");
    const iaObsEventos = document.getElementById("ia-obs-eventos");
    const btnAtualizarIaObs = document.getElementById("btn-atualizar-ia-obs");

    const modalAppDialog = document.getElementById("modal-app-dialog");
    const modalAppDialogTitle = document.getElementById("modal-app-dialog-title");
    const modalAppDialogMessage = document.getElementById("modal-app-dialog-message");
    const modalAppDialogActionsAlert = document.getElementById("modal-app-dialog-actions-alert");
    const modalAppDialogActionsConfirm = document.getElementById("modal-app-dialog-actions-confirm");
    const btnAppDialogAlertOk = document.getElementById("btn-app-dialog-alert-ok");
    const btnAppDialogCancel = document.getElementById("btn-app-dialog-cancel");
    const btnAppDialogConfirm = document.getElementById("btn-app-dialog-confirm");

    let appDialogResolve = null;
    let appDialogMode = null;
    let appDialogOnKey = null;
    let appDialogPreviousFocus = null;

    function somenteDigitos(value) {
        return (value || "").replace(/\D/g, "");
    }

    function formatarCnpj(value) {
        const digits = somenteDigitos(value).slice(0, 14);
        if (digits.length <= 2) return digits;
        if (digits.length <= 5) return `${digits.slice(0, 2)}.${digits.slice(2)}`;
        if (digits.length <= 8) return `${digits.slice(0, 2)}.${digits.slice(2, 5)}.${digits.slice(5)}`;
        if (digits.length <= 12) return `${digits.slice(0, 2)}.${digits.slice(2, 5)}.${digits.slice(5, 8)}/${digits.slice(8)}`;
        return `${digits.slice(0, 2)}.${digits.slice(2, 5)}.${digits.slice(5, 8)}/${digits.slice(8, 12)}-${digits.slice(12)}`;
    }

    /** Lista/tabela: oculta filial e dígitos verificadores (tooltip com CNPJ completo). */
    function mascararCnpjLista(cnpj) {
        const d = somenteDigitos(cnpj).slice(0, 14);
        if (d.length !== 14) return "—";
        return `${d.slice(0, 2)}.${d.slice(2, 5)}.${d.slice(5, 8)}/****-**`;
    }

    function formatarCpf(value) {
        const digits = somenteDigitos(value).slice(0, 11);
        if (digits.length <= 3) return digits;
        if (digits.length <= 6) return `${digits.slice(0, 3)}.${digits.slice(3)}`;
        if (digits.length <= 9) return `${digits.slice(0, 3)}.${digits.slice(3, 6)}.${digits.slice(6)}`;
        return `${digits.slice(0, 3)}.${digits.slice(3, 6)}.${digits.slice(6, 9)}-${digits.slice(9)}`;
    }

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

    function closeAppDialog(value) {
        if (!modalAppDialog) return;
        const mode = appDialogMode;
        const fn = appDialogResolve;
        appDialogResolve = null;
        appDialogMode = null;
        if (appDialogOnKey) {
            document.removeEventListener("keydown", appDialogOnKey);
            appDialogOnKey = null;
        }
        modalAppDialog.classList.add("hidden");
        modalAppDialog.setAttribute("aria-hidden", "true");
        document.body.classList.remove("modal-app-dialog-open");
        const prev = appDialogPreviousFocus;
        appDialogPreviousFocus = null;
        if (prev && typeof prev.focus === "function") {
            try {
                prev.focus();
            } catch (_) {
                /* ignore */
            }
        }
        if (typeof fn !== "function") return;
        if (mode === "alert") fn();
        else fn(!!value);
    }

    function openAppDialogSetup(mode, title, message, opts) {
        const o = opts || {};
        const confirmLabel = o.confirmLabel != null ? o.confirmLabel : "OK";
        const cancelLabel = o.cancelLabel != null ? o.cancelLabel : "Cancelar";
        const danger = !!o.danger;
        if (modalAppDialogTitle) modalAppDialogTitle.textContent = title || (mode === "alert" ? "Aviso" : "Confirmação");
        if (modalAppDialogMessage) modalAppDialogMessage.textContent = message || "";
        if (mode === "alert") {
            if (modalAppDialogActionsAlert) modalAppDialogActionsAlert.classList.remove("hidden");
            if (modalAppDialogActionsConfirm) modalAppDialogActionsConfirm.classList.add("hidden");
        } else {
            if (modalAppDialogActionsAlert) modalAppDialogActionsAlert.classList.add("hidden");
            if (modalAppDialogActionsConfirm) modalAppDialogActionsConfirm.classList.remove("hidden");
            if (btnAppDialogConfirm) {
                btnAppDialogConfirm.textContent = confirmLabel;
                btnAppDialogConfirm.classList.toggle("btn--danger", danger);
            }
            if (btnAppDialogCancel) btnAppDialogCancel.textContent = cancelLabel;
        }
        appDialogMode = mode;
        appDialogPreviousFocus = document.activeElement;
        modalAppDialog.classList.remove("hidden");
        modalAppDialog.removeAttribute("aria-hidden");
        document.body.classList.add("modal-app-dialog-open");
        appDialogOnKey = (e) => {
            if (e.key !== "Escape") return;
            e.preventDefault();
            if (mode === "alert") closeAppDialog();
            else closeAppDialog(false);
        };
        document.addEventListener("keydown", appDialogOnKey);
        requestAnimationFrame(() => {
            if (mode === "alert" && btnAppDialogAlertOk) btnAppDialogAlertOk.focus();
            else if (mode === "confirm") {
                const focusEl = danger && btnAppDialogCancel ? btnAppDialogCancel : btnAppDialogConfirm;
                if (focusEl) focusEl.focus();
            }
        });
    }

    /**
     * Diálogo de aviso (substitui window.alert).
     * @param {string} message
     * @param {string} [title]
     * @returns {Promise<void>}
     */
    function appAlert(message, title) {
        return new Promise((resolve) => {
            if (!modalAppDialog || !modalAppDialogTitle) {
                window.alert(message);
                resolve();
                return;
            }
            appDialogResolve = resolve;
            openAppDialogSetup("alert", title || "Aviso", message, {});
        });
    }

    /**
     * Diálogo de confirmação (substitui window.confirm).
     * @param {string} message
     * @param {string} [title]
     * @param {{ confirmLabel?: string, cancelLabel?: string, danger?: boolean }} [opts]
     * @returns {Promise<boolean>}
     */
    function appConfirm(message, title, opts) {
        return new Promise((resolve) => {
            if (!modalAppDialog || !modalAppDialogTitle) {
                resolve(window.confirm(message));
                return;
            }
            appDialogResolve = resolve;
            openAppDialogSetup("confirm", title || "Confirmação", message, opts || {});
        });
    }

    if (modalAppDialog && btnAppDialogAlertOk && btnAppDialogCancel && btnAppDialogConfirm) {
        btnAppDialogAlertOk.addEventListener("click", () => closeAppDialog());
        btnAppDialogCancel.addEventListener("click", () => closeAppDialog(false));
        btnAppDialogConfirm.addEventListener("click", () => closeAppDialog(true));
        modalAppDialog.addEventListener("click", (e) => {
            if (e.target !== modalAppDialog) return;
            if (appDialogMode === "alert") closeAppDialog();
            else closeAppDialog(false);
        });
    }

    function iniciaisDoNome(nome) {
        const s = (nome || "").trim();
        if (!s) return "?";
        const parts = s.split(/\s+/).filter(Boolean);
        if (parts.length >= 2) {
            return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
        }
        return s.slice(0, 2).toUpperCase();
    }

    function closeUserMenu() {
        if (!userMenuDropdown || !btnUserMenu) return;
        userMenuDropdown.classList.add("hidden");
        btnUserMenu.setAttribute("aria-expanded", "false");
    }

    function toggleUserMenu() {
        if (!userMenuDropdown || !btnUserMenu) return;
        const willOpen = userMenuDropdown.classList.contains("hidden");
        userMenuDropdown.classList.toggle("hidden");
        btnUserMenu.setAttribute("aria-expanded", willOpen ? "true" : "false");
    }

    function atualizarPainelUsuario() {
        if (!session || !sessionAvatarInitials || !sessionAvatarInitialsCard) return;
        const ini = iniciaisDoNome(session.nome);
        sessionAvatarInitials.textContent = ini;
        sessionAvatarInitialsCard.textContent = ini;
        if (sessionDropdownName) sessionDropdownName.textContent = session.nome || "—";
        if (sessionDropdownMeta) {
            const email = session.email || "";
            sessionDropdownMeta.textContent = email
                ? `${session.perfil || ""}${session.perfil ? " · " : ""}${email}`
                : (session.perfil || "—");
        }
    }

    function setViewByPerfil() {
        if (!session) {
            loginCard.classList.remove("hidden");
            appShell.classList.add("hidden");
            sessionInfo.classList.add("hidden");
            if (appHeader) appHeader.classList.add("hidden");
            if (appMain) appMain.classList.add("main--login-only");
            closeUserMenu();
            return;
        }
        loginCard.classList.add("hidden");
        appShell.classList.remove("hidden");
        sessionInfo.classList.remove("hidden");
        if (appHeader) appHeader.classList.remove("hidden");
        if (appMain) appMain.classList.remove("main--login-only");
        atualizarPainelUsuario();
        sidebarItems.forEach((item) => {
            const allowed = item.dataset.perfil === session.perfil;
            item.classList.toggle("hidden", !allowed);
        });
        applyFeatureFlags();
        const initialScreen = session.perfil === "CONTADOR" ? "screen-contador-dashboard" : "screen-cliente-dashboard";
        navigateToScreen(initialScreen);
        refreshDocumentosValidadosIfNeeded(initialScreen);
        refreshDashboardIfNeeded(initialScreen);
        refreshPendenciasContadorIfNeeded(initialScreen);
        refreshPendenciasClienteIfNeeded(initialScreen);
    }

    function applyFeatureFlags() {
        const certOn = features.certificadoDigital === true;
        const offEl = document.getElementById("cert-modulo-off");
        const onEl = document.getElementById("cert-modulo-on");
        if (offEl) {
            offEl.classList.toggle("hidden", certOn);
        }
        if (onEl) {
            onEl.classList.toggle("hidden", !certOn);
        }
        if (!session) {
            return;
        }
        document.querySelectorAll(".sidebar__item[data-feature]").forEach((el) => {
            const allowed = el.dataset.perfil === session.perfil;
            let featOk = true;
            if (el.dataset.feature === "certificadoDigital") {
                featOk = features.certificadoDigital === true;
            }
            el.classList.toggle("hidden", !allowed || !featOk);
        });
    }

    async function loadFeatures() {
        try {
            const r = await fetch("/api/features");
            if (r.ok) {
                const j = await r.json();
                features = { certificadoDigital: false, ...j };
            }
        } catch (_) {
            /* ignore */
        }
        applyFeatureFlags();
    }

    function navigateToScreen(screenId) {
        if (screenId === "screen-fiscal-certificados" && features.certificadoDigital !== true) {
            screenId = "screen-contador-dashboard";
        }
        appScreens.forEach((screen) => {
            screen.classList.toggle("hidden", screen.id !== screenId);
        });
        sidebarItems.forEach((item) => {
            item.classList.toggle("active", item.dataset.target === screenId);
        });
        if (screenId === "screen-fiscal-certificados" && features.certificadoDigital === true) {
            popularCertEmpresaSelect();
            void carregarListaCertificados();
        }
    }

    function empresaEstaAtiva(e) {
        return e == null || e.ativo !== false;
    }

    function renderEmpresas(empresas) {
        empresasCache = Array.isArray(empresas) ? empresas.slice() : [];
        const ativas = empresasCache.filter(empresaEstaAtiva);
        templateEmpresaEl.innerHTML = "";
        ativas.forEach((e) => {
            const op = document.createElement("option");
            op.value = e.id;
            op.textContent = `${e.razaoSocial} (${formatarCnpj(e.cnpj)})`;
            templateEmpresaEl.appendChild(op);
        });
        popularSelectDocsValidadosEmpresa(ativas);
        popularCertEmpresaSelect();
        renderEmpresasTabelaUI();
    }

    function popularCertEmpresaSelect() {
        const sel = document.getElementById("cert-empresa-id");
        if (!sel) {
            return;
        }
        const prev = sel.value;
        sel.innerHTML = '<option value="">— Nenhuma —</option>';
        empresasCache.filter(empresaEstaAtiva).forEach((e) => {
            const op = document.createElement("option");
            op.value = e.id;
            op.textContent = `${e.razaoSocial} (${formatarCnpj(e.cnpj)})`;
            sel.appendChild(op);
        });
        if (prev && [...sel.options].some((o) => o.value === String(prev))) {
            sel.value = prev;
        }
    }

    function empresasLinhasVisiveis() {
        const q = (empresasBuscaInput && empresasBuscaInput.value ? empresasBuscaInput.value : "").trim();
        const qLower = q.toLowerCase();
        const qDigits = somenteDigitos(q);
        if (!empresasCache.length) return [];
        if (!q) {
            const sorted = [...empresasCache].sort((a, b) => (Number(b.id) || 0) - (Number(a.id) || 0));
            return sorted.slice(0, 1);
        }
        return empresasCache.filter((e) => {
            const nome = (e.razaoSocial || "").toLowerCase();
            const cnpj = somenteDigitos(e.cnpj || "");
            if (nome.includes(qLower)) return true;
            if (qDigits.length >= 1 && cnpj.includes(qDigits)) return true;
            return false;
        });
    }

    function renderEmpresasTabelaUI() {
        listaEmpresas.innerHTML = "";
        if (!empresasCache.length) {
            empresasEmpty.classList.remove("hidden");
            if (empresasTableWrap) empresasTableWrap.classList.add("hidden");
            if (empresasBuscaVazio) empresasBuscaVazio.classList.add("hidden");
            if (empresasCount) {
                empresasCount.classList.add("hidden");
                empresasCount.textContent = "0";
            }
            if (empresasListaHint) empresasListaHint.textContent = "";
            return;
        }
        empresasEmpty.classList.add("hidden");
        if (empresasCount) {
            empresasCount.textContent = String(empresasCache.length);
            empresasCount.classList.remove("hidden");
        }
        const linhas = empresasLinhasVisiveis();
        const temBusca = (empresasBuscaInput && empresasBuscaInput.value ? empresasBuscaInput.value : "").trim().length > 0;
        if (empresasListaHint) {
            if (!temBusca) {
                const incluirInativas = empresasIncluirInativasEl && empresasIncluirInativasEl.checked;
                let base =
                    empresasCache.length > 1
                        ? "Mostrando apenas a última empresa cadastrada. Use a busca para localizar as demais."
                        : "Única empresa cadastrada.";
                if (incluirInativas) {
                    base = "Listagem inclui empresas inativas. " + base;
                }
                empresasListaHint.textContent = base;
            } else {
                empresasListaHint.textContent =
                    linhas.length === 1 ? "1 resultado encontrado." : `${linhas.length} resultados encontrados.`;
            }
        }
        if (!linhas.length) {
            if (empresasTableWrap) empresasTableWrap.classList.add("hidden");
            if (empresasBuscaVazio) empresasBuscaVazio.classList.toggle("hidden", !temBusca);
            return;
        }
        if (empresasBuscaVazio) empresasBuscaVazio.classList.add("hidden");
        if (empresasTableWrap) empresasTableWrap.classList.remove("hidden");
        linhas.forEach((e) => {
            const tr = document.createElement("tr");
            const cnpjFmt = formatarCnpj(e.cnpj);
            const meiCell = e.mei
                ? '<span class="empresas-badge empresas-badge--mei">Sim</span>'
                : '<span class="empresas-badge empresas-badge--nao">Não</span>';
            const idAttr = String(e.id);
            const ativa = empresaEstaAtiva(e);
            const nomeHtml = escapeHtml(e.razaoSocial)
                + (ativa ? "" : ' <span class="empresas-badge empresas-badge--inativa">Inativa</span>');
            const acoesHtml = ativa
                ? `<button type="button" class="empresas-acao empresas-acao--edit" data-empresa-editar="${idAttr}">Editar</button>`
                    + `<button type="button" class="empresas-acao empresas-acao--del" data-empresa-excluir="${idAttr}">Desativar</button>`
                : `<button type="button" class="empresas-acao empresas-acao--reativar" data-empresa-reativar="${idAttr}">Reativar</button>`;
            tr.innerHTML =
                `<td class="empresas-table__nome">${nomeHtml}</td>`
                + `<td class="empresas-table__cnpj" title="CNPJ completo: ${escapeHtml(cnpjFmt)}">${escapeHtml(mascararCnpjLista(e.cnpj))}</td>`
                + `<td class="empresas-table__mei">${meiCell}</td>`
                + `<td class="empresas-table__acoes">${acoesHtml}</td>`;
            listaEmpresas.appendChild(tr);
        });
    }

    function preencherFormularioEmpresa(e) {
        document.getElementById("cnpj").value = formatarCnpj(e.cnpj);
        document.getElementById("razaoSocial").value = e.razaoSocial || "";
        document.getElementById("cpfResponsavel").value = e.cpfResponsavel ? formatarCpf(e.cpfResponsavel) : "";
        document.getElementById("mei").checked = !!e.mei;
        const vd = document.getElementById("vencimentoDas");
        const vc = document.getElementById("vencimentoCertificadoMei");
        if (vd) vd.value = e.vencimentoDas || "";
        if (vc) vc.value = e.vencimentoCertificadoMei || "";
    }

    function limparEdicaoEmpresa() {
        empresaEditandoId = null;
        empresaForm.reset();
        if (btnEmpresaSubmit) btnEmpresaSubmit.textContent = "Cadastrar empresa";
        if (btnEmpresaCancelar) btnEmpresaCancelar.classList.add("hidden");
        if (btnEmpresaExcluir) btnEmpresaExcluir.classList.add("hidden");
        if (empresasFormPanelTitle) empresasFormPanelTitle.textContent = "Novo cadastro";
    }

    function iniciarEdicaoEmpresaNoFormulario(e) {
        if (!e || e.id == null) return;
        if (!empresaEstaAtiva(e)) {
            setFeedback(
                empresaFeedback,
                "Esta empresa está inativa. Marque “Mostrar empresas inativas”, use Reativar na tabela e depois edite.",
                "err"
            );
            return;
        }
        empresaEditandoId = Number(e.id);
        preencherFormularioEmpresa(e);
        if (btnEmpresaSubmit) btnEmpresaSubmit.textContent = "Salvar alterações";
        if (btnEmpresaCancelar) btnEmpresaCancelar.classList.remove("hidden");
        if (btnEmpresaExcluir) btnEmpresaExcluir.classList.remove("hidden");
        if (empresasFormPanelTitle) empresasFormPanelTitle.textContent = "Editar empresa";
        setFeedback(empresaFeedback, "", null);
        const cnpjEl = document.getElementById("cnpj");
        if (cnpjEl) cnpjEl.focus();
    }

    function aplicarBuscaEmpresaAoFormulario() {
        const linhas = empresasLinhasVisiveis();
        if (!linhas.length) {
            setFeedback(empresaFeedback, "Nenhuma empresa encontrada para este termo.", "err");
            return;
        }
        iniciarEdicaoEmpresaNoFormulario(linhas[0]);
        setFeedback(
            empresaFeedback,
            linhas.length > 1
                ? "Primeiro resultado da busca carregado para edição (vários encontrados)."
                : "Empresa carregada no formulário para edição.",
            "ok"
        );
    }

    async function excluirEmpresaPorId(id) {
        if (!Number.isFinite(id) || id < 1) return;
        const ok = await appConfirm(
            "A empresa será desativada: some das listas e de novos cadastros; pendências, templates e usuários permanecem no histórico. Você pode reativar depois.",
            "Desativar empresa",
            {
                confirmLabel: "Desativar",
                cancelLabel: "Cancelar",
                danger: true,
            }
        );
        if (!ok) return;
        setFeedback(empresaFeedback, "", null);
        try {
            await fetchJson(`/api/empresas/${id}`, { method: "DELETE", headers: headers() });
            if (empresaEditandoId === id) limparEdicaoEmpresa();
            setFeedback(empresaFeedback, "Empresa desativada.", "ok");
            await carregarEmpresas();
        } catch (err) {
            setFeedback(empresaFeedback, err.message || "Não foi possível desativar.", "err");
        }
    }

    async function reativarEmpresaPorId(id) {
        if (!Number.isFinite(id) || id < 1) return;
        setFeedback(empresaFeedback, "", null);
        try {
            await fetchJson(`/api/empresas/${id}/reativar`, { method: "POST", headers: headers() });
            setFeedback(empresaFeedback, "Empresa reativada.", "ok");
            await carregarEmpresas();
        } catch (err) {
            setFeedback(empresaFeedback, err.message || "Não foi possível reativar.", "err");
        }
    }

    function popularSelectDocsValidadosEmpresa(empresas) {
        if (!docsValidadosEmpresaEl) return;
        const prev = docsValidadosEmpresaEl.value;
        docsValidadosEmpresaEl.innerHTML = "";
        empresas.forEach((e) => {
            const op = document.createElement("option");
            op.value = e.id;
            op.textContent = `${formatarCnpj(e.cnpj)} — ${e.razaoSocial}`;
            docsValidadosEmpresaEl.appendChild(op);
        });
        if (prev && [...docsValidadosEmpresaEl.options].some((o) => o.value === String(prev))) {
            docsValidadosEmpresaEl.value = prev;
        }
    }

    function pendenciaStatusContadorBadge(status) {
        const map = {
            VALIDADO: { cls: "status-badge status-badge--ok", label: "Validado" },
            ENVIADO: { cls: "status-badge status-badge--analise", label: "Enviado" },
            REJEITADO: { cls: "status-badge status-badge--rejeitado", label: "Rejeitado" },
            PENDENTE: { cls: "status-badge status-badge--pendente", label: "Pendente" },
        };
        return map[status] || { cls: "status-badge", label: status || "—" };
    }

    function renderPendenciasContador(pendencias, arquivoInfo, incluirArquivadas) {
        const banner = document.getElementById("pendencias-arquivo-banner");
        if (banner) {
            if (arquivoInfo && arquivoInfo.arquivada && incluirArquivadas && pendencias.length) {
                banner.classList.remove("hidden");
                const quando = arquivoInfo.arquivadaEm ? ` em ${arquivoInfo.arquivadaEm}` : "";
                banner.textContent = `Competência arquivada${quando} — todos os itens estão validados; dados permanecem armazenados.`;
            } else {
                banner.classList.add("hidden");
                banner.textContent = "";
            }
        }
        listaPendencias.innerHTML = "";
        if (!pendencias.length) {
            pendenciasEmpty.classList.remove("hidden");
            if (
                arquivoInfo
                && arquivoInfo.arquivada
                && !incluirArquivadas
                && arquivoInfo.existeCompetencia
            ) {
                pendenciasEmpty.textContent =
                    "Esta competência está arquivada: todas as pendências foram validadas e os registros foram guardados no histórico. Marque \"Incluir competências arquivadas\" para ver a lista completa.";
            } else {
                pendenciasEmpty.textContent = "Nenhuma pendência para a competência.";
            }
            return;
        }
        pendenciasEmpty.classList.add("hidden");
        pendenciasEmpty.textContent = "Nenhuma pendência para a competência.";
        pendencias.forEach((p) => {
            const card = document.createElement("article");
            card.className = "pendencia-card";
            card.setAttribute("role", "listitem");

            const row = document.createElement("div");
            row.className = "pendencia-card__row";
            const idEl = document.createElement("span");
            idEl.className = "pendencia-card__id";
            idEl.textContent = `#${p.id}`;
            const emp = document.createElement("h3");
            emp.className = "pendencia-card__empresa";
            emp.textContent = p.empresaRazaoSocial || p.clientePessoaFisicaNome || "—";
            const badgeWrap = document.createElement("span");
            badgeWrap.className = "pendencia-card__badge-wrap";
            const st = pendenciaStatusContadorBadge(p.status);
            const badge = document.createElement("span");
            badge.className = st.cls;
            badge.textContent = st.label;
            badgeWrap.appendChild(badge);
            row.appendChild(idEl);
            row.appendChild(emp);
            row.appendChild(badgeWrap);
            card.appendChild(row);

            const meta = document.createElement("div");
            meta.className = "pendencia-card__meta";
            const docP = document.createElement("p");
            docP.className = "pendencia-card__meta-item";
            docP.innerHTML = `<span class="pendencia-card__meta-lbl">Documento</span><span class="pendencia-card__meta-val">${escapeHtml(p.templateDocumentoNome || "—")}</span>`;
            const venP = document.createElement("p");
            venP.className = "pendencia-card__meta-item";
            venP.innerHTML = `<span class="pendencia-card__meta-lbl">Vencimento</span><span class="pendencia-card__meta-val">${escapeHtml(p.vencimento || "—")}</span>`;
            const cnpjP = document.createElement("p");
            cnpjP.className = "pendencia-card__meta-item";
            const docLbl = p.empresaCnpj ? "CNPJ" : "CPF";
            const docVal = p.empresaCnpj
                ? formatarCnpj(String(p.empresaCnpj).replace(/\D/g, ""))
                : p.clientePessoaFisicaCpf
                  ? formatarCpf(String(p.clientePessoaFisicaCpf).replace(/\D/g, ""))
                  : "—";
            cnpjP.innerHTML = `<span class="pendencia-card__meta-lbl">${docLbl}</span><span class="pendencia-card__meta-val">${escapeHtml(docVal)}</span>`;
            meta.appendChild(docP);
            meta.appendChild(venP);
            meta.appendChild(cnpjP);
            card.appendChild(meta);

            if (p.status === "VALIDADO") {
                const actions = document.createElement("div");
                actions.className = "pendencia-card__actions";
                const btn = document.createElement("button");
                btn.type = "button";
                btn.className = "btn btn--ghost btn--inline";
                btn.textContent = "Ver dados extraídos";
                btn.addEventListener("click", () => abrirModalDadosExtraidos(p.id));
                actions.appendChild(btn);
                card.appendChild(actions);
            }

            listaPendencias.appendChild(card);
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
            const header = document.createElement("div");
            header.className = "pendencia-cliente-header";
            header.textContent = `#${p.id} ${p.templateDocumentoNome} — vence ${p.vencimento}`;

            const badge = document.createElement("span");
            badge.className = "status-badge";
            if (p.status === "VALIDADO") {
                badge.classList.add("status-badge--ok");
                badge.textContent = "OK pela IA";
            } else if (p.status === "REJEITADO") {
                badge.classList.add("status-badge--rejeitado");
                badge.textContent = "Rejeitado pela IA";
            } else if (p.status === "ENVIADO") {
                badge.classList.add("status-badge--analise");
                badge.textContent = "Em análise da IA";
            } else {
                badge.classList.add("status-badge--pendente");
                badge.textContent = "Pendente de envio";
            }

            header.appendChild(document.createTextNode(" "));
            header.appendChild(badge);
            li.appendChild(header);

            if (p.observacaoAnalise) {
                const motivo = document.createElement("div");
                motivo.className = "muted pendencia-cliente-motivo";
                motivo.textContent = `Motivo: ${p.observacaoAnalise}`;
                li.appendChild(motivo);
            }
            if (p.status === "VALIDADO") {
                const btn = document.createElement("button");
                btn.type = "button";
                btn.className = "btn btn--ghost btn--inline";
                btn.textContent = "Ver dados extraídos";
                btn.addEventListener("click", () => abrirModalDadosExtraidos(p.id));
                li.appendChild(btn);
            }
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
            const jsonRaw = item.dadosExtraidosJson || "";
            const holeriteCompleto =
                jsonRaw.includes('"detalhamentoDocumento"') && jsonRaw.includes("HOLERITE_ESCRITORIO_COMPLETO");
            const jsonPretty = formatJson(item.dadosExtraidosJson);
            const avisoHolerite = holeriteCompleto
                ? '<p class="holerite-badge holerite-badge--lista">Holerite com captura completa — use o botão "Ver campos" para o painel estruturado.</p>'
                : "";
            li.innerHTML = `#${item.id} - ${item.nomeArquivoOriginal} - ${item.tipoDocumento} - conf. ${confianca} - ${item.status} <span class="severity severity--${sevClass}">${item.severidade}</span><br><span class="muted">${motivo}</span>${avisoHolerite}<pre class="ia-json">${jsonPretty}</pre>`;
            const histBtn = document.createElement("button");
            histBtn.type = "button";
            histBtn.className = "btn btn--ghost";
            histBtn.textContent = "Ver histórico";
            histBtn.style.marginLeft = "8px";
            histBtn.addEventListener("click", async () => abrirModalHistorico(item.id));
            li.appendChild(histBtn);
            if (item.status === "PROCESSADO" && item.pendenciaId) {
                const dadosBtn = document.createElement("button");
                dadosBtn.type = "button";
                dadosBtn.className = "btn btn--ghost";
                dadosBtn.style.marginLeft = "8px";
                dadosBtn.textContent = "Ver campos";
                dadosBtn.addEventListener("click", () => abrirModalDadosExtraidos(item.pendenciaId));
                li.appendChild(dadosBtn);
            }
            listaIaRevisao.appendChild(li);
        });
    }

    async function abrirModalHistorico(processamentoId) {
        modalHistoricoContent.innerHTML =
            "<div class='dashboard-donuts-loading'><span class='spinner-360' aria-hidden='true'></span><span class='muted'>Carregando histórico…</span></div>";
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

    function formatarLabelCampo(chave) {
        if (!chave) return "";
        return chave
            .replace(/([a-z])([A-Z])/g, "$1 $2")
            .replace(/_/g, " ")
            .replace(/\b\w/g, (c) => c.toUpperCase());
    }

    /** Painel visual do holerite a partir de {@code detalhamentoDocumento} (JSON da API). */
    function renderHoleritePainelDetalhe(det, capturaPerfil) {
        if (!det || typeof det !== "object") return "";
        const esc = (v) => escapeHtml(v == null ? "" : String(v));
        const nonEmpty = (v) => v != null && String(v).trim() !== "";

        function kvRows(obj, labels) {
            if (!obj || typeof obj !== "object") return "";
            let html = "";
            for (const [key, label] of Object.entries(labels)) {
                const val = obj[key];
                if (nonEmpty(val)) {
                    html += `<div class="holerite-kv"><span class="holerite-k">${esc(label)}</span><span class="holerite-v">${esc(val)}</span></div>`;
                }
            }
            return html;
        }

        function rubricaTable(items, titulo) {
            if (!Array.isArray(items) || !items.length) return "";
            const head = "<thead><tr><th>Descrição</th><th>Ref.</th><th>Valor</th></tr></thead>";
            const body = items
                .map((r) => {
                    const d = r.descricao != null ? r.descricao : "";
                    const ref = r.referencia != null ? r.referencia : "";
                    const br =
                        r.valorOriginalBr != null && String(r.valorOriginalBr).trim() !== ""
                            ? r.valorOriginalBr
                            : r.valorNumerico != null
                              ? r.valorNumerico
                              : "";
                    return `<tr><td>${esc(d)}</td><td>${esc(ref)}</td><td>${esc(br)}</td></tr>`;
                })
                .join("");
            return `<section class="holerite-secao"><h4 class="holerite-secao-titulo">${esc(titulo)}</h4><table class="dados-tabela holerite-mini">${head}<tbody>${body}</tbody></table></section>`;
        }

        let html = '<div class="holerite-painel">';
        if (capturaPerfil === "HOLERITE_ESCRITORIO_COMPLETO") {
            html += '<p class="holerite-badge">Holerite · captura alinhada ao escritório</p>';
        }

        const emp = det.empresa;
        if (emp && typeof emp === "object") {
            const inner = kvRows(emp, {
                razaoSocial: "Razão social",
                cnpjFormatado: "CNPJ",
                cnpjDigitos: "CNPJ (só dígitos)",
                endereco: "Endereço",
            });
            if (inner) {
                html += `<section class="holerite-secao"><h4 class="holerite-secao-titulo">Empregador</h4><div class="holerite-kv-grid">${inner}</div></section>`;
            }
        }

        const fun = det.funcionario;
        if (fun && typeof fun === "object") {
            const inner = kvRows(fun, {
                nome: "Nome",
                cpf: "CPF",
                pisPasep: "PIS/PASEP",
                cargo: "Cargo",
                departamento: "Departamento",
                dataAdmissao: "Admissão",
            });
            if (inner) {
                html += `<section class="holerite-secao"><h4 class="holerite-secao-titulo">Trabalhador</h4><div class="holerite-kv-grid">${inner}</div></section>`;
            }
        }

        const per = det.periodo;
        if (per && typeof per === "object") {
            const inner = kvRows(per, {
                competencia: "Competência / referência",
                tipoFolha: "Tipo de folha",
                diasTrabalhados: "Dias trabalhados",
            });
            if (inner) {
                html += `<section class="holerite-secao"><h4 class="holerite-secao-titulo">Período</h4><div class="holerite-kv-grid">${inner}</div></section>`;
            }
        }

        html += rubricaTable(det.proventos, "Proventos");
        html += rubricaTable(det.descontos, "Descontos");

        const tot = det.totais;
        if (tot && typeof tot === "object") {
            const inner = kvRows(tot, {
                totalProventosBr: "Total proventos",
                totalDescontosBr: "Total descontos",
                valorLiquidoBr: "Valor líquido",
                valorLiquidoNumerico: "Líquido (número)",
            });
            if (inner) {
                html += `<section class="holerite-secao"><h4 class="holerite-secao-titulo">Totais</h4><div class="holerite-kv-grid">${inner}</div></section>`;
            }
        }

        const bas = det.bases;
        if (bas && typeof bas === "object") {
            const inner = kvRows(bas, {
                salarioBaseESocialBr: "Salário base eSocial",
                baseCalculoFgtsBr: "Base FGTS",
                fgtsMesBr: "FGTS mês",
                baseCalculoInssBr: "Base INSS",
            });
            if (inner) {
                html += `<section class="holerite-secao"><h4 class="holerite-secao-titulo">Bases e encargos</h4><div class="holerite-kv-grid">${inner}</div></section>`;
            }
        }

        const extra = det.camposAdicionais;
        if (extra && typeof extra === "object" && !Array.isArray(extra)) {
            const keys = Object.keys(extra);
            if (keys.length) {
                let inner = "";
                for (const k of keys) {
                    if (nonEmpty(extra[k])) {
                        inner += `<div class="holerite-kv"><span class="holerite-k">${esc(k)}</span><span class="holerite-v">${esc(extra[k])}</span></div>`;
                    }
                }
                if (inner) {
                    html += `<section class="holerite-secao"><h4 class="holerite-secao-titulo">Outros</h4><div class="holerite-kv-grid">${inner}</div></section>`;
                }
            }
        }

        html += "</div>";
        return html;
    }

    async function abrirModalDadosExtraidos(pendenciaId) {
        modalDadosExtraidosMeta.innerHTML =
            '<span class="loading-inline"><span class="spinner-360" aria-hidden="true"></span><span>Carregando…</span></span>';
        modalDadosExtraidosContent.innerHTML = "";
        setFeedback(modalDadosFeedback, "", null);
        modalDadosToolbar.classList.add("hidden");
        modalDadosContext = { pendenciaId: null, processamentoId: null };
        modalDadosExtraidos.classList.remove("hidden");
        try {
            const data = await fetchJson(`/api/pendencias/${pendenciaId}/dados-extraidos`, { headers: headers() });
            modalDadosContext = { pendenciaId, processamentoId: data.processamentoId };
            let meta = `${data.nomeArquivoOriginal || "Documento"} — ${data.tipoDocumento || ""} — conf. ${Number(data.confianca || 0).toFixed(2)}`;
            if (data.capturaPerfil === "HOLERITE_ESCRITORIO_COMPLETO") {
                meta += " · captura completa (holerite)";
            }
            modalDadosExtraidosMeta.textContent = meta;
            modalDadosToolbar.classList.remove("hidden");
            const editavelModal = session.perfil === "CONTADOR" || session.perfil === "CLIENTE";
            btnSalvarDadosExtraidos.classList.toggle("hidden", !editavelModal);
            const temDetalheHolerite =
                data.detalhamentoDocumento && typeof data.detalhamentoDocumento === "object";
            const todosCampos = Array.isArray(data.campos) ? data.campos : [];
            if (!todosCampos.length && !temDetalheHolerite) {
                modalDadosExtraidosContent.innerHTML = "<span class='muted'>Nenhum campo armazenado.</span>";
                return;
            }
            const holeritePrefix = "holerite.";
            const camposTabela = temDetalheHolerite
                ? todosCampos.filter((c) => !String(c.nome || "").startsWith(holeritePrefix))
                : todosCampos;
            const camposHoleriteOcultos = temDetalheHolerite
                ? todosCampos.filter((c) => String(c.nome || "").startsWith(holeritePrefix))
                : [];

            const editavel = editavelModal;
            const painelHtml = temDetalheHolerite
                ? renderHoleritePainelDetalhe(data.detalhamentoDocumento, data.capturaPerfil)
                : "";

            function linhaCampo(c) {
                const rot = escapeHtml(formatarLabelCampo(c.nome));
                const tipo = escapeHtml(c.tipo || "TEXTO");
                if (editavel) {
                    const nomeEnc = encodeURIComponent(c.nome || "");
                    const valAttr = escapeAttr(c.valor ?? "");
                    return `<tr><th>${rot}</th><td><input type="text" class="dados-campo-valor" data-nome="${nomeEnc}" value="${valAttr}" /></td><td class="muted">${tipo}</td></tr>`;
                }
                return `<tr><th>${rot}</th><td>${escapeHtml(c.valor || "")}</td><td class="muted">${tipo}</td></tr>`;
            }

            const rows = camposTabela.map(linhaCampo).join("");
            const hiddenHolerite =
                editavel && camposHoleriteOcultos.length
                    ? camposHoleriteOcultos
                          .map((c) => {
                              const nomeEnc = encodeURIComponent(c.nome || "");
                              const valAttr = escapeAttr(c.valor ?? "");
                              return `<input type="hidden" class="dados-campo-valor" data-nome="${nomeEnc}" value="${valAttr}" />`;
                          })
                          .join("")
                    : !editavel && camposHoleriteOcultos.length
                      ? `<details class="holerite-detalhe-tecnico"><summary>Campos técnicos do holerite (plano)</summary><table class="dados-tabela"><thead><tr><th>Campo</th><th>Valor</th><th>Tipo</th></tr></thead><tbody>${camposHoleriteOcultos.map(linhaCampo).join("")}</tbody></table></details>`
                      : "";

            const tituloTabela = temDetalheHolerite ? "<h4 class=\"dados-campos-principais\">Campos principais e conferência</h4>" : "";
            const tabelaHtml =
                camposTabela.length > 0
                    ? `${tituloTabela}<table class="dados-tabela"><thead><tr><th>Campo</th><th>Valor</th><th>Tipo</th></tr></thead><tbody>${rows}</tbody></table>`
                    : temDetalheHolerite
                      ? "<p class=\"muted\">Sem linhas gerais além do holerite estruturado acima.</p>"
                      : "";

            modalDadosExtraidosContent.innerHTML = `${painelHtml}${tabelaHtml}${hiddenHolerite}`;
        } catch (e) {
            modalDadosExtraidosMeta.textContent = "";
            modalDadosExtraidosContent.innerHTML = `<span class='feedback err'>${escapeHtml(e.message || "Falha ao carregar dados.")}</span>`;
        }
    }

    async function exportarDadosExtraidosArquivo(formato) {
        if (!modalDadosContext.pendenciaId) return;
        setFeedback(modalDadosFeedback, "", null);
        try {
            const res = await fetch(
                `/api/pendencias/${modalDadosContext.pendenciaId}/dados-extraidos/export?formato=${encodeURIComponent(formato)}`,
                { headers: headers() }
            );
            if (!res.ok) {
                const text = await res.text();
                let msg = res.statusText;
                try {
                    const j = JSON.parse(text);
                    if (j.message) msg = j.message;
                } catch { /* ignore */ }
                throw new Error(msg);
            }
            const blob = await res.blob();
            const url = URL.createObjectURL(blob);
            const a = document.createElement("a");
            a.href = url;
            a.download = `dados-extraidos-${modalDadosContext.pendenciaId}.${formato === "pdf" ? "pdf" : "csv"}`;
            a.click();
            URL.revokeObjectURL(url);
            setFeedback(modalDadosFeedback, "Download iniciado.", "ok");
        } catch (e) {
            setFeedback(modalDadosFeedback, e.message || "Falha na exportação.", "err");
        }
    }

    async function salvarDadosExtraidosEditados() {
        if (!modalDadosContext.processamentoId) return;
        setFeedback(modalDadosFeedback, "", null);
        const inputs = modalDadosExtraidosContent.querySelectorAll("input.dados-campo-valor");
        const campos = [...inputs].map((el) => ({
            nome: decodeURIComponent(el.getAttribute("data-nome") || ""),
            valor: el.value,
        }));
        try {
            await fetchJson(`/api/inteligencia/documentos/${modalDadosContext.processamentoId}/campos`, {
                method: "PATCH",
                headers: headers({ "Content-Type": "application/json" }),
                body: JSON.stringify({ campos }),
            });
            setFeedback(modalDadosFeedback, "Alterações salvas. Registro no histórico da revisão.", "ok");
            await abrirModalDadosExtraidos(modalDadosContext.pendenciaId);
        } catch (e) {
            setFeedback(modalDadosFeedback, e.message || "Falha ao salvar.", "err");
        }
    }

    function escapeHtml(s) {
        const div = document.createElement("div");
        div.textContent = s == null ? "" : String(s);
        return div.innerHTML;
    }

    function escapeAttr(s) {
        return String(s ?? "")
            .replace(/&/g, "&amp;")
            .replace(/"/g, "&quot;")
            .replace(/</g, "&lt;");
    }

    function fecharModalDadosExtraidos() {
        modalDadosExtraidos.classList.add("hidden");
    }

    function formatJson(jsonStr) {
        if (!jsonStr) return "{}";
        try {
            return JSON.stringify(JSON.parse(jsonStr), null, 2);
        } catch {
            return jsonStr;
        }
    }

    async function carregarEmpresas() {
        const incluir = empresasIncluirInativasEl && empresasIncluirInativasEl.checked;
        const qs = incluir ? "?incluirInativas=true" : "";
        const empresas = await fetchJson(`/api/empresas${qs}`, { headers: headers() });
        renderEmpresas(empresas);
    }

    async function carregarPendenciasContador() {
        const ano = Number(document.getElementById("pend-ano").value);
        const mes = Number(document.getElementById("pend-mes").value);
        const chk = document.getElementById("pend-mostrar-arquivadas");
        const incluirArquivadas = chk ? chk.checked : false;
        let arquivoInfo = { existeCompetencia: false, arquivada: false, arquivadaEm: null };
        try {
            arquivoInfo = await fetchJson(`/api/pendencias/competencia-arquivo?ano=${ano}&mes=${mes}`, {
                headers: headers(),
            });
        } catch {
            /* meta opcional */
        }
        let pendencias = [];
        try {
            pendencias = await fetchJson(
                `/api/pendencias?ano=${ano}&mes=${mes}&incluirArquivadas=${incluirArquivadas}`,
                { headers: headers() }
            );
        } catch (e) {
            setFeedback(pendenciasFeedback, e.message || "Erro ao listar pendências.", "err");
            renderPendenciasContador([], arquivoInfo, incluirArquivadas);
            return;
        }
        setFeedback(pendenciasFeedback, "", null);
        renderPendenciasContador(pendencias, arquivoInfo, incluirArquivadas);
    }

    async function carregarFilaIa() {
        const incluirConcluidos = document.getElementById("ia-incluir-concluidos")?.checked || false;
        const itens = await fetchJson(
            `/api/inteligencia/documentos?incluirConcluidosNaRevisao=${incluirConcluidos}`,
            { headers: headers() }
        );
        renderIaRevisao(itens);
    }

    async function carregarPendenciasCliente() {
        const ano = Number(document.getElementById("cliente-ano").value);
        const mes = Number(document.getElementById("cliente-mes").value);
        const pendencias = await fetchJson(`/api/pendencias?ano=${ano}&mes=${mes}`, { headers: headers() });
        renderPendenciasCliente(pendencias);
    }

    function renderDocValidadoKpis(doc, camposArr, temDetalhe) {
        const map = {};
        (camposArr || []).forEach((c) => {
            map[c.nome] = c.valor;
        });
        let valorTitulo = "Valor principal";
        let valorVal = map.valor;
        if (temDetalhe && doc.detalhamentoDocumento && doc.detalhamentoDocumento.totais) {
            const t = doc.detalhamentoDocumento.totais;
            if (t.valorLiquidoBr) {
                valorTitulo = "Valor líquido";
                valorVal = t.valorLiquidoBr;
            } else if (t.valorLiquidoNumerico) {
                valorTitulo = "Valor líquido";
                valorVal = t.valorLiquidoNumerico;
            }
        }
        const cnpjDig = map.cnpj || "";
        const cnpjFmt =
            cnpjDig && String(cnpjDig).replace(/\D/g, "").length === 14
                ? formatarCnpj(String(cnpjDig).replace(/\D/g, ""))
                : cnpjDig || "—";
        const comp = map.competencia || "—";
        const ven = map.vencimento || "—";
        return `<div class="doc-validado-kpis" role="group" aria-label="Resumo do documento">
            <div class="doc-validado-kpi"><span class="doc-validado-kpi__label">CNPJ</span><span class="doc-validado-kpi__val">${escapeHtml(cnpjFmt)}</span></div>
            <div class="doc-validado-kpi"><span class="doc-validado-kpi__label">${escapeHtml(valorTitulo)}</span><span class="doc-validado-kpi__val doc-validado-kpi__val--destaque">${escapeHtml(valorVal || "—")}</span></div>
            <div class="doc-validado-kpi"><span class="doc-validado-kpi__label">Competência</span><span class="doc-validado-kpi__val">${escapeHtml(comp)}</span></div>
            <div class="doc-validado-kpi"><span class="doc-validado-kpi__label">Vencimento</span><span class="doc-validado-kpi__val">${escapeHtml(ven)}</span></div>
        </div>`;
    }

    function renderDocValidadoCard(doc, editavel) {
        const campos = doc.campos || [];
        const temDetalhe = doc.detalhamentoDocumento && typeof doc.detalhamentoDocumento === "object";
        const captura = doc.capturaPerfil || "";
        const holeritePrefix = "holerite.";
        const camposTabela = temDetalhe
            ? campos.filter((c) => !String(c.nome || "").startsWith(holeritePrefix))
            : campos;
        const camposOcultos = temDetalhe
            ? campos.filter((c) => String(c.nome || "").startsWith(holeritePrefix))
            : [];

        const conf = doc.confianca != null ? Number(doc.confianca).toFixed(2) : "—";
        const confPct = doc.confianca != null ? Math.round(Number(doc.confianca) * 100) : null;
        const tipoShort = escapeHtml(doc.tipoDocumentoDetectado || "—");
        const painelHol = temDetalhe ? renderHoleritePainelDetalhe(doc.detalhamentoDocumento, captura) : "";
        const kpis = renderDocValidadoKpis(doc, campos, temDetalhe);
        const badgeCompleto =
            captura === "HOLERITE_ESCRITORIO_COMPLETO"
                ? '<span class="doc-chip doc-chip--accent">Captura completa</span>'
                : "";

        function linhaCampoDoc(c) {
            const rot = escapeHtml(formatarLabelCampo(c.nome));
            const tipo = escapeHtml(c.tipo || "TEXTO");
            if (editavel) {
                const nomeEnc = encodeURIComponent(c.nome || "");
                const valAttr = escapeAttr(c.valor ?? "");
                return `<tr><th>${rot}</th><td><input type="text" class="dados-campo-valor-doc" data-nome="${nomeEnc}" value="${valAttr}" /></td><td class="muted">${tipo}</td></tr>`;
            }
            return `<tr><th>${rot}</th><td>${escapeHtml(c.valor ?? "")}</td><td class="muted">${tipo}</td></tr>`;
        }

        let tableBlock = "";
        let actions = "";
        if (editavel && (camposTabela.length || camposOcultos.length)) {
            const rows = camposTabela.map(linhaCampoDoc).join("");
            const hiddenHolerite =
                camposOcultos.length > 0
                    ? camposOcultos
                          .map((c) => {
                              const nomeEnc = encodeURIComponent(c.nome || "");
                              const valAttr = escapeAttr(c.valor ?? "");
                              return `<input type="hidden" class="dados-campo-valor-doc" data-nome="${nomeEnc}" value="${valAttr}" />`;
                          })
                          .join("")
                    : "";
            const tituloSec =
                temDetalhe && camposTabela.length
                    ? '<h4 class="doc-validado-secao-titulo">Campos para conferência e edição</h4>'
                    : camposTabela.length
                      ? '<h4 class="doc-validado-secao-titulo">Campos extraídos</h4>'
                      : '<h4 class="doc-validado-secao-titulo">Conferência</h4>';
            const tabelaVisivel =
                camposTabela.length > 0
                    ? `<table class="dados-tabela dados-tabela--doc-validado"><thead><tr><th>Campo</th><th>Valor</th><th>Tipo</th></tr></thead><tbody>${rows}</tbody></table>`
                    : camposOcultos.length > 0
                      ? `<p class="muted doc-validado-inline-hint">O detalhamento está no painel acima. Campos técnicos do holerite permanecem ao salvar.</p>`
                      : "";
            tableBlock =
                camposTabela.length > 0 || camposOcultos.length > 0
                    ? `${tituloSec}<div class="doc-validado-table-wrap">${tabelaVisivel}${hiddenHolerite}</div>`
                    : "";
            actions = `<div class="doc-validado-card__actions"><button type="button" class="btn btn--ghost btn-salvar-doc-validado" data-processamento-id="${doc.processamentoId}">Salvar alterações</button><span class="doc-validado-save-feedback feedback" role="status"></span></div>`;
        } else {
            const rows = camposTabela.map(linhaCampoDoc).join("");
            const tituloSec =
                temDetalhe && camposTabela.length
                    ? '<h4 class="doc-validado-secao-titulo">Campos para conferência</h4>'
                    : "";
            tableBlock =
                camposTabela.length > 0
                    ? `${tituloSec}<div class="doc-validado-table-wrap"><table class="dados-tabela dados-tabela--doc-validado"><thead><tr><th>Campo</th><th>Valor</th><th>Tipo</th></tr></thead><tbody>${rows}</tbody></table></div>`
                    : !temDetalhe
                      ? "<p class=\"muted doc-validado-sem-campos\">Nenhum campo extraído armazenado para este documento.</p>"
                      : "";
        }

        const inicial = escapeHtml((doc.nomeArquivoOriginal || "Arquivo").slice(0, 1).toUpperCase());

        return `<article class="doc-validado-card">
            <div class="doc-validado-card__top">
                <div class="doc-validado-card__identity">
                    <div class="doc-validado-avatar" aria-hidden="true">${inicial}</div>
                    <div class="doc-validado-card__titles">
                        <h3 class="doc-validado-card__filename">${escapeHtml(doc.nomeArquivoOriginal || "Arquivo")}</h3>
                        <p class="doc-validado-card__pendencia muted">Pendência #${doc.pendenciaId} — ${escapeHtml(doc.templatePendenciaNome || "")}</p>
                    </div>
                </div>
                <div class="doc-validado-card__meta-row">
                    <div class="doc-validado-card__chips">
                        <span class="doc-chip doc-chip--tipo" title="Tipo detectado pela IA">${tipoShort}</span>
                        ${badgeCompleto}
                        <span class="doc-chip doc-chip--conf" title="Confiança da leitura">${confPct != null ? `${confPct}%` : conf} confiança</span>
                    </div>
                    <p class="doc-validado-card__updated muted">Atualizado em ${escapeHtml(doc.atualizadoEm || "—")}</p>
                </div>
            </div>
            ${kpis}
            ${painelHol ? `<div class="doc-validado-holerite">${painelHol}</div>` : ""}
            <div class="doc-validado-card__body">${tableBlock}</div>
            ${actions}
        </article>`;
    }

    function docTabIconSvg(idAba) {
        const id = (idAba || "OUTROS").toUpperCase();
        const svg = `
<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">`;
        const icons = {
            NOTA_FISCAL: `${svg}<path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><path d="M14 2v6h6"/><path d="M12 18v-6"/><path d="M9 15h6"/><circle cx="12" cy="10" r="1" fill="currentColor" stroke="none"/></svg>`,
            FOLHA_PAGAMENTO: `${svg}<path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><path d="M14 2v6h6"/><path d="M9 13h6"/><path d="M9 17h4"/><circle cx="12" cy="9" r="2"/></svg>`,
            EXTRATO_BANCARIO: `${svg}<rect x="3" y="3" width="18" height="18" rx="2"/><path d="M3 9h18"/><path d="M9 21V9"/></svg>`,
            RECIBO_DESPESA: `${svg}<path d="M4 2v20l2-1 2 1 2-1 2 1 2-1 2 1 2-1 2 1V2l-2 1-2-1-2 1-2-1-2 1-2-1-2 1-2-1z"/><path d="M8 10h8"/><path d="M8 14h5"/></svg>`,
            GUIA_IMPOSTO: `${svg}<path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><path d="M14 2v6h6"/><path d="m9 15 2 2 4-4"/></svg>`,
            CONTRATO_SOCIAL: `${svg}<path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><path d="M14 2v6h6"/><path d="m9 15 2 2 4-4"/><path d="M9 12h.01"/></svg>`,
            ATA_REUNIAO: `${svg}<path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>`,
            DECLARACAO_ACESSORIA: `${svg}<path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><path d="M14 2v6h6"/><path d="M8 13h8"/><path d="M8 17h6"/></svg>`,
            EMPRESTIMO_FINANCIAMENTO: `${svg}<rect x="2" y="5" width="20" height="14" rx="2"/><path d="M2 10h20"/><path d="M6 15h.01M12 15h.01M18 15h.01"/></svg>`,
            FLUXO_CAIXA: `${svg}<path d="M3 3v18h18"/><path d="M7 12l4-4 4 4 4-4"/><path d="M7 18h10"/></svg>`,
            OUTROS: `${svg}<path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/><path d="M12 11v6"/><path d="M9 14h6"/></svg>`,
        };
        return icons[id] || icons.OUTROS;
    }

    function idAbaFromPayload(a) {
        if (a.idAba) return String(a.idAba);
        const t = (a.titulo || "").toLowerCase();
        if (t.includes("nota")) return "NOTA_FISCAL";
        if (t.includes("holerite") || t.includes("folha")) return "FOLHA_PAGAMENTO";
        if (t.includes("extrato")) return "EXTRATO_BANCARIO";
        if (t.includes("recibo")) return "RECIBO_DESPESA";
        if (t.includes("guia")) return "GUIA_IMPOSTO";
        if (t.includes("contrato")) return "CONTRATO_SOCIAL";
        if (t.includes("ata")) return "ATA_REUNIAO";
        if (t.includes("declara")) return "DECLARACAO_ACESSORIA";
        if (t.includes("empréstimo") || t.includes("financiamento")) return "EMPRESTIMO_FINANCIAMENTO";
        if (t.includes("fluxo")) return "FLUXO_CAIXA";
        return "OUTROS";
    }

    function hideDocsValidadosPager() {
        if (docsValidadosPager) docsValidadosPager.hidden = true;
    }

    function updateDocsValidadosPager(activeIdx, total) {
        if (!docsValidadosPager || !docsValidadosPagerCurrent || !docsValidadosPagerTotal) return;
        if (total <= 0) {
            hideDocsValidadosPager();
            return;
        }
        docsValidadosPager.hidden = false;
        docsValidadosPagerCurrent.textContent = String(activeIdx + 1);
        docsValidadosPagerTotal.textContent = String(total);
        if (docsValidadosPagerPrev) docsValidadosPagerPrev.disabled = activeIdx <= 0;
        if (docsValidadosPagerNext) docsValidadosPagerNext.disabled = activeIdx >= total - 1;
    }

    function getActiveDocsValidadosTabIndex() {
        const active = docsValidadosTabs.querySelector(".doc-tab--active");
        if (!active) return 0;
        return Number(active.getAttribute("data-tab-idx"));
    }

    function selectDocsValidadosTab(j) {
        const tabs = docsValidadosTabs.querySelectorAll(".doc-tab");
        const panels = docsValidadosPanels.querySelectorAll(".doc-tab-panel");
        if (!tabs.length) return;
        const n = tabs.length;
        const idx = Math.max(0, Math.min(j, n - 1));
        tabs.forEach((b, k) => {
            const on = k === idx;
            b.classList.toggle("doc-tab--active", on);
            b.setAttribute("aria-selected", on);
        });
        panels.forEach((p, k) => {
            p.classList.toggle("hidden", k !== idx);
        });
        updateDocsValidadosPager(idx, n);
        const activeBtn = tabs[idx];
        if (activeBtn && activeBtn.scrollIntoView) {
            activeBtn.scrollIntoView({ block: "nearest", inline: "center", behavior: "smooth" });
        }
    }

    function renderDocumentosValidadosPorAba(payload) {
        const abas = payload.abas || [];
        const editavel = session && (session.perfil === "CLIENTE" || session.perfil === "CONTADOR");
        if (!abas.length) {
            docsValidadosPanels.innerHTML = "<p class=\"muted\">Nenhuma aba disponível.</p>";
            hideDocsValidadosPager();
            return;
        }
        let activeIdx = abas.findIndex((a) => (a.documentos || []).length > 0);
        if (activeIdx < 0) activeIdx = 0;

        docsValidadosTabs.innerHTML = abas
            .map((a, i) => {
                const n = (a.documentos || []).length;
                const abaId = idAbaFromPayload(a);
                const badge = n
                    ? `<span class="doc-tab-carousel__count" aria-label="${n} documento(s)">${n}</span>`
                    : "";
                const selected = i === activeIdx;
                return (
                    `<button type="button" role="tab" class="doc-tab doc-tab--carousel${selected ? " doc-tab--active" : ""}" data-tab-idx="${i}" aria-selected="${selected}" title="${escapeAttr(a.titulo)}">`
                    + `<span class="doc-tab-carousel__icon-wrap">`
                    + `<span class="doc-tab-carousel__icon">${docTabIconSvg(abaId)}</span>`
                    + `${badge}`
                    + `</span>`
                    + `<span class="doc-tab-carousel__label">${escapeHtml(a.titulo)}</span>`
                    + `</button>`
                );
            })
            .join("");

        docsValidadosPanels.innerHTML = abas
            .map((a, i) => {
                const docs = a.documentos || [];
                const inner = docs.length
                    ? docs.map((d) => renderDocValidadoCard(d, editavel)).join("")
                    : "<p class=\"muted doc-tab-empty\">Nenhum documento com análise concluída nesta categoria ainda.</p>";
                const hidden = i !== activeIdx ? " hidden" : "";
                return `<div class="doc-tab-panel${hidden}" data-panel-idx="${i}" role="tabpanel">${inner}</div>`;
            })
            .join("");

        docsValidadosTabs.querySelectorAll(".doc-tab").forEach((btn) => {
            btn.addEventListener("click", () => {
                const j = Number(btn.getAttribute("data-tab-idx"));
                selectDocsValidadosTab(j);
            });
        });
        updateDocsValidadosPager(activeIdx, abas.length);
    }

    function atualizarCabecalhoEmpresaDocsValidados(data) {
        if (!docsValidadosEmpresaWrap || !docsValidadosClienteEmpresa) return;
        const arqDetails = document.getElementById("docs-validados-arq-details");
        if (arqDetails) {
            arqDetails.classList.toggle("hidden", session.perfil !== "CONTADOR");
        }
        if (session.perfil === "CONTADOR") {
            docsValidadosClienteEmpresa.classList.add("hidden");
            docsValidadosEmpresaWrap.classList.remove("hidden");
        } else {
            docsValidadosEmpresaWrap.classList.add("hidden");
            docsValidadosClienteEmpresa.classList.remove("hidden");
            if (data && data.cnpj && data.razaoSocial) {
                docsValidadosClienteEmpresa.textContent = `CNPJ: ${formatarCnpj(data.cnpj)} — ${data.razaoSocial}`;
            } else if (data && data.cpfClientePf && data.nomeClientePf) {
                docsValidadosClienteEmpresa.textContent = `CPF: ${formatarCpf(data.cpfClientePf)} — ${data.nomeClientePf}`;
            } else {
                docsValidadosClienteEmpresa.textContent = "Nenhum tomador vinculado ao seu usuário.";
            }
        }
    }

    async function carregarDocumentosValidadosPorAba() {
        if (!session || (session.perfil !== "CLIENTE" && session.perfil !== "CONTADOR")) return;
        setFeedback(docsValidadosFeedback, "Carregando...", null);
        docsValidadosTabs.innerHTML = "";
        docsValidadosPanels.innerHTML = "";
        hideDocsValidadosPager();
        try {
            let url = "/api/inteligencia/documentos/portal/validados-por-aba";
            if (session.perfil === "CONTADOR") {
                if (!docsValidadosEmpresaEl || docsValidadosEmpresaEl.options.length === 0) {
                    atualizarCabecalhoEmpresaDocsValidados({});
                    setFeedback(docsValidadosFeedback, "Cadastre uma empresa para visualizar os documentos por CNPJ.", "err");
                    hideDocsValidadosPager();
                    return;
                }
                const empId = docsValidadosEmpresaEl.value;
                if (!empId) {
                    setFeedback(docsValidadosFeedback, "Selecione a empresa (CNPJ).", "err");
                    hideDocsValidadosPager();
                    return;
                }
                const incluirArq = document.getElementById("docs-validados-incluir-arquivadas")?.checked === true;
                const bust = Date.now();
                url += `?empresaId=${encodeURIComponent(empId)}&incluirCompetenciasArquivadas=${incluirArq ? 1 : 0}&_=${bust}`;
            }
            const data = await fetchJson(url, { headers: headers(), cache: "no-store" });
            atualizarCabecalhoEmpresaDocsValidados(data);
            renderDocumentosValidadosPorAba(data);
            setFeedback(docsValidadosFeedback, "", null);
        } catch (e) {
            setFeedback(docsValidadosFeedback, e.message || "Falha ao carregar documentos validados.", "err");
            hideDocsValidadosPager();
        }
    }

    function refreshDocumentosValidadosIfNeeded(screenId) {
        if (!session || (session.perfil !== "CLIENTE" && session.perfil !== "CONTADOR")) return;
        if (screenId === "screen-documentos-validados-ia") {
            carregarDocumentosValidadosPorAba();
        }
    }

    function contarPendenciasPorStatus(list) {
        const acc = { PENDENTE: 0, ENVIADO: 0, VALIDADO: 0, REJEITADO: 0 };
        (list || []).forEach((p) => {
            const s = p.status;
            if (Object.prototype.hasOwnProperty.call(acc, s)) acc[s] += 1;
        });
        return acc;
    }

    const LABEL_DOC_FISCAL = {
        NFE: "NF-e",
        NFSE: "NFS-e",
        NFCE: "NFC-e",
        CTE: "CT-e",
        MDFE: "MDF-e",
        CTE_OS: "CT-e OS",
    };
    const LABEL_OP_FISCAL = { ENTRADA: "Entrada", SAIDA: "Saída" };

    const DONUT_COLORS = {
        accent: "#2563EB",
        green: "#10B981",
        amber: "#F59E0B",
        red: "#EF4444",
        slate: "#94A3B8",
        violet: "#8B5CF6",
    };

    function conicGradientFromSegments(segments) {
        const total = segments.reduce((s, x) => s + x.value, 0);
        if (total <= 0) {
            return `conic-gradient(${DONUT_COLORS.slate} 0deg 360deg)`;
        }
        let acc = 0;
        const parts = [];
        segments.forEach((it) => {
            const frac = it.value / total;
            const start = acc;
            acc += frac;
            const a0 = (start * 360).toFixed(3);
            const a1 = (acc * 360).toFixed(3);
            parts.push(`${it.color} ${a0}deg ${a1}deg`);
        });
        return `conic-gradient(${parts.join(", ")})`;
    }

    function renderDonutCard(title, segments, centerText) {
        const grad = conicGradientFromSegments(segments);
        const total = segments.reduce((s, x) => s + x.value, 0);
        const legend = segments
            .map(
                (it) =>
                    `<li><span class="dashboard-donut-swatch" style="background:${it.color}"></span>${escapeHtml(it.label)}<span class="dashboard-donut-legend__val">${escapeHtml(String(it.value))}</span></li>`
            )
            .join("");
        const center = escapeHtml(centerText !== undefined && centerText !== null ? centerText : total === 0 ? "—" : String(total));
        return (
            `<div class="dashboard-donut-card">`
            + `<h4 class="dashboard-donut-card__title">${escapeHtml(title)}</h4>`
            + `<div class="dashboard-donut-visual">`
            + `<div class="dashboard-donut-ring">`
            + `<div class="dashboard-donut-fill" style="background:${grad}"></div>`
            + `<div class="dashboard-donut-hole">${center}</div></div>`
            + `<ul class="dashboard-donut-legend">${legend}</ul>`
            + `</div></div>`
        );
    }

    function renderDashboardDonutsContador(container, st, radar) {
        if (!container) return;
        const pendSeg = [
            { label: "Pendente", value: st.PENDENTE || 0, color: DONUT_COLORS.amber },
            { label: "Enviado", value: st.ENVIADO || 0, color: DONUT_COLORS.accent },
            { label: "Validado", value: st.VALIDADO || 0, color: DONUT_COLORS.green },
            { label: "Rejeitado", value: st.REJEITADO || 0, color: DONUT_COLORS.red },
        ];
        const totalPend = pendSeg.reduce((s, x) => s + x.value, 0);
        const cardPend = renderDonutCard("Pendências do mês (status)", pendSeg, String(totalPend));

        const porTipo = (radar && radar.porTipoDocumento) || {};
        const tipoEntries = Object.entries(porTipo).filter(([, n]) => n > 0);
        const palette = [DONUT_COLORS.accent, DONUT_COLORS.green, DONUT_COLORS.amber, DONUT_COLORS.violet, DONUT_COLORS.red, DONUT_COLORS.slate];
        let radarSeg = tipoEntries.map(([k, n], i) => ({
            label: LABEL_DOC_FISCAL[k] || k,
            value: n,
            color: palette[i % palette.length],
        }));
        const totalRadar = radar && typeof radar.totalNotas === "number"
            ? radar.totalNotas
            : tipoEntries.reduce((s, [, n]) => s + n, 0);
        if (radarSeg.length === 0) {
            radarSeg = [{ label: "Nenhuma nota por tipo", value: 0, color: DONUT_COLORS.slate }];
        }
        const cardRadar = renderDonutCard("Notas fiscais (por tipo)", radarSeg, String(totalRadar));

        container.innerHTML = `<div class="dashboard-donuts">${cardPend}${cardRadar}</div>`;
    }

    function renderDashboardDonutsCliente(container, st) {
        if (!container) return;
        const pendSeg = [
            { label: "Pendente", value: st.PENDENTE || 0, color: DONUT_COLORS.amber },
            { label: "Enviado", value: st.ENVIADO || 0, color: DONUT_COLORS.accent },
            { label: "Validado", value: st.VALIDADO || 0, color: DONUT_COLORS.green },
            { label: "Rejeitado", value: st.REJEITADO || 0, color: DONUT_COLORS.red },
        ];
        const totalPend = pendSeg.reduce((s, x) => s + x.value, 0);
        container.innerHTML = `<div class="dashboard-donuts">${renderDonutCard("Suas pendências (status)", pendSeg, String(totalPend))}</div>`;
    }

    function formatarRadarResumo(radar) {
        if (!radar || typeof radar.totalNotas !== "number") return "Indisponível.";
        const parts = [`${radar.totalNotas} nota(s) registrada(s)`];
        const porTipo = radar.porTipoDocumento || {};
        const tipos = Object.entries(porTipo)
            .filter(([, n]) => n > 0)
            .map(([k, n]) => `${LABEL_DOC_FISCAL[k] || k}: ${n}`);
        if (tipos.length) parts.push(tipos.join(" · "));
        const porOp = radar.porOperacao || {};
        const ops = Object.entries(porOp)
            .filter(([, n]) => n > 0)
            .map(([k, n]) => `${LABEL_OP_FISCAL[k] || k}: ${n}`);
        if (ops.length) parts.push(ops.join(" · "));
        return `${parts.join(". ")}.`;
    }

    function renderDashboardKpis(container, cards) {
        if (!container) return;
        container.innerHTML = cards
            .map((c) => {
                const hint = c.hint
                    ? `<br><span class="dashboard-kpi__hint">${escapeHtml(c.hint)}</span>`
                    : "";
                return (
                    `<div class="dashboard-kpi"><span class="dashboard-kpi__value">${escapeHtml(String(c.value))}</span>`
                    + `<span class="dashboard-kpi__label">${escapeHtml(c.label)}${hint}</span></div>`
                );
            })
            .join("");
    }

    async function carregarDashboardContador() {
        const compEl = document.getElementById("dashboard-contador-competencia");
        const kpisEl = document.getElementById("dashboard-contador-kpis");
        const radarEl = document.getElementById("dashboard-contador-radar");
        const fbEl = document.getElementById("dashboard-contador-feedback");
        const donutsEl = document.getElementById("dashboard-contador-donuts");
        if (!kpisEl || !session || session.perfil !== "CONTADOR") return;

        const comp = competenciaAtual();
        if (compEl) compEl.textContent = `${String(comp.mes).padStart(2, "0")}/${comp.ano}`;

        if (donutsEl) {
            donutsEl.innerHTML =
                '<div class="dashboard-donuts-loading"><span class="spinner-360" aria-hidden="true"></span><span class="muted">Carregando visão 360°…</span></div>';
        }

        const errs = [];
        let empresasN = 0;
        let pendList = [];
        let rel = null;
        let radar = null;
        let iaN = 0;

        try {
            const empresas = await fetchJson("/api/empresas", { headers: headers() });
            empresasN = empresas.length;
        } catch {
            errs.push("empresas");
        }

        try {
            // Competências arquivadas (todas validadas) somem da lista padrão; o painel precisa dos totais reais.
            pendList = await fetchJson(
                `/api/pendencias?ano=${comp.ano}&mes=${comp.mes}&incluirArquivadas=true`,
                { headers: headers() }
            );
        } catch {
            errs.push("pendências");
        }

        try {
            rel = await fetchJson("/api/fiscal/relatorios/estrategico", { headers: headers() });
        } catch {
            errs.push("relatório fiscal");
        }

        try {
            radar = await fetchJson("/api/fiscal/radar", { headers: headers() });
        } catch {
            errs.push("radar de notas");
        }

        try {
            const fila = await fetchJson(
                "/api/inteligencia/documentos?somenteRevisar=true&incluirConcluidosNaRevisao=false",
                { headers: headers() }
            );
            iaN = fila.length;
        } catch {
            errs.push("fila IA");
        }

        const st = contarPendenciasPorStatus(pendList);
        const pendHint = `Pendente ${st.PENDENTE} · Enviado ${st.ENVIADO} · Validado ${st.VALIDADO} · Rejeitado ${st.REJEITADO}`;

        renderDashboardDonutsContador(donutsEl, st, radar);

        renderDashboardKpis(kpisEl, [
            { value: empresasN, label: "Empresas cadastradas" },
            { value: pendList.length, label: "Pendências no mês", hint: pendHint },
            { value: iaN, label: "Fila revisão IA" },
            { value: rel ? rel.totalNotasEmitidas : "—", label: "Notas fiscais (total)" },
            { value: rel ? rel.totalAlertasAbertos : "—", label: "Alertas em aberto" },
            { value: rel ? rel.totalCadastrosCpfCnpj : "—", label: "Cadastros CPF/CNPJ" },
            { value: rel ? rel.totalCobrancasGeradas : "—", label: "Cobranças" },
            { value: rel ? rel.totalCertificadosVendidos : "—", label: "Certificados (pedidos)" },
        ]);

        if (radarEl) radarEl.textContent = formatarRadarResumo(radar);

        if (fbEl) {
            if (errs.length) {
                setFeedback(fbEl, `Não foi possível atualizar: ${errs.join(", ")}.`, "err");
                fbEl.classList.remove("hidden");
            } else {
                setFeedback(fbEl, "", null);
                fbEl.classList.add("hidden");
            }
        }
    }

    async function carregarDashboardCliente() {
        const compEl = document.getElementById("dashboard-cliente-competencia");
        const kpisEl = document.getElementById("dashboard-cliente-kpis");
        const fbEl = document.getElementById("dashboard-cliente-feedback");
        const donutsEl = document.getElementById("dashboard-cliente-donuts");
        if (!kpisEl || !session || session.perfil !== "CLIENTE") return;

        const comp = competenciaAtual();
        if (compEl) compEl.textContent = `${String(comp.mes).padStart(2, "0")}/${comp.ano}`;

        if (donutsEl) {
            donutsEl.innerHTML =
                '<div class="dashboard-donuts-loading"><span class="spinner-360" aria-hidden="true"></span><span class="muted">Carregando visão 360°…</span></div>';
        }

        const errs = [];
        let pendList = [];
        try {
            pendList = await fetchJson(
                `/api/pendencias?ano=${comp.ano}&mes=${comp.mes}&incluirArquivadas=true`,
                { headers: headers() }
            );
        } catch {
            errs.push("pendências");
        }

        const st = contarPendenciasPorStatus(pendList);
        const pendHint = `Pendente ${st.PENDENTE} · Enviado ${st.ENVIADO} · Validado ${st.VALIDADO} · Rejeitado ${st.REJEITADO}`;
        const emAberto = st.PENDENTE + st.ENVIADO;

        renderDashboardDonutsCliente(donutsEl, st);

        renderDashboardKpis(kpisEl, [
            { value: pendList.length, label: "Pendências no mês", hint: pendHint },
            { value: emAberto, label: "Em aberto (pendente + enviado)", hint: "Itens a concluir ou em análise" },
        ]);

        if (fbEl) {
            if (errs.length) {
                setFeedback(fbEl, `Não foi possível atualizar: ${errs.join(", ")}.`, "err");
                fbEl.classList.remove("hidden");
            } else {
                setFeedback(fbEl, "", null);
                fbEl.classList.add("hidden");
            }
        }
    }

    function refreshDashboardIfNeeded(screenId) {
        if (!session) return;
        if (screenId === "screen-contador-dashboard" && session.perfil === "CONTADOR") {
            void carregarDashboardContador();
        } else if (screenId === "screen-cliente-dashboard" && session.perfil === "CLIENTE") {
            void carregarDashboardCliente();
        }
    }

    function refreshPendenciasContadorIfNeeded(screenId) {
        if (!session || session.perfil !== "CONTADOR") return;
        if (screenId === "screen-pendencias") {
            void carregarPendenciasContador();
        }
    }

    /** Recarrega a lista do cliente ao entrar na tela (filtro ano/mês atual do formulário). */
    function refreshPendenciasClienteIfNeeded(screenId) {
        if (!session || session.perfil !== "CLIENTE") return;
        if (screenId === "screen-cliente-pendencias") {
            void carregarPendenciasCliente();
        }
    }

    async function carregarIaObservadora() {
        setFeedback(iaObsFeedback, "", null);
        try {
            const data = await fetchJson("/api/ia-observadora/insights", { headers: headers() });
            iaObsResumo.innerHTML = `<strong>Total de eventos (7 dias):</strong> ${data.totalEventosPeriodo}<br>`
                + `<strong>Por perfil:</strong> ${escapeHtml(JSON.stringify(data.acoesPorPerfil || {}))}<br>`
                + `<strong>Por categoria:</strong> ${escapeHtml(JSON.stringify(data.acoesPorCategoria || {}))}<br>`
                + `<strong>Top caminhos:</strong> ${escapeHtml(JSON.stringify(data.topCaminhos || {}))}`;
            const sugs = data.sugestoesAutonomia || [];
            iaObsSugestoes.innerHTML = sugs.length
                ? sugs.map((t) => `<div class="ia-obs-sugestao">${escapeHtml(t)}</div>`).join("")
                : "<span class='muted'>Sem sugestoes ainda.</span>";
            const evs = data.ultimosEventos || [];
            iaObsEventos.innerHTML = evs.length
                ? evs
                    .map(
                        (e) =>
                            `<div class="ia-obs-linha">${escapeHtml(e.criadoEm)} | ${escapeHtml(e.perfil || "-")} | ${escapeHtml(e.metodoHttp)} ${escapeHtml(e.path)} | ${e.statusHttp}</div>`
                    )
                    .join("")
                : "<span class='muted'>Nenhum evento.</span>";
        } catch (e) {
            iaObsResumo.innerHTML = "";
            iaObsSugestoes.innerHTML = "";
            iaObsEventos.innerHTML = "";
            setFeedback(iaObsFeedback, e.message || "Falha ao carregar IA observadora.", "err");
        }
    }

    async function initAfterLogin() {
        setViewByPerfil();
        const comp = competenciaAtual();
        if (session.perfil === "CONTADOR") {
            document.getElementById("nota-data").value = new Date().toISOString().slice(0, 10);
            document.getElementById("pend-ano").value = comp.ano;
            document.getElementById("pend-mes").value = comp.mes;
            await carregarEmpresas();
            await carregarPendenciasContador();
            await carregarFilaIa();
            await carregarRadar();
            await carregarRelatorioEstrategico();
            await carregarIaObservadora();
        } else {
            document.getElementById("cliente-ano").value = comp.ano;
            document.getElementById("cliente-mes").value = comp.mes;
            await carregarPendenciasCliente();
        }
    }

    document.addEventListener("submit", async (ev) => {
        const form = ev.target;
        if (!(form instanceof HTMLFormElement) || form.id !== "form-login") return;
        ev.preventDefault();

        const feedbackEl = document.getElementById("login-feedback") || loginFeedback;
        setFeedback(feedbackEl, "", null);
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
            setFeedback(feedbackEl, e.message || "Falha no login.", "err");
        }
    });

    if (btnUserMenu && userMenuWrap) {
        btnUserMenu.addEventListener("click", (e) => {
            e.stopPropagation();
            toggleUserMenu();
        });
        document.addEventListener("click", (e) => {
            if (!userMenuWrap.contains(e.target)) closeUserMenu();
        });
        document.addEventListener("keydown", (e) => {
            if (e.key === "Escape") closeUserMenu();
        });
    }
    if (btnMinhaConta) {
        btnMinhaConta.addEventListener("click", () => {
            closeUserMenu();
            if (!session) return;
            const initialScreen =
                session.perfil === "CONTADOR" ? "screen-contador-dashboard" : "screen-cliente-dashboard";
            navigateToScreen(initialScreen);
            refreshDocumentosValidadosIfNeeded(initialScreen);
            refreshDashboardIfNeeded(initialScreen);
            refreshPendenciasContadorIfNeeded(initialScreen);
        });
    }

    logoutBtn.addEventListener("click", () => {
        limparSessao();
        setViewByPerfil();
    });

    cnpjInput.addEventListener("input", () => {
        cnpjInput.value = formatarCnpj(cnpjInput.value);
    });
    cpfResponsavelInput.addEventListener("input", () => {
        cpfResponsavelInput.value = formatarCpf(cpfResponsavelInput.value);
    });

    empresaForm.addEventListener("submit", async (ev) => {
        ev.preventDefault();
        setFeedback(empresaFeedback, "", null);
        try {
            const cpfResponsavelDigits = somenteDigitos(document.getElementById("cpfResponsavel").value);
            const payload = {
                cnpj: somenteDigitos(document.getElementById("cnpj").value),
                razaoSocial: document.getElementById("razaoSocial").value.trim(),
                cpfResponsavel: cpfResponsavelDigits || null,
                mei: document.getElementById("mei").checked,
                vencimentoDas: document.getElementById("vencimentoDas").value || null,
                vencimentoCertificadoMei: document.getElementById("vencimentoCertificadoMei").value || null,
            };
            if (empresaEditandoId != null) {
                await fetchJson(`/api/empresas/${empresaEditandoId}`, {
                    method: "PUT",
                    headers: headers({ "Content-Type": "application/json" }),
                    body: JSON.stringify(payload),
                });
                setFeedback(empresaFeedback, "Empresa atualizada.", "ok");
                limparEdicaoEmpresa();
            } else {
                await fetchJson("/api/empresas", {
                    method: "POST",
                    headers: headers({ "Content-Type": "application/json" }),
                    body: JSON.stringify(payload),
                });
                setFeedback(empresaFeedback, "Empresa cadastrada.", "ok");
                empresaForm.reset();
            }
            await carregarEmpresas();
        } catch (e) {
            setFeedback(empresaFeedback, e.message || "Erro ao salvar empresa.", "err");
        }
    });

    if (btnEmpresaCancelar) {
        btnEmpresaCancelar.addEventListener("click", () => {
            limparEdicaoEmpresa();
            setFeedback(empresaFeedback, "", null);
        });
    }

    if (btnEmpresaExcluir) {
        btnEmpresaExcluir.addEventListener("click", () => {
            const id = empresaEditandoId != null ? Number(empresaEditandoId) : NaN;
            excluirEmpresaPorId(id);
        });
    }

    if (empresasBuscaInput) {
        empresasBuscaInput.addEventListener("input", () => {
            renderEmpresasTabelaUI();
        });
        empresasBuscaInput.addEventListener("keydown", (ev) => {
            if (ev.key === "Enter") {
                ev.preventDefault();
                aplicarBuscaEmpresaAoFormulario();
            }
        });
    }

    if (listaEmpresas) {
        listaEmpresas.addEventListener("click", async (ev) => {
            const editBtn = ev.target.closest("[data-empresa-editar]");
            if (editBtn) {
                const raw = editBtn.getAttribute("data-empresa-editar");
                const id = raw ? parseInt(raw, 10) : NaN;
                if (!Number.isFinite(id)) return;
                const e = empresasCache.find((x) => Number(x.id) === id);
                if (!e) return;
                iniciarEdicaoEmpresaNoFormulario(e);
                return;
            }
            const reativarBtn = ev.target.closest("[data-empresa-reativar]");
            if (reativarBtn) {
                ev.preventDefault();
                const raw = reativarBtn.getAttribute("data-empresa-reativar");
                const id = raw ? parseInt(raw, 10) : NaN;
                await reativarEmpresaPorId(id);
                return;
            }
            const delBtn = ev.target.closest("[data-empresa-excluir]");
            if (delBtn) {
                ev.preventDefault();
                const raw = delBtn.getAttribute("data-empresa-excluir");
                const id = raw ? parseInt(raw, 10) : NaN;
                await excluirEmpresaPorId(id);
            }
        });
    }

    if (empresasIncluirInativasEl) {
        empresasIncluirInativasEl.addEventListener("change", () => {
            void carregarEmpresas();
        });
    }

    templateForm.addEventListener("submit", async (ev) => {
        ev.preventDefault();
        setFeedback(templateFeedback, "", null);
        try {
            await fetchJson("/api/templates-documentos", {
                method: "POST",
                headers: headers({ "Content-Type": "application/json" }),
                body: JSON.stringify({
                    empresaId: Number(templateEmpresaEl.value),
                    clientePessoaFisicaId: null,
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

    const btnPendenciasAtualizar = document.getElementById("btn-pendencias-atualizar");
    if (btnPendenciasAtualizar) {
        btnPendenciasAtualizar.addEventListener("click", () => {
            void carregarPendenciasContador();
        });
    }
    const pendMostrarArquivadas = document.getElementById("pend-mostrar-arquivadas");
    if (pendMostrarArquivadas) {
        pendMostrarArquivadas.addEventListener("change", () => {
            void carregarPendenciasContador();
        });
    }

    btnAtualizarIaObs.addEventListener("click", async () => {
        setFeedback(iaObsFeedback, "", null);
        try {
            await carregarIaObservadora();
        } catch (e) {
            setFeedback(iaObsFeedback, e.message || "Erro.", "err");
        }
    });

    btnRecarregarIa.addEventListener("click", async () => {
        setFeedback(iaFeedback, "", null);
        try {
            await carregarFilaIa();
        } catch (e) {
            setFeedback(iaFeedback, e.message || "Erro ao carregar análises IA.", "err");
        }
    });
    const iaIncluirConcluidosEl = document.getElementById("ia-incluir-concluidos");
    if (iaIncluirConcluidosEl) {
        iaIncluirConcluidosEl.addEventListener("change", () => {
            void carregarFilaIa();
        });
    }
    const docsValidadosIncluirArqEl = document.getElementById("docs-validados-incluir-arquivadas");
    if (docsValidadosIncluirArqEl) {
        docsValidadosIncluirArqEl.addEventListener("change", () => {
            void carregarDocumentosValidadosPorAba();
        });
    }

    btnFecharHistorico.addEventListener("click", fecharModalHistorico);
    modalHistorico.addEventListener("click", (ev) => {
        if (ev.target === modalHistorico) fecharModalHistorico();
    });
    btnFecharDadosExtraidos.addEventListener("click", fecharModalDadosExtraidos);
    modalDadosExtraidos.addEventListener("click", (ev) => {
        if (ev.target === modalDadosExtraidos) fecharModalDadosExtraidos();
    });
    btnExportDadosCsv.addEventListener("click", () => exportarDadosExtraidosArquivo("csv"));
    btnExportDadosPdf.addEventListener("click", () => exportarDadosExtraidosArquivo("pdf"));
    btnSalvarDadosExtraidos.addEventListener("click", () => salvarDadosExtraidosEditados());

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

        const fileInput = document.getElementById("upload-arquivo");
        const arquivos = Array.from(fileInput.files || []).filter(Boolean);
        if (!arquivos.length) {
            setFeedback(uploadFeedback, "Selecione pelo menos um arquivo.", "err");
            return;
        }
        if (arquivos.length > 25) {
            setFeedback(uploadFeedback, "No máximo 25 arquivos por envio.", "err");
            return;
        }

        const fd = new FormData();
        const obs = document.getElementById("upload-observacao").value.trim();
        if (obs) fd.append("observacao", obs);
        const umArquivo = arquivos.length === 1;
        if (umArquivo) {
            fd.append("arquivo", arquivos[0]);
        } else {
            arquivos.forEach((f) => fd.append("arquivos", f));
        }

        const uploadUrl = umArquivo
            ? `/api/pendencias/${uploadPendenciaEl.value}/entregas`
            : `/api/pendencias/${uploadPendenciaEl.value}/entregas/lote`;

        try {
            const controller = new AbortController();
            const t = window.setTimeout(() => controller.abort(), umArquivo ? 25000 : 120000);
            const res = await fetch(uploadUrl, {
                method: "POST",
                headers: headers(),
                body: fd,
                signal: controller.signal,
            });
            window.clearTimeout(t);
            const text = await res.text();
            let data = null;
            try { data = text ? JSON.parse(text) : null; } catch {}
            if (!res.ok) throw new Error((data && data.message) || "Erro no upload.");

            uploadForm.reset();
            if (umArquivo) {
                setFeedback(uploadFeedback, "Documento enviado e pendência marcada como ENVIADO.", "ok");
            } else {
                const nOk = data && Array.isArray(data.entregas) ? data.entregas.length : 0;
                const errs = data && Array.isArray(data.erros) ? data.erros : [];
                let msg =
                    (data && data.message) ||
                    (errs.length ? `${nOk} enviado(s); alguns falharam.` : `${nOk} documento(s) enviado(s) e pendência marcada como ENVIADO.`);
                if (errs.length && res.status === 200) {
                    const det = errs
                        .slice(0, 6)
                        .map((e) => `${e.nomeArquivoOriginal}: ${e.mensagem}`)
                        .join(" · ");
                    msg = `${msg} ${det}${errs.length > 6 ? " · …" : ""}`;
                }
                setFeedback(uploadFeedback, msg, errs.length && nOk === 0 ? "err" : "ok");
                if (errs.length && nOk === 0) {
                    await carregarPendenciasCliente();
                    await carregarDocumentosValidadosPorAba();
                    return;
                }
            }
            await carregarPendenciasCliente();
            await carregarDocumentosValidadosPorAba();
        } catch (e) {
            const msg =
                e && e.name === "AbortError"
                    ? "Tempo limite do envio esgotado. Tente menos arquivos ou tamanhos menores."
                    : e.message || "Erro no upload.";
            setFeedback(uploadFeedback, msg, "err");
        }
    });

    btnAtualizarDocsValidados.addEventListener("click", async () => {
        setFeedback(docsValidadosFeedback, "", null);
        try {
            await carregarDocumentosValidadosPorAba();
        } catch (e) {
            setFeedback(docsValidadosFeedback, e.message || "Erro ao atualizar.", "err");
        }
    });

    if (docsValidadosPagerPrev) {
        docsValidadosPagerPrev.addEventListener("click", () => {
            selectDocsValidadosTab(getActiveDocsValidadosTabIndex() - 1);
        });
    }
    if (docsValidadosPagerNext) {
        docsValidadosPagerNext.addEventListener("click", () => {
            selectDocsValidadosTab(getActiveDocsValidadosTabIndex() + 1);
        });
    }

    if (docsValidadosEmpresaEl) {
        docsValidadosEmpresaEl.addEventListener("change", () => {
            carregarDocumentosValidadosPorAba();
        });
    }

    docsValidadosPanels.addEventListener("click", async (ev) => {
        const btn = ev.target.closest(".btn-salvar-doc-validado");
        if (!btn) return;
        const processamentoId = Number(btn.getAttribute("data-processamento-id"));
        const feedback = btn.parentElement.querySelector(".doc-validado-save-feedback");
        if (feedback) {
            feedback.textContent = "";
            feedback.className = "doc-validado-save-feedback feedback";
        }
        const card = btn.closest(".doc-validado-card");
        const inputs = card.querySelectorAll("input.dados-campo-valor-doc");
        const campos = [...inputs].map((el) => ({
            nome: decodeURIComponent(el.getAttribute("data-nome") || ""),
            valor: el.value,
        }));
        try {
            await fetchJson(`/api/inteligencia/documentos/${processamentoId}/campos`, {
                method: "PATCH",
                headers: headers({ "Content-Type": "application/json" }),
                body: JSON.stringify({ campos }),
            });
            if (feedback) {
                feedback.textContent = "Alterações salvas.";
                feedback.classList.add("ok");
            }
            await carregarDocumentosValidadosPorAba();
        } catch (e) {
            if (feedback) {
                feedback.textContent = e.message || "Falha ao salvar.";
                feedback.classList.add("err");
            }
        }
    });

    sidebarItems.forEach((item) => {
        item.addEventListener("click", () => {
            if (!item.classList.contains("hidden")) {
                const target = item.dataset.target;
                navigateToScreen(target);
                refreshDocumentosValidadosIfNeeded(target);
                refreshDashboardIfNeeded(target);
                refreshPendenciasContadorIfNeeded(target);
                refreshPendenciasClienteIfNeeded(target);
            }
        });
    });

    appShell.addEventListener("click", (ev) => {
        const btn = ev.target.closest(".dashboard-goto");
        if (!btn || !btn.dataset.target) return;
        navigateToScreen(btn.dataset.target);
        refreshDocumentosValidadosIfNeeded(btn.dataset.target);
        refreshDashboardIfNeeded(btn.dataset.target);
        refreshPendenciasContadorIfNeeded(btn.dataset.target);
        refreshPendenciasClienteIfNeeded(btn.dataset.target);
    });

    async function carregarRadar() {
        try {
            const radar = await fetchJson("/api/fiscal/radar", { headers: headers() });
            radarJson.textContent = JSON.stringify(radar, null, 2);
        } catch {
            radarJson.textContent = "{}";
        }
    }

    async function baixarDanfeSimulacaoPdf(notaId) {
        const res = await fetch(`/api/fiscal/notas/${notaId}/danfe-simulacao.pdf`, { headers: headers() });
        if (!res.ok) {
            const text = await res.text();
            let msg = res.statusText;
            try {
                const j = JSON.parse(text);
                if (j.message) msg = j.message;
            } catch {
                /* ignore */
            }
            throw new Error(msg);
        }
        const blob = await res.blob();
        const url = URL.createObjectURL(blob);
        const a = document.createElement("a");
        a.href = url;
        a.download = `danfe-simulacao-${notaId}.pdf`;
        a.click();
        URL.revokeObjectURL(url);
    }

    async function carregarRelatorioEstrategico() {
        try {
            const rel = await fetchJson("/api/fiscal/relatorios/estrategico", { headers: headers() });
            relatorioJson.textContent = JSON.stringify(rel, null, 2);
        } catch {
            relatorioJson.textContent = "{}";
        }
    }

    function coletarCamposOpcionaisNfse() {
        const o = {};
        function putString(id, key) {
            const el = document.getElementById(id);
            if (!el) return;
            const v = typeof el.value === "string" ? el.value.trim() : el.value;
            if (v === "" || v == null) return;
            o[key] = v;
        }
        putString("nfse-numero-exibicao", "nfseNumeroExibicao");
        putString("nfse-codigo-verificacao", "nfseCodigoVerificacao");
        putString("nfse-razao-emitente", "nfseRazaoEmitente");
        putString("nfse-razao-tomador", "nfseRazaoTomador");
        putString("nfse-endereco-emitente", "nfseEnderecoEmitente");
        putString("nfse-endereco-tomador", "nfseEnderecoTomador");
        putString("nfse-im-emitente", "nfseInscricaoMunicipalEmitente");
        putString("nfse-im-tomador", "nfseInscricaoMunicipalTomador");
        putString("nfse-discriminacao", "nfseDiscriminacao");
        putString("nfse-email-tomador", "nfseEmailTomador");
        putString("nfse-codigo-servico", "nfseCodigoServicoTexto");
        const ded = document.getElementById("nfse-valor-deducoes");
        if (ded && ded.value !== "" && !Number.isNaN(Number(ded.value))) {
            o.nfseValorDeducoes = Number(ded.value);
        }
        const aliq = document.getElementById("nfse-aliquota-iss");
        if (aliq && aliq.value !== "" && !Number.isNaN(Number(aliq.value))) {
            o.nfseAliquotaIss = Number(aliq.value);
        }
        const iptu = document.getElementById("nfse-credito-iptu");
        if (iptu && iptu.value !== "" && !Number.isNaN(Number(iptu.value))) {
            o.nfseCreditoIptu = Number(iptu.value);
        }
        const ven = document.getElementById("nfse-data-vencimento-iss");
        if (ven && ven.value) {
            o.nfseDataVencimentoIss = ven.value;
        }
        return o;
    }

    function preencherMassaTestePmspNaTela() {
        document.getElementById("nota-emitente").value = "99999999000100";
        document.getElementById("nota-destinatario").value = "99999999000147";
        document.getElementById("nota-tipo").value = "NFSE";
        document.getElementById("nota-valor").value = "10";
        document.getElementById("nota-data").value = "2011-08-03";
        document.getElementById("nota-municipio").value = "São Paulo";
        document.getElementById("nota-uf").value = "SP";
        document.getElementById("nfse-numero-exibicao").value = "00002701";
        document.getElementById("nfse-codigo-verificacao").value = "HXBA-6GMC";
        const raz = "INSCRICAO PARA TESTE NFE - PJ/0001";
        document.getElementById("nfse-razao-emitente").value = raz;
        document.getElementById("nfse-razao-tomador").value = raz;
        const end = "R PEDRO AMERICO 00032, 27 ANDAR - CENTRO - CEP: 01045-010";
        document.getElementById("nfse-endereco-emitente").value = end;
        document.getElementById("nfse-endereco-tomador").value = end;
        document.getElementById("nfse-im-emitente").value = "3.961.999-4";
        document.getElementById("nfse-im-tomador").value = "3.961.999-0";
        document.getElementById("nfse-discriminacao").value = "teste";
        document.getElementById("nfse-email-tomador").value = "-----";
        document.getElementById("nfse-codigo-servico").value =
            "08567 - Centros de emagrecimento, spa e congêneres.";
        document.getElementById("nfse-valor-deducoes").value = "0";
        document.getElementById("nfse-aliquota-iss").value = "5";
        document.getElementById("nfse-credito-iptu").value = "0";
        document.getElementById("nfse-data-vencimento-iss").value = "2011-09-10";
        const det = document.getElementById("nfse-pmsp-details");
        if (det) {
            det.open = true;
        }
        setFeedback(
            notaFeedback,
            "Massa de teste PMSP gerada nos campos. Confira abaixo e clique em Emitir nota.",
            "ok"
        );
        const formNota = document.getElementById("form-fiscal-nota");
        if (formNota && typeof formNota.scrollIntoView === "function") {
            formNota.scrollIntoView({ behavior: "smooth", block: "nearest" });
        }
    }

    const btnMassaPmspTela = document.getElementById("btn-gerar-massa-pmsp-tela");
    if (btnMassaPmspTela) {
        btnMassaPmspTela.addEventListener("click", preencherMassaTestePmspNaTela);
    }
    const btnMassaPmsp = document.getElementById("btn-preencher-massa-pmsp");
    if (btnMassaPmsp) {
        btnMassaPmsp.addEventListener("click", preencherMassaTestePmspNaTela);
    }

    fiscalNotaForm.addEventListener("submit", async (ev) => {
        ev.preventDefault();
        setFeedback(notaFeedback, "", null);
        if (notaPosEmitir) {
            notaPosEmitir.classList.add("hidden");
            notaPosEmitir.innerHTML = "";
        }
        try {
            const baseNota = {
                documentoEmitente: somenteDigitos(document.getElementById("nota-emitente").value),
                documentoDestinatario: somenteDigitos(document.getElementById("nota-destinatario").value),
                tipoDocumento: document.getElementById("nota-tipo").value,
                tipoOperacao: document.getElementById("nota-operacao").value,
                valorTotal: Number(document.getElementById("nota-valor").value),
                dataEmissao: document.getElementById("nota-data").value,
                municipio: document.getElementById("nota-municipio").value.trim(),
                uf: document.getElementById("nota-uf").value.trim().toUpperCase(),
            };
            const nfseOpc = coletarCamposOpcionaisNfse();
            const criada = await fetchJson("/api/fiscal/notas", {
                method: "POST",
                headers: headers({ "Content-Type": "application/json" }),
                body: JSON.stringify({ ...baseNota, ...nfseOpc }),
            });
            fiscalNotaForm.reset();
            document.getElementById("nota-data").value = new Date().toISOString().slice(0, 10);
            setFeedback(notaFeedback, "Nota registrada. Chave/protocolo SEFAZ conforme modo configurado (simulação ou stub real).", "ok");
            if (notaPosEmitir && criada && criada.id != null) {
                const chave = criada.chaveAcesso ? escapeHtml(criada.chaveAcesso) : "—";
                const prot = criada.protocoloAutorizacao ? escapeHtml(criada.protocoloAutorizacao) : "—";
                const modo = criada.sefazModo ? escapeHtml(criada.sefazModo) : "—";
                notaPosEmitir.innerHTML = `<div class="nota-pos-emitir__box">
                    <p class="nota-pos-emitir__titulo">Nota #${criada.id}</p>
                    <p class="muted nota-pos-emitir__linhas">Modo: <strong>${modo}</strong> · Chave (simulada ou retorno): <code>${chave}</code> · Protocolo: <code>${prot}</code></p>
                    <button type="button" class="btn btn--ghost btn-baixar-danfe-sim" data-nota-id="${criada.id}">Baixar PDF (DANFE simulação)</button>
                    <span class="muted nota-pos-emitir__aviso">Documento apenas para teste — sem valor fiscal.</span>
                </div>`;
                notaPosEmitir.classList.remove("hidden");
            }
            await carregarRadar();
        } catch (e) {
            setFeedback(notaFeedback, e.message || "Falha ao emitir nota.", "err");
        }
    });

    if (screenFiscalNotas) {
        screenFiscalNotas.addEventListener("click", async (ev) => {
            const btn = ev.target.closest(".btn-baixar-danfe-sim");
            if (!btn) return;
            const id = btn.getAttribute("data-nota-id");
            if (!id) return;
            setFeedback(notaFeedback, "Gerando PDF...", null);
            try {
                await baixarDanfeSimulacaoPdf(id);
                setFeedback(notaFeedback, "Download do PDF simulado iniciado.", "ok");
            } catch (e) {
                setFeedback(notaFeedback, e.message || "Falha ao baixar PDF.", "err");
            }
        });
    }

    btnRadarNotas.addEventListener("click", async () => {
        setFeedback(radarFeedback, "", null);
        try {
            await carregarRadar();
            setFeedback(radarFeedback, "Radar atualizado.", "ok");
        } catch (e) {
            setFeedback(radarFeedback, e.message || "Erro ao atualizar radar.", "err");
        }
    });

    fiscalCadastroForm.addEventListener("submit", async (ev) => {
        ev.preventDefault();
        setFeedback(cadastroFeedback, "", null);
        try {
            await fetchJson("/api/fiscal/cadastros", {
                method: "POST",
                headers: headers({ "Content-Type": "application/json" }),
                body: JSON.stringify({
                    tipoDocumento: document.getElementById("cadastro-tipo").value,
                    documento: somenteDigitos(document.getElementById("cadastro-documento").value),
                    nome: document.getElementById("cadastro-nome").value.trim(),
                }),
            });
            fiscalCadastroForm.reset();
            setFeedback(cadastroFeedback, "Cadastro fiscal salvo.", "ok");
        } catch (e) {
            setFeedback(cadastroFeedback, e.message || "Erro ao salvar cadastro.", "err");
        }
    });

    fiscalCobrancaForm.addEventListener("submit", async (ev) => {
        ev.preventDefault();
        setFeedback(cobrancaFeedback, "", null);
        try {
            await fetchJson("/api/fiscal/cobrancas", {
                method: "POST",
                headers: headers({ "Content-Type": "application/json" }),
                body: JSON.stringify({
                    documentoPagador: somenteDigitos(document.getElementById("cobranca-documento").value),
                    valor: Number(document.getElementById("cobranca-valor").value),
                    meioPagamento: document.getElementById("cobranca-meio").value,
                    vencimento: document.getElementById("cobranca-vencimento").value,
                    descricao: document.getElementById("cobranca-descricao").value.trim(),
                }),
            });
            fiscalCobrancaForm.reset();
            setFeedback(cobrancaFeedback, "Cobranca gerada.", "ok");
        } catch (e) {
            setFeedback(cobrancaFeedback, e.message || "Erro ao gerar cobranca.", "err");
        }
    });

    const CERT_STATUS_OPTS = [
        "EM_ANALISE",
        "AGUARDANDO_BIOMETRIA",
        "EMITIDO",
        "INSTALADO",
        "APROVADO",
        "ENTREGUE",
        "CANCELADO",
        "RENOVACAO_PENDENTE",
    ];

    async function carregarListaCertificados() {
        if (!features.certificadoDigital) {
            return;
        }
        const tbody = document.getElementById("cert-tbody");
        const vazio = document.getElementById("cert-lista-vazio");
        const tab = document.getElementById("cert-tabela");
        if (!tbody) {
            return;
        }
        try {
            const list = await fetchJson("/api/fiscal/certificados", { headers: headers() });
            tbody.innerHTML = "";
            if (!list.length) {
                if (vazio) {
                    vazio.classList.remove("hidden");
                }
                if (tab) {
                    tab.classList.add("hidden");
                }
                return;
            }
            if (vazio) {
                vazio.classList.add("hidden");
            }
            if (tab) {
                tab.classList.remove("hidden");
            }
            list.forEach((row) => {
                const tr = document.createElement("tr");
                const empNome = row.empresa && row.empresa.razaoSocial ? row.empresa.razaoSocial : "—";
                const venc = row.dataVencimentoPrevista || "—";
                let criado = "—";
                if (row.criadoEm) {
                    criado =
                        typeof row.criadoEm === "string"
                            ? row.criadoEm.slice(0, 10)
                            : String(row.criadoEm);
                }
                const sel = document.createElement("select");
                sel.className = "cert-status-select";
                sel.setAttribute("aria-label", "Status do pedido");
                CERT_STATUS_OPTS.forEach((s) => {
                    const o = document.createElement("option");
                    o.value = s;
                    o.textContent = s;
                    if (row.status === s) {
                        o.selected = true;
                    }
                    sel.appendChild(o);
                });
                const id = Number(row.id);
                sel.addEventListener("change", async () => {
                    try {
                        await fetchJson(`/api/fiscal/certificados/${id}`, {
                            method: "PATCH",
                            headers: headers({ "Content-Type": "application/json" }),
                            body: JSON.stringify({ status: sel.value }),
                        });
                        setFeedback(certFeedback, "Status atualizado.", "ok");
                    } catch (e) {
                        setFeedback(certFeedback, e.message || "Erro ao atualizar status.", "err");
                        void carregarListaCertificados();
                    }
                });
                const td0 = document.createElement("td");
                td0.textContent = criado;
                const td1 = document.createElement("td");
                td1.textContent = row.titular || "";
                const td2 = document.createElement("td");
                td2.textContent = empNome;
                const td3 = document.createElement("td");
                td3.textContent = row.tipoCertificado || "";
                const td4 = document.createElement("td");
                td4.textContent = venc;
                const td5 = document.createElement("td");
                td5.appendChild(sel);
                tr.append(td0, td1, td2, td3, td4, td5);
                tbody.appendChild(tr);
            });
        } catch (e) {
            tbody.innerHTML = "";
            if (vazio) {
                vazio.classList.remove("hidden");
            }
            if (tab) {
                tab.classList.add("hidden");
            }
        }
    }

    if (fiscalCertificadoForm) {
        fiscalCertificadoForm.addEventListener("submit", async (ev) => {
            ev.preventDefault();
            if (!features.certificadoDigital) {
                return;
            }
            setFeedback(certFeedback, "", null);
            try {
                const empSel = document.getElementById("cert-empresa-id");
                const empresaId = empSel && empSel.value ? Number(empSel.value) : null;
                const venc = document.getElementById("cert-venc-previsto");
                const obs = document.getElementById("cert-obs");
                const payload = {
                    documentoSolicitante: somenteDigitos(document.getElementById("cert-doc").value),
                    titular: document.getElementById("cert-titular").value.trim(),
                    emailContato: document.getElementById("cert-email").value.trim(),
                    tipoCertificado: document.getElementById("cert-tipo").value.trim(),
                    validadeMeses: Number(document.getElementById("cert-validade").value),
                };
                if (empresaId && Number.isFinite(empresaId)) {
                    payload.empresaId = empresaId;
                }
                if (venc && venc.value) {
                    payload.dataVencimentoPrevista = venc.value;
                }
                if (obs && obs.value.trim()) {
                    payload.observacaoInterna = obs.value.trim();
                }
                await fetchJson("/api/fiscal/certificados", {
                    method: "POST",
                    headers: headers({ "Content-Type": "application/json" }),
                    body: JSON.stringify(payload),
                });
                fiscalCertificadoForm.reset();
                setFeedback(certFeedback, "Pedido de certificado registrado.", "ok");
                await carregarListaCertificados();
            } catch (e) {
                setFeedback(certFeedback, e.message || "Erro ao registrar certificado.", "err");
            }
        });
    }

    btnGerarAlertas.addEventListener("click", async () => {
        setFeedback(alertaFeedback, "", null);
        try {
            const res = await fetchJson("/api/fiscal/alertas/automaticos", {
                method: "POST",
                headers: headers(),
            });
            setFeedback(alertaFeedback, `${res.alertasCriados || 0} alerta(s) criado(s).`, "ok");
            await carregarRelatorioEstrategico();
        } catch (e) {
            setFeedback(alertaFeedback, e.message || "Erro ao gerar alertas.", "err");
        }
    });

    btnRelatorioEstrategico.addEventListener("click", async () => {
        setFeedback(alertaFeedback, "", null);
        try {
            await carregarRelatorioEstrategico();
            setFeedback(alertaFeedback, "Relatorio atualizado.", "ok");
        } catch (e) {
            setFeedback(alertaFeedback, e.message || "Erro ao atualizar relatorio.", "err");
        }
    });

    btnPrefeituras.addEventListener("click", async () => {
        setFeedback(alertaFeedback, "", null);
        try {
            const data = await fetchJson("/api/fiscal/prefeituras/compatibilidade", { headers: headers() });
            relatorioJson.textContent = JSON.stringify(data, null, 2);
            setFeedback(alertaFeedback, "Compatibilidade carregada.", "ok");
        } catch (e) {
            setFeedback(alertaFeedback, e.message || "Erro ao consultar prefeituras.", "err");
        }
    });

    (async function bootstrap() {
        await loadFeatures();
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

    if ("serviceWorker" in navigator) {
        const localHost =
            location.hostname === "localhost" ||
            location.hostname === "127.0.0.1" ||
            location.hostname === "[::1]";
        if (location.protocol === "https:" || localHost) {
            window.addEventListener("load", () => {
                navigator.serviceWorker.register("/sw.js").catch(() => {});
            });
        }
    }
})();
