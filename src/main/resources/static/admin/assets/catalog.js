(() => {
    "use strict";

    const toggleSelector = "[data-tree-toggle]";

    function controlledChildren(toggle) {
        const item = toggle.closest?.(".hierarchy-item");
        return item?.querySelector?.(":scope > .tree-children") ?? null;
    }

    function setExpanded(toggle, expanded) {
        const children = controlledChildren(toggle);
        const symbol = toggle.querySelector("[data-tree-toggle-symbol]");
        const nodeLabel = toggle.dataset.nodeLabel || "Hierarchieast";

        toggle.setAttribute("aria-expanded", String(expanded));
        toggle.setAttribute("aria-label", `${nodeLabel}: Kinder ${expanded ? "ausblenden" : "einblenden"}`);
        toggle.classList.toggle("is-expanded", expanded);

        if (symbol) {
            symbol.textContent = expanded ? "−" : "+";
        }
        if (children) {
            children.hidden = !expanded;
        }
    }

    function toggleFromEvent(event) {
        const toggle = event.target.closest?.(toggleSelector);
        if (!toggle) {
            return;
        }

        const expanding = toggle.getAttribute("aria-expanded") !== "true";
        const children = controlledChildren(toggle);
        setExpanded(toggle, expanding);

        if (expanding
                && children
                && children.dataset.loaded !== "true"
                && !toggle.classList.contains("is-loading")) {
            window.htmx?.trigger(toggle, "tree-load");
        }
    }

    function requestToggle(event) {
        const element = event.detail?.elt;
        return element?.matches?.(toggleSelector) ? element : null;
    }

    document.addEventListener("click", toggleFromEvent);

    document.body.addEventListener("htmx:beforeRequest", (event) => {
        const toggle = requestToggle(event);
        if (!toggle) {
            return;
        }

        const children = controlledChildren(toggle);
        toggle.classList.add("is-loading");
        toggle.setAttribute("aria-busy", "true");
        toggle.disabled = true;
        if (children) {
            children.hidden = false;
            children.classList.add("is-loading");
            children.setAttribute("aria-busy", "true");
        }
    });

    document.body.addEventListener("htmx:afterSwap", (event) => {
        const target = event.detail?.target;
        if (target?.classList?.contains("tree-children")) {
            target.dataset.loaded = "true";
        }
    });

    document.body.addEventListener("htmx:afterRequest", (event) => {
        const toggle = requestToggle(event);
        if (!toggle) {
            return;
        }

        const children = controlledChildren(toggle);
        toggle.classList.remove("is-loading");
        toggle.removeAttribute("aria-busy");
        toggle.disabled = false;

        if (children) {
            children.classList.remove("is-loading");
            children.removeAttribute("aria-busy");
        }

        if (event.detail?.failed && children) {
            children.dataset.loaded = "false";
            const message = document.createElement("p");
            message.className = "tree-load-error";
            message.textContent = "Kinder konnten nicht geladen werden. Schließe und öffne den Ast für einen neuen Versuch.";
            children.replaceChildren(message);
        }
    });

    let pendingNavigation = null;

    function activeForm() {
        return document.querySelector("[data-catalog-concept-form]");
    }

    function dialogFor(selector) {
        return document.querySelector(selector);
    }

    function showDialog(dialog) {
        if (!dialog) {
            return;
        }
        if (typeof dialog.showModal === "function") {
            dialog.showModal();
        } else {
            dialog.setAttribute("open", "open");
        }
    }

    function closeDialog(dialog) {
        if (!dialog) {
            return;
        }
        if (typeof dialog.close === "function") {
            dialog.close();
        } else {
            dialog.removeAttribute("open");
        }
    }

    function markDirty(event) {
        const form = event.target.closest?.("[data-catalog-concept-form]");
        if (form) {
            form.dataset.dirty = "true";
        }
    }

    function suggestCode(event) {
        const displayName = event.target.closest?.("[data-display-name]");
        if (!displayName) {
            return;
        }
        const form = displayName.closest("[data-catalog-concept-form]");
        const code = form?.querySelector("[data-code-from-display-name]");
        if (!code || code.dataset.edited === "true") {
            return;
        }
        code.value = displayName.value
            .normalize("NFD")
            .replace(/[\u0300-\u036f]/g, "")
            .toUpperCase()
            .replace(/[^A-Z0-9]+/g, "_")
            .replace(/^_+|_+$/g, "")
            .replace(/^[^A-Z]+/, "");
    }

    document.addEventListener("input", (event) => {
        markDirty(event);
        suggestCode(event);
        if (event.target.matches?.("[data-code-from-display-name]")) {
            event.target.dataset.edited = "true";
        }
    });
    document.addEventListener("change", markDirty);

    document.addEventListener("submit", (event) => {
        const form = event.target.matches?.("[data-catalog-concept-form]") ? event.target : null;
        if (!form) {
            return;
        }
        const active = form.querySelector("input[name='active']");
        if (form.dataset.originalActive === "true" && active && !active.checked && form.dataset.deactivationConfirmed !== "true") {
            event.preventDefault();
            showDialog(dialogFor("[data-deactivation-dialog]"));
            return;
        }
        form.dataset.dirty = "false";
        const saveButton = form.querySelector("[data-save-button]");
        if (saveButton) {
            saveButton.disabled = true;
            saveButton.textContent = "Speichert …";
        }
    });

    document.addEventListener("click", (event) => {
        const confirm = event.target.closest?.("[data-deactivation-confirm]");
        if (confirm) {
            const form = activeForm();
            if (form) {
                form.dataset.deactivationConfirmed = "true";
                closeDialog(dialogFor("[data-deactivation-dialog]"));
                form.requestSubmit();
            }
            return;
        }
        if (event.target.closest?.("[data-deactivation-cancel]")) {
            closeDialog(dialogFor("[data-deactivation-dialog]"));
            return;
        }
        if (event.target.closest?.("[data-dirty-cancel]")) {
            pendingNavigation = null;
            closeDialog(dialogFor("[data-dirty-dialog]"));
            return;
        }
        if (event.target.closest?.("[data-dirty-discard]")) {
            const target = pendingNavigation;
            pendingNavigation = null;
            closeDialog(dialogFor("[data-dirty-dialog]"));
            if (target) {
                window.location.assign(target);
            }
        }
    });

    document.addEventListener("click", (event) => {
        const link = event.target.closest?.("a[href]");
        const form = activeForm();
        if (!link || !form || form.dataset.dirty !== "true" || link.matches("[data-discard-form]")) {
            return;
        }
        if (link.origin !== window.location.origin || link.href === window.location.href) {
            return;
        }
        event.preventDefault();
        event.stopImmediatePropagation();
        pendingNavigation = link.href;
        showDialog(dialogFor("[data-dirty-dialog]"));
    }, true);

    document.addEventListener("keydown", (event) => {
        if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === "s") {
            const form = activeForm();
            if (form) {
                event.preventDefault();
                form.requestSubmit();
            }
        }
    });

    window.addEventListener("beforeunload", (event) => {
        if (activeForm()?.dataset.dirty === "true") {
            event.preventDefault();
            event.returnValue = "";
        }
    });
})();
