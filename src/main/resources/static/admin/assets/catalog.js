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
            const targetName = event.target?.name;
            if (targetName
                    && targetName !== "weightWarningsAcknowledged"
                    && targetName !== "inactiveRelationsAcknowledged") {
                const acknowledgement = form.querySelector("input[name='weightWarningsAcknowledged']");
                if (acknowledgement) {
                    acknowledgement.checked = false;
                }
            }
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

    function discardChangesAndNavigate() {
        const form = activeForm();
        if (form) {
            form.dataset.dirty = "false";
        }
        const navigation = pendingNavigation;
        pendingNavigation = null;
        closeDialog(dialogFor("[data-dirty-dialog]"));
        if (!navigation) {
            return;
        }
        if (navigation.kind === "url") {
            window.location.assign(navigation.target);
            return;
        }
        if (navigation.kind === "form") {
            if (navigation.submitter) {
                navigation.form.requestSubmit(navigation.submitter);
            } else {
                navigation.form.requestSubmit();
            }
        }
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
        const submittedForm = event.target.matches?.("form") ? event.target : null;
        const editingForm = activeForm();
        if (submittedForm
                && !submittedForm.matches("[data-catalog-concept-form]")
                && editingForm?.dataset.dirty === "true") {
            event.preventDefault();
            event.stopImmediatePropagation();
            pendingNavigation = {
                kind: "form",
                form: submittedForm,
                submitter: event.submitter ?? null
            };
            showDialog(dialogFor("[data-dirty-dialog]"));
            return;
        }

        const form = submittedForm?.matches("[data-catalog-concept-form]") ? submittedForm : null;
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
    }, true);

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
            discardChangesAndNavigate();
        }
    });

    document.addEventListener("click", (event) => {
        const link = event.target.closest?.("a[href]");
        const form = activeForm();
        if (!link || !form || form.dataset.dirty !== "true") {
            return;
        }
        if (link.matches("[data-discard-form]")) {
            form.dataset.dirty = "false";
            return;
        }
        if (link.origin !== window.location.origin || link.href === window.location.href) {
            return;
        }
        event.preventDefault();
        event.stopImmediatePropagation();
        pendingNavigation = {kind: "url", target: link.href};
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

    function refinementEditor() {
        return document.querySelector("[data-refinement-editor]");
    }

    function refinementChangeSelector(parentId, childId) {
        return `[data-pending-refinement][data-parent-id="${parentId}"][data-child-id="${childId}"]`;
    }

    function changeInput(editor, type, parentId, childId, relatedVersion) {
        const changes = editor.querySelector("[data-pending-refinements]");
        if (!changes) {
            return null;
        }
        const existing = changes.querySelector(refinementChangeSelector(parentId, childId));
        if (existing) {
            existing.remove();
        }
        const input = document.createElement("input");
        input.type = "hidden";
        input.name = "relationChange";
        input.value = `${type}:${parentId}:${childId}:${relatedVersion}`;
        input.dataset.pendingRefinement = "true";
        input.dataset.parentId = String(parentId);
        input.dataset.childId = String(childId);
        input.dataset.changeType = type;
        input.dataset.relatedVersion = String(relatedVersion);
        changes.append(input);
        return input;
    }

    function removeChange(editor, parentId, childId) {
        editor.querySelector(refinementChangeSelector(parentId, childId))?.remove();
    }

    function relationEntry(editor, parentId, childId) {
        return editor.querySelector(`[data-relation-entry][data-parent-id="${parentId}"][data-child-id="${childId}"]`);
    }

    function setRelationDirty(editor) {
        const form = editor.closest("[data-catalog-concept-form]");
        if (!form) {
            return;
        }
        form.dataset.dirty = "true";
        ["inactiveRelationsAcknowledged", "weightWarningsAcknowledged"].forEach((name) => {
            const acknowledgement = form.querySelector(`input[name='${name}']`);
            if (acknowledgement) {
                acknowledgement.checked = false;
            }
        });
    }

    function addPendingRelation(editor, button) {
        const currentId = editor.dataset.conceptId;
        const direction = button.dataset.direction;
        const candidateId = button.dataset.candidateId;
        if (!currentId || !candidateId || !direction) {
            return;
        }
        const parentId = direction === "PARENT" ? candidateId : currentId;
        const childId = direction === "PARENT" ? currentId : candidateId;
        if (relationEntry(editor, parentId, childId)) {
            return;
        }
        const list = editor.querySelector(`[data-relation-list="${direction}"]`);
        if (!list) {
            return;
        }
        list.querySelector("[data-empty-relations]")?.remove();
        changeInput(editor, "ADD", parentId, childId, button.dataset.candidateVersion || "0");
        const item = document.createElement("li");
        item.className = "pending-relation";
        item.dataset.relationEntry = "true";
        item.dataset.parentId = parentId;
        item.dataset.childId = childId;
        item.dataset.relatedVersion = button.dataset.candidateVersion || "0";
        item.dataset.original = "false";
        const label = document.createElement("span");
        label.textContent = `${button.dataset.candidateName || "Konzept"} `;
        const code = document.createElement("code");
        code.textContent = button.dataset.candidateCode || "";
        label.append(code);
        const marker = document.createElement("small");
        marker.textContent = button.dataset.candidateActive === "true" ? " vorgemerkt" : " inaktiv · vorgemerkt";
        label.append(marker);
        const remove = document.createElement("button");
        remove.type = "button";
        remove.className = "button button--secondary";
        remove.dataset.removeRelation = "true";
        remove.textContent = "Vormerkung zurücknehmen";
        item.append(label, remove);
        list.append(item);
        setRelationDirty(editor);
    }

    function toggleExistingRemoval(editor, entry) {
        const parentId = entry.dataset.parentId;
        const childId = entry.dataset.childId;
        const removed = entry.dataset.pendingRemoval === "true";
        if (removed) {
            entry.dataset.pendingRemoval = "false";
            entry.classList.remove("pending-removal");
            entry.querySelector("[data-remove-relation]").textContent = "Entfernen";
            removeChange(editor, parentId, childId);
        } else {
            entry.dataset.pendingRemoval = "true";
            entry.classList.add("pending-removal");
            entry.querySelector("[data-remove-relation]").textContent = "Entfernen zurücknehmen";
            changeInput(editor, "REMOVE", parentId, childId, entry.dataset.relatedVersion || "0");
        }
        setRelationDirty(editor);
    }

    function requestPickerResults(editor, search) {
        const picker = editor.querySelector("[data-relation-picker]");
        const results = editor.querySelector("[data-relation-picker-results]");
        const url = picker?.dataset.pickerUrl;
        if (!url || !results || !window.htmx) {
            return;
        }
        window.htmx.ajax("GET", `${url}&q=${encodeURIComponent(search || "")}`, {target: results, swap: "innerHTML"});
    }

    function restorePendingRefinements() {
        const editor = refinementEditor();
        if (!editor) {
            return;
        }
        const pending = editor.querySelectorAll("[data-pending-refinement]");
        if (pending.length > 0) {
            const form = editor.closest("[data-catalog-concept-form]");
            if (form) {
                form.dataset.dirty = "true";
            }
        }
        pending.forEach((input) => {
            const parentId = input.dataset.parentId;
            const childId = input.dataset.childId;
            const existing = relationEntry(editor, parentId, childId);
            if (input.dataset.changeType === "REMOVE" && existing) {
                existing.dataset.pendingRemoval = "true";
                existing.classList.add("pending-removal");
                const button = existing.querySelector("[data-remove-relation]");
                if (button) {
                    button.textContent = "Entfernen zurücknehmen";
                }
            }
            if (input.dataset.changeType === "ADD" && !existing) {
                const direction = parentId === editor.dataset.conceptId ? "CHILD" : "PARENT";
                const list = editor.querySelector(`[data-relation-list="${direction}"]`);
                if (!list) {
                    return;
                }
                list.querySelector("[data-empty-relations]")?.remove();
                const item = document.createElement("li");
                item.className = "pending-relation";
                item.dataset.relationEntry = "true";
                item.dataset.parentId = parentId;
                item.dataset.childId = childId;
                item.dataset.relatedVersion = input.dataset.relatedVersion || "0";
                item.dataset.original = "false";
                const label = document.createElement("span");
                label.textContent = `Vorgemerkte Beziehung zu Konzept #${parentId === editor.dataset.conceptId ? childId : parentId} `;
                const marker = document.createElement("small");
                marker.textContent = "vorgemerkt";
                label.append(marker);
                const remove = document.createElement("button");
                remove.type = "button";
                remove.className = "button button--secondary";
                remove.dataset.removeRelation = "true";
                remove.textContent = "Vormerkung zurücknehmen";
                item.append(label, remove);
                list.append(item);
            }
        });
    }

    let pickerTimer = null;

    document.addEventListener("click", (event) => {
        const resetSeasonality = event.target.closest?.("[data-reset-seasonality]");
        if (resetSeasonality) {
            const input = resetSeasonality.closest(".season-field")?.querySelector("input");
            if (input) {
                input.value = "1.0";
                input.dispatchEvent(new Event("input", {bubbles: true}));
            }
            return;
        }
        const pickerButton = event.target.closest?.("[data-open-relation-picker]");
        if (pickerButton) {
            const editor = refinementEditor();
            const picker = editor?.querySelector("[data-relation-picker]");
            if (!editor || !picker) {
                return;
            }
            picker.hidden = false;
            picker.dataset.pickerUrl = pickerButton.dataset.pickerUrl;
            const input = picker.querySelector("[data-relation-picker-search]");
            input?.focus();
            requestPickerResults(editor, input?.value || "");
            return;
        }
        const add = event.target.closest?.("[data-add-relation]");
        if (add && !add.disabled) {
            const editor = refinementEditor();
            if (editor) {
                addPendingRelation(editor, add);
            }
            return;
        }
        const remove = event.target.closest?.("[data-remove-relation]");
        if (remove) {
            const editor = refinementEditor();
            const entry = remove.closest?.("[data-relation-entry]");
            if (!editor || !entry) {
                return;
            }
            if (entry.dataset.original === "true") {
                toggleExistingRemoval(editor, entry);
            } else {
                removeChange(editor, entry.dataset.parentId, entry.dataset.childId);
                entry.remove();
                setRelationDirty(editor);
            }
        }
    });

    document.addEventListener("input", (event) => {
        const search = event.target.closest?.("[data-relation-picker-search]");
        const editor = refinementEditor();
        if (!search || !editor) {
            return;
        }
        window.clearTimeout(pickerTimer);
        pickerTimer = window.setTimeout(() => requestPickerResults(editor, search.value), 180);
    });

    document.body.addEventListener("htmx:afterSwap", restorePendingRefinements);
    restorePendingRefinements();

    window.addEventListener("beforeunload", (event) => {
        if (activeForm()?.dataset.dirty === "true") {
            event.preventDefault();
            event.returnValue = "";
        }
    });
})();

(() => {
    "use strict";

    function exclusionEditor() {
        return document.querySelector("[data-exclusion-target-list]")?.closest("form");
    }

    function requestExclusionPicker(query) {
        const form = exclusionEditor();
        const picker = form?.querySelector("[data-exclusion-picker]");
        const results = form?.querySelector("[data-exclusion-picker-results]");
        if (!picker?.dataset.pickerUrl || !results || !window.htmx) {
            return;
        }
        window.htmx.ajax("GET", `${picker.dataset.pickerUrl}?q=${encodeURIComponent(query || "")}`,
            {target: results, swap: "innerHTML"});
    }

    function updateTargetValue(entry) {
        const hidden = entry.querySelector("[data-exclusion-target-value]");
        const refinements = entry.querySelector("[data-exclusion-refinements]");
        if (hidden) {
            hidden.value = `${entry.dataset.targetId}:${refinements?.checked === true}`;
        }
    }

    function appendTarget(button) {
        const form = exclusionEditor();
        const list = form?.querySelector("[data-exclusion-target-list]");
        const id = button.dataset.candidateId;
        if (!form || !list || !id || list.querySelector(`[data-target-id="${id}"]`)) {
            return;
        }
        list.querySelector("[data-empty-exclusion-targets]")?.remove();
        const entry = document.createElement("li");
        entry.dataset.exclusionTargetEntry = "true";
        entry.dataset.targetId = id;
        const hidden = document.createElement("input");
        hidden.type = "hidden";
        hidden.name = "target";
        hidden.value = `${id}:false`;
        hidden.dataset.exclusionTargetValue = "true";
        const text = document.createElement("span");
        const name = document.createElement("strong");
        name.textContent = button.dataset.candidateName || "Zutat";
        const code = document.createElement("code");
        code.textContent = button.dataset.candidateCode || "";
        const status = document.createElement("small");
        status.textContent = button.dataset.candidateActive === "true" ? " aktiv" : " inaktiv";
        text.append(name, document.createTextNode(" "), code, status);
        const label = document.createElement("label");
        const checkbox = document.createElement("input");
        checkbox.type = "checkbox";
        checkbox.dataset.exclusionRefinements = "true";
        label.append(checkbox, document.createTextNode(" bekannte Konkretisierungen dieses Ziels mit ausschließen"));
        const remove = document.createElement("button");
        remove.type = "button";
        remove.className = "button button--secondary";
        remove.dataset.removeExclusionTarget = "true";
        remove.textContent = "Entfernen";
        entry.append(hidden, text, label, remove);
        list.append(entry);
        form.dataset.dirty = "true";
    }

    let pickerTimer = null;
    document.addEventListener("click", (event) => {
        if (event.target.closest?.("[data-open-exclusion-picker]")) {
            const form = exclusionEditor();
            const picker = form?.querySelector("[data-exclusion-picker]");
            if (!picker) return;
            picker.hidden = false;
            const input = picker.querySelector("[data-exclusion-picker-search]");
            input?.focus();
            requestExclusionPicker(input?.value || "");
            return;
        }
        const add = event.target.closest?.("[data-add-exclusion-target]");
        if (add) {
            appendTarget(add);
            return;
        }
        const remove = event.target.closest?.("[data-remove-exclusion-target]");
        if (remove) {
            const entry = remove.closest("[data-exclusion-target-entry]");
            const form = exclusionEditor();
            entry?.remove();
            if (form) form.dataset.dirty = "true";
        }
    });
    document.addEventListener("change", (event) => {
        const checkbox = event.target.closest?.("[data-exclusion-refinements]");
        if (!checkbox) return;
        updateTargetValue(checkbox.closest("[data-exclusion-target-entry]"));
    });
    document.addEventListener("input", (event) => {
        const input = event.target.closest?.("[data-exclusion-picker-search]");
        if (!input) return;
        window.clearTimeout(pickerTimer);
        pickerTimer = window.setTimeout(() => requestExclusionPicker(input.value), 180);
    });
})();

(() => {
    "use strict";

    function selectedConceptIdFromLocation() {
        const selected = new URLSearchParams(window.location.search).get("selected");
        if (selected && /^\d+$/.test(selected)) {
            return selected;
        }
        const match = window.location.pathname.match(/^\/admin\/catalog\/(\d+)(?:\/edit)?$/);
        return match?.[1] ?? null;
    }

    function conceptIdFromLink(link) {
        if (!link) {
            return null;
        }
        const match = link.pathname.match(/^\/admin\/catalog\/(\d+)(?:\/edit)?$/);
        return match?.[1] ?? null;
    }

    let currentCatalogSelectionId = selectedConceptIdFromLocation();

    function synchronizeCatalogSelection(selectedConceptId) {
        const selected = selectedConceptId == null ? null : String(selectedConceptId);
        document.querySelectorAll(".tree-node").forEach((node) => {
            const link = node.querySelector(":scope > .tree-node-link");
            node.classList.toggle("is-selected", selected !== null && conceptIdFromLink(link) === selected);
        });
        document.querySelectorAll(".catalog-list > li").forEach((item) => {
            const link = item.querySelector(".list-entry-heading > a");
            item.classList.toggle("selected", selected !== null && conceptIdFromLink(link) === selected);
        });
    }

    function updateCatalogFeedback(selectionOutsideResults) {
        document.querySelectorAll(".save-notice").forEach((notice) => notice.remove());

        const detailColumn = document.querySelector(".catalog-detail-column");
        if (!detailColumn) {
            return;
        }
        let outsideNotice = detailColumn.querySelector(":scope > .notice");
        if (!selectionOutsideResults) {
            outsideNotice?.remove();
            return;
        }
        if (outsideNotice) {
            return;
        }

        outsideNotice = document.createElement("p");
        outsideNotice.className = "notice";
        outsideNotice.textContent = "Das ausgewählte Konzept liegt außerhalb der aktuellen Treffer.";
        const detail = detailColumn.querySelector(":scope > #catalog-detail");
        if (detail) {
            detailColumn.insertBefore(outsideNotice, detail);
        } else {
            detailColumn.append(outsideNotice);
        }
    }

    document.body.addEventListener("catalogSelectionState", (event) => {
        const state = event.detail ?? {};
        updateCatalogFeedback(state.selectionOutsideResults === true);
        if (state.selectedConceptId !== undefined && state.selectedConceptId !== null) {
            currentCatalogSelectionId = String(state.selectedConceptId);
            synchronizeCatalogSelection(currentCatalogSelectionId);
        }
    });

    document.body.addEventListener("htmx:afterSwap", (event) => {
        if (event.detail?.target?.classList?.contains("tree-children") && currentCatalogSelectionId !== null) {
            synchronizeCatalogSelection(currentCatalogSelectionId);
        }
    });

    if (currentCatalogSelectionId !== null) {
        synchronizeCatalogSelection(currentCatalogSelectionId);
    }
})();
