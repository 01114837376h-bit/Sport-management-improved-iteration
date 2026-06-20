package sports.trie;

/**
 * Generic Trie used for both name-based (A–Z) and ID-based (0–9) indexing.
 *
 * Root layout
 * ───────────
 *  NAME trie  → root array of 26 slots, index = ch - 'A'
 *  ID   trie  → root array of 10 slots, index = digit - '0'
 *
 * Each root slot holds the HEAD of a linked list.
 * Every linked-list node is a TrieNode with:
 *   value, nextSibling (same layer), firstChild (next layer), dataPointer.
 *
 * A new node is created ONLY when the required character does not yet exist
 * in the current layer's linked list — so each layer has at most 26 (or 10)
 * nodes total.
 *
 * Deletion walks from the terminal node back toward the root and removes a
 * node ONLY when it has no children and no dataPointer (nothing else depends
 * on it). Linked-list integrity is preserved with a prev pointer.
 */
public class Trie {

    public enum TrieType { NAME, ID }

    private final TrieNode[] root;
    private final TrieType   type;

    // ── Construction ───────────────────────────────────────────────────────────

    public Trie(TrieType type) {
        this.type = type;
        this.root = (type == TrieType.NAME) ? new TrieNode[26]
                : new TrieNode[10];
    }

    // ── Public API ─────────────────────────────────────────────────────────────

    /**
     * Insert a key and associate it with the given object.
     *
     * Returns true  — key was new and data was stored.
     * Returns false — key already exists (duplicate rejected) or key is invalid.
     *
     * FIX: was key.length() <= 1 which wrongly rejected single-character keys.
     *      Changed to key.isEmpty() so single-char keys ("A", "1") are accepted.
     * FIX: was silently overwriting duplicates. Now rejects them and returns
     *      false so DataStore can report a real failure to the caller.
     */
    public boolean insert(String key, Object data) {
        if (key == null || key.isEmpty()) return false;
        String normalized = normalize(key);

        int rootIdx = rootIndex(normalized.charAt(0));
        if (rootIdx < 0) return false;                  // character outside valid range

        // ── Layer 0: root array ────────────────────────────────────────────────
        if (root[rootIdx] == null) {
            root[rootIdx] = new TrieNode(normalized.charAt(0));
        }
        TrieNode current = root[rootIdx];

        // ── Layers 1 … n-1: walk / create ─────────────────────────────────────
        for (int i = 1; i < normalized.length(); i++) {
            current = findOrCreateChild(current, normalized.charAt(i));
            if (current == null) return false;          // node creation failed (should not happen)
        }

        // ── Terminal node ──────────────────────────────────────────────────────
        // FIX: reject duplicates instead of silently overwriting.
        if (current.dataPointer != null) return false;  // key already present

        current.dataPointer = data;
        return true;
    }

    /**
     * Search for a key.
     * Returns the associated object, or null if not found.
     */
    public Object search(String key) {
        if (key == null || key.isEmpty()) return null;
        String normalized = normalize(key);

        int rootIdx = rootIndex(normalized.charAt(0));
        if (rootIdx < 0 || root[rootIdx] == null) return null;

        TrieNode current = findInList(root[rootIdx], normalized.charAt(0));
        if (current == null) return null;

        for (int i = 1; i < normalized.length(); i++) {
            if (current.firstChild == null) return null;
            current = findInList(current.firstChild, normalized.charAt(i));
            if (current == null) return null;
        }

        return current.dataPointer;
    }

    /**
     * Delete a key.
     *
     * Returns true  — key existed and was successfully removed.
     * Returns false — key was not present (nothing to delete).
     *
     * FIX: old code returned true even when dataPointer was already null,
     *      meaning it reported success for a key that was never inserted.
     *      Now we check dataPointer != null before clearing it.
     *
     * After clearing the terminal dataPointer, cascades upward and removes
     * nodes that are now "empty" (no children, no data).
     */
    public boolean delete(String key) {
        if (key == null || key.isEmpty()) return false;
        String normalized = normalize(key);

        TrieNode[] path = new TrieNode[normalized.length()];

        int rootIdx = rootIndex(normalized.charAt(0));
        if (rootIdx < 0 || root[rootIdx] == null) return false;

        TrieNode current = findInList(root[rootIdx], normalized.charAt(0));
        if (current == null) return false;
        path[0] = current;

        for (int i = 1; i < normalized.length(); i++) {
            if (current.firstChild == null) return false;
            current = findInList(current.firstChild, normalized.charAt(i));
            if (current == null) return false;
            path[i] = current;
        }

        TrieNode terminal = path[normalized.length() - 1];

        // FIX: if dataPointer is already null the key was never successfully
        //      inserted, or was already deleted — report false instead of true.
        if (terminal.dataPointer == null) return false;
        terminal.dataPointer = null;

        // Walk back up: prune nodes that are now empty
        for (int i = normalized.length() - 1; i >= 0; i--) {
            TrieNode node = path[i];
            if (node.firstChild != null || node.dataPointer != null) break;

            if (i == 0) {
                removeFromRootList(rootIdx, node);
            } else {
                removeFromChildList(path[i - 1], node);
            }
        }

        return true;
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    /** Normalize: uppercase for name trie, keep as-is for ID trie. */
    private String normalize(String key) {
        return (type == TrieType.NAME) ? key.toUpperCase() : key;
    }

    /** Map first character to root-array index. Returns -1 on invalid input. */
    private int rootIndex(char ch) {
        if (type == TrieType.NAME) {
            int idx = ch - 'A';
            return (idx >= 0 && idx < 26) ? idx : -1;
        } else {
            int idx = ch - '0';
            return (idx >= 0 && idx < 10) ? idx : -1;
        }
    }

    /**
     * Find a node with the given character in a linked list starting at head.
     * Returns null if not found.
     */
    private TrieNode findInList(TrieNode head, char ch) {
        TrieNode cur = head;
        while (cur != null) {
            if (cur.value == ch) return cur;
            cur = cur.nextSibling;
        }
        return null;
    }

    /**
     * Find or create a child node for 'ch' under 'parent'.
     * If 'parent' has no firstChild, create one.
     * Otherwise walk the sibling list; append a new node only if 'ch' absent.
     */
    private TrieNode findOrCreateChild(TrieNode parent, char ch) {
        if (parent.firstChild == null) {
            parent.firstChild = new TrieNode(ch);
            return parent.firstChild;
        }

        TrieNode cur  = parent.firstChild;
        TrieNode tail = cur;
        while (cur != null) {
            if (cur.value == ch) return cur;
            tail = cur;
            cur  = cur.nextSibling;
        }

        tail.nextSibling = new TrieNode(ch);
        return tail.nextSibling;
    }

    /** Remove 'node' from the root-level linked list at root[rootIdx]. */
    private void removeFromRootList(int rootIdx, TrieNode node) {
        if (root[rootIdx] == null) return;

        if (root[rootIdx] == node) {
            root[rootIdx] = node.nextSibling;
            return;
        }

        TrieNode prev = root[rootIdx];
        while (prev.nextSibling != null && prev.nextSibling != node) {
            prev = prev.nextSibling;
        }
        if (prev.nextSibling == node) {
            prev.nextSibling = node.nextSibling;
        }
    }

    /** Remove 'node' from parent.firstChild sibling list. */
    private void removeFromChildList(TrieNode parent, TrieNode node) {
        if (parent.firstChild == null) return;

        if (parent.firstChild == node) {
            parent.firstChild = node.nextSibling;
            return;
        }

        TrieNode prev = parent.firstChild;
        while (prev.nextSibling != null && prev.nextSibling != node) {
            prev = prev.nextSibling;
        }
        if (prev.nextSibling == node) {
            prev.nextSibling = node.nextSibling;
        }
    }
}