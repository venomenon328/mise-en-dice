(() => {
    "use strict";

    const toggleSelector = "[data-tree-toggle]";
    const expandedTreeStorageKey = "mise-en-dice.catalog.expanded-tree-occurrences.v1";

    function topLevelTreeScope(toggle) {
        const scope = toggle.closest?.("#hierarchy-roots, [id^='tree-focus-']");
        return scope?.id ?? null;
    }

    function treeStateKey(toggle) {
        if (toggle.dataset.treeStateKey) {
            return toggle.dataset.treeStateKey;
        }

        const nodeId = toggle.dataset.nodeId;
        if (!nodeId) {
            return null;
        }

        const parentChildren = toggle.closest?.(".tree-children");
        const parentItem = parentChildren?.closest?.(".hierarchy-item");
        const parentToggle = parentItem?.querySelector?.(":scope > .tree-node > [data-tree-toggle]");
        const parentKey = parentToggle ? treeStateKey(parentToggle) : topLevelTreeScope(toggle);
        if (!parentKey) {
            return null;
        }

        const key = `${parentKey}/${nodeId}`;
        toggle.dataset.treeStateKey = key;
        return key;
    }

    function loadExpandedTreeKeys() {
        try {
            const stored = JSON.parse(window.sessionStorage.getItem(expandedTreeStorageKey) ?? "[]");
            return new Set(Array.isArray(stored)
                ? stored.filter((value) => typeof value === "string")
                : []);
        } catch {
            return new Set();
        }
    }

    const expandedTreeKeys = loadExpandedTreeKeys();

    function persistExpandedTreeKeys() {
        try {
            window.sessionStorage.setItem(expandedTreeStorageKey, JSON.stringify([...expandedTreeKeys]));
        } catch {
            // Tree-state preservation is progressive enhancement; normal navigation must keep working.
        }
    }

    function rememberToggleState(toggle) {
        const key = treeStateKey(toggle);
        if (!key) {
            return;
        }

        if (toggle.getAttribute("aria-expanded") === "true") {
            expandedTreeKeys.add(key);
        } else {
            expandedTreeKeys.delete(key);
        }
        persistExpandedTreeKeys();
    }

    function restoreExpandedTreeState(root) {
        if (!root) {
            return;
        }

        const toggles = root.matches?.(toggleSelector)
            ? [root]
            : Array.from(root.querySelectorAll?.(toggleSelector) ?? []);
        toggles.forEach((toggle) => {
            const key = treeStateKey(toggle);
            if (key
                    && expandedTreeKeys.has(key)
                    && toggle.getAttribute("aria-expanded") !== "true"
                    && !toggle.disabled) {
                toggle.click();
            }
        });
    }

    document.addEventListener("click", (event) => {
        const toggle = event.target.closest?.(toggleSelector);
        if (toggle) {
            rememberToggleState(toggle);
        }
    });

    document.body.addEventListener("htmx:afterSwap", (event) => {
        restoreExpandedTreeState(event.detail?.target);
    });

    restoreExpandedTreeState(document);
})();
