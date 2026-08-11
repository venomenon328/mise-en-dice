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
})();
