package sports.trie;

public class TrieNode {

    public char value;
    public TrieNode nextSibling;   // horizontal — same layer
    public TrieNode firstChild;    // vertical   — next layer
    public Object dataPointer;


    public TrieNode(char value) {
        this.value = value;
       this.nextSibling = null;
        this.firstChild  = null;
        this.dataPointer = null;

    }
}


