# Sports Management System

A Java + Swing desktop application for Windows implementing full CRUD operations
on a three-tier hierarchy (Sports → Clubs → Members), with custom trie indexing,
three sorting algorithms, and three search strategies.

---

## Requirements
- **JDK 17+** — download free from https://adoptium.net
- No other dependencies. Swing is bundled with the JDK.

## How to Run
Double-click `run.bat`, or from a command prompt inside this folder:
```
run.bat
```

---

## Architecture

```
sports/
├── Main.java                     Entry point (SwingUtilities.invokeLater)
├── DataStore.java                Single source of truth; owns all entities + both tries
│
├── entities/
│   ├── Sport.java                sportID (auto), sportName, reputation, List<Club>
│   ├── Club.java                 clubID  (auto), clubName, clubScore, List<Member>, →Sport
│   └── Member.java               memberID(auto), memberName, scoring, →Club
│
├── trie/
│   ├── TrieNode.java             value | nextSibling | firstChild | dataPointer
│   └── Trie.java                 NAME trie (26-slot root) + ID trie (10-slot root)
│                                 insert / search / delete (bottom-up safe pruning)
│
├── sorting/
│   └── SortingAlgorithms.java    mergeSort / selectionSort / quickSort
│                                 + shared Comparators for all entity fields
│
├── search/
│   └── SearchEngine.java         trieSearch   — O(k) via Trie
│                                 linearSearch — O(n), full path required
│                                 binarySearch — O(log n) on sorted list
│                                               printAll=true → collect everything
└── gui/
    └── MainWindow.java           4-tab Swing UI
                                  Tab 1: Sports  (insert/edit/delete + live table)
                                  Tab 2: Clubs   (insert/edit/delete + live table)
                                  Tab 3: Members (insert/edit/delete + live table)
                                  Tab 4: Search & Sort
```

---

## Key Design Decisions

### Trie Structure
- Two separate tries: one for **names** (A–Z root), one for **IDs** (0–9 root)
- Every non-root layer is a **linked list** of TrieNode siblings
- A new node is only created when a character is absent from that layer's list
  → maximum 26 nodes per layer (name trie), 10 (ID trie)
- `dataPointer` is non-null only at a terminal node (the stored entity object)
- Deletion walks **bottom-up**: a node is removed only if it has no children
  and no dataPointer; linked-list integrity is preserved with a prev pointer

### Cascade Delete
```
deleteSport  → iterates clubs → for each club: iterates members → deleteMemberInternal
                                              → deleteClubInternal
             → removes sport from trie + master list

deleteClub   → iterates members → deleteMemberInternal
             → removes club from trie, master list, and parent sport's list

deleteMember → removes from trie, master list, and parent club's list
```

### Binary Search + printAll mode
`SearchEngine.binarySearch(list, target, cmp, printAll)`
- `printAll = false` → standard binary search, returns matching item(s)
- `printAll = true`  → skips comparison; acts as a printer, returns all items
- This single method powers both targeted search and "Select All" display

### Sorting
All three algorithms operate on `List<T>` with a `Comparator<T>` — fully generic.
The comparators (MEMBER_BY_NAME, CLUB_BY_SCORE, etc.) live in SortingAlgorithms
and are reused across the GUI and search engine.

---

## Usage Guide

### Insert a Sport
Tab "Sports" → fill Name + Reputation → click "Insert Sport"

### Insert a Club
Tab "Clubs" → fill Club Name, Score, and the **exact** parent Sport name → "Insert Club"

### Insert a Member
Tab "Members" → fill Name, Scoring, parent Sport name, parent Club name → "Insert Member"

### Edit / Delete
Select a row in any table → click Edit or Delete.
Deleting a Sport cascades to all its Clubs and their Members.

### Search
Tab "Search & Sort":
- **Trie (Index)**: fastest, enter name only
- **Linear**: slower O(n), requires sport/club path for lower-level entities
- **Binary Search**: choose algorithm + field → sorts then searches

### Select All
Tab "Search & Sort" → right panel → choose entity, sorted/unsorted, algorithm → "Select All"
Results appear in the green console panel at the bottom.
