package sports.gui;

import sports.DataStore;
import sports.entities.Club;
import sports.entities.Member;
import sports.entities.Sport;
import sports.search.SearchEngine;
import sports.sorting.SortingAlgorithms;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * MainWindow — Swing UI for the Sports Management System.
 *
 * Responsibilities
 * ────────────────
 * • Reads user input and calls DataStore for all mutations (insert/edit/delete).
 * • For display, works only on COPIES of the master lists — never mutates them.
 * • All search operations are delegated exclusively to SearchEngine.
 * • All sort operations are delegated exclusively to SortingAlgorithms.
 * • Binary search path: sort the copy first (user's chosen algorithm), then
 *   pass the sorted copy AND the SAME comparator to SearchEngine.binarySearch().
 * • Trie and linear search paths: no sorting — just call SearchEngine directly.
 * • Select All: copies master list, optionally sorts it, then iterates —
 *   does NOT use binarySearch() for retrieval.
 *
 * Architecture rules enforced here
 * ─────────────────────────────────
 * • SearchEngine is the only caller of trie/linear/binary search logic.
 * • SortingAlgorithms is the only caller of sort logic.
 * • A single generic applySort() replaces the old three duplicated per-entity
 *   sort helpers.
 * • Binary search always uses the same comparator for both sort and search.
 */
public class MainWindow extends JFrame {

    private final DataStore store = new DataStore();

    // ── Sports tab ─────────────────────────────────────────────────────────────
    private JTextField tfSportName, tfSportRep;
    private DefaultTableModel sportTableModel;
    private JTable sportTable;

    // ── Clubs tab ──────────────────────────────────────────────────────────────
    private JTextField tfClubName, tfClubScore, tfClubParentSport;
    private DefaultTableModel clubTableModel;
    private JTable clubTable;

    // ── Members tab ────────────────────────────────────────────────────────────
    private JTextField tfMemberName, tfMemberScore, tfMemberParentSport, tfMemberParentClub;
    private DefaultTableModel memberTableModel;
    private JTable memberTable;

    // ── Search & Sort tab ─────────────────────────────────────────────────────
    private JComboBox<String> cbSearchType, cbEntityType, cbSortAlgo, cbSortField;
    private JTextField tfSearchName, tfSearchSport, tfSearchClub;
    private JTextArea  taResults;

    // ── Palette ───────────────────────────────────────────────────────────────
    private static final Color BG      = new Color(30,  30,  46);
    private static final Color SURFACE = new Color(49,  50,  68);
    private static final Color ACCENT  = new Color(137, 180, 250);
    private static final Color TEXT    = new Color(205, 214, 244);
    private static final Color SUBTEXT = new Color(166, 173, 200);
    private static final Color GREEN   = new Color(166, 227, 161);
    private static final Color RED     = new Color(243, 139, 168);
    private static final Color YELLOW  = new Color(249, 226, 175);

    // ═══════════════════════════════════════════════════════════════════════════

    public MainWindow() {
        super("Sports Management System");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);
        applyGlobalTheme();

        JTabbedPane tabs = new JTabbedPane();
        styleTabPane(tabs);
        tabs.addTab("🏅 Sports",        buildSportTab());
        tabs.addTab("🏟 Clubs",         buildClubTab());
        tabs.addTab("👤 Members",       buildMemberTab());
        tabs.addTab("🔍 Search & Sort", buildSearchTab());

        add(tabs);
        setVisible(true);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  THEME HELPERS
    // ═══════════════════════════════════════════════════════════════════════════

    private void applyGlobalTheme() {
        getContentPane().setBackground(BG);
        UIManager.put("TabbedPane.background",       BG);
        UIManager.put("TabbedPane.foreground",       TEXT);
        UIManager.put("TabbedPane.selected",         SURFACE);
        UIManager.put("TabbedPane.contentAreaColor", BG);
        UIManager.put("Panel.background",            BG);
        UIManager.put("TextField.background",        SURFACE);
        UIManager.put("TextField.foreground",        TEXT);
        UIManager.put("TextField.caretForeground",   ACCENT);
        UIManager.put("ComboBox.background",         SURFACE);
        UIManager.put("ComboBox.foreground",         TEXT);
        UIManager.put("Table.background",            SURFACE);
        UIManager.put("Table.foreground",            TEXT);
        UIManager.put("Table.gridColor",             BG);
        UIManager.put("Table.selectionBackground",   ACCENT);
        UIManager.put("Table.selectionForeground",   BG);
        UIManager.put("TableHeader.background",      BG);
        UIManager.put("TableHeader.foreground",      ACCENT);
        UIManager.put("ScrollPane.background",       BG);
        UIManager.put("TextArea.background",         SURFACE);
        UIManager.put("TextArea.foreground",         TEXT);
        UIManager.put("Label.foreground",            TEXT);
    }

    private void styleTabPane(JTabbedPane t) {
        t.setBackground(BG);
        t.setForeground(TEXT);
        t.setFont(new Font("Segoe UI", Font.BOLD, 13));
    }

    private JPanel panel(LayoutManager lm) {
        JPanel p = new JPanel(lm);
        p.setBackground(BG);
        return p;
    }

    private JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(SUBTEXT);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        return l;
    }

    private JTextField field() {
        JTextField f = new JTextField();
        f.setBackground(SURFACE);
        f.setForeground(TEXT);
        f.setCaretColor(ACCENT);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ACCENT, 1),
                BorderFactory.createEmptyBorder(4, 6, 4, 6)));
        f.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        return f;
    }

    private JButton btn(String text, Color color) {
        JButton b = new JButton(text);
        b.setBackground(color);
        b.setForeground(BG);
        b.setFocusPainted(false);
        b.setFont(new Font("Segoe UI", Font.BOLD, 13));
        b.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private TitledBorder titledBorder(String title) {
        TitledBorder tb = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(ACCENT, 1), title);
        tb.setTitleColor(ACCENT);
        tb.setTitleFont(new Font("Segoe UI", Font.BOLD, 12));
        return tb;
    }

    private DefaultTableModel makeTableModel(String... cols) {
        return new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    }

    private JTable makeTable(DefaultTableModel model) {
        JTable t = new JTable(model);
        t.setBackground(SURFACE);
        t.setForeground(TEXT);
        t.setGridColor(BG);
        t.setSelectionBackground(ACCENT);
        t.setSelectionForeground(BG);
        t.setRowHeight(26);
        t.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        t.getTableHeader().setBackground(BG);
        t.getTableHeader().setForeground(ACCENT);
        t.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        return t;
    }

    private JScrollPane scroll(Component c) {
        JScrollPane sp = new JScrollPane(c);
        sp.getViewport().setBackground(SURFACE);
        sp.setBorder(BorderFactory.createLineBorder(ACCENT, 1));
        return sp;
    }

    private GridBagConstraints gbc() {
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets  = new Insets(5, 8, 5, 8);
        gc.anchor  = GridBagConstraints.WEST;
        gc.fill    = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        return gc;
    }

    private void styleCombo(JComboBox<?> cb) {
        cb.setBackground(SURFACE);
        cb.setForeground(TEXT);
        cb.setFont(new Font("Segoe UI", Font.PLAIN, 13));
    }

    private void info(String msg)  { JOptionPane.showMessageDialog(this, msg, "Info",  JOptionPane.INFORMATION_MESSAGE); }
    private void error(String msg) { JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE); }

    // ═══════════════════════════════════════════════════════════════════════════
    //  GENERIC SORT DISPATCHER
    //
    //  Single method replaces the old three duplicated applySortSports /
    //  applySortClubs / applySortMembers methods. Called only from the binary-
    //  search path and the "Select All sorted" path.
    // ═══════════════════════════════════════════════════════════════════════════

    private <T> void applySort(List<T> list, String algo, Comparator<T> cmp) {
        switch (algo) {
            case "Merge Sort"     -> SortingAlgorithms.mergeSort(list, cmp);
            case "Selection Sort" -> SortingAlgorithms.selectionSort(list, cmp);
            case "Quick Sort"     -> SortingAlgorithms.quickSort(list, cmp);
            // Unknown algorithm string: leave list unsorted (safe fallback).
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  TAB 1 — SPORTS
    // ═══════════════════════════════════════════════════════════════════════════

    private JPanel buildSportTab() {
        JPanel root = panel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel form = panel(new GridBagLayout());
        form.setBorder(titledBorder("Sport Details"));
        GridBagConstraints gc = gbc();

        gc.gridx = 0; gc.gridy = 0; form.add(label("Sport Name:"), gc);
        gc.gridx = 1; tfSportName = field(); form.add(tfSportName, gc);
        gc.gridx = 0; gc.gridy = 1; form.add(label("Reputation (0-10):"), gc);
        gc.gridx = 1; tfSportRep  = field(); form.add(tfSportRep, gc);

        JPanel btns = panel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton bInsert = btn("Insert Sport", GREEN);
        JButton bEdit   = btn("Edit Sport",   YELLOW);
        JButton bDelete = btn("Delete Sport", RED);
        btns.add(bInsert); btns.add(bEdit); btns.add(bDelete);

        sportTableModel = makeTableModel("ID", "Sport Name", "Reputation", "# Clubs");
        sportTable = makeTable(sportTableModel);

        JPanel top = panel(new BorderLayout(0, 8));
        top.add(form, BorderLayout.CENTER);
        top.add(btns, BorderLayout.SOUTH);
        root.add(top,              BorderLayout.NORTH);
        root.add(scroll(sportTable), BorderLayout.CENTER);

        // ── Listeners ──────────────────────────────────────────────────────────

        bInsert.addActionListener(e -> {
            String name   = tfSportName.getText().trim();
            String repStr = tfSportRep.getText().trim();
            if (name.isEmpty()) { error("Sport name is required."); return; }
            float rep = 0f;
            try { rep = Float.parseFloat(repStr); } catch (NumberFormatException ignored) {}

            Sport s = store.insertSport(name, rep);
            if (s == null) { error("Insert failed: sport \"" + name + "\" already exists."); return; }
            refreshSportTable();
            info("Sport \"" + s.getSportName() + "\" inserted with ID " + s.getSportID());
            tfSportName.setText(""); tfSportRep.setText("");
        });

        bEdit.addActionListener(e -> {
            int row = sportTable.getSelectedRow();
            if (row < 0) { error("Select a sport to edit."); return; }
            int id = (int) sportTableModel.getValueAt(row, 0);
            Sport s = getSportByID(id);
            if (s == null) return;

            String newName = JOptionPane.showInputDialog(this, "New name:", s.getSportName());
            if (newName == null || newName.trim().isEmpty()) return;
            String repStr = JOptionPane.showInputDialog(this, "New reputation:", s.getReputation());
            float newRep = s.getReputation();
            try { newRep = Float.parseFloat(repStr); } catch (Exception ignored) {}

            boolean ok = store.editSport(s, newName.trim(), newRep);
            if (!ok) { error("Edit failed: name \"" + newName.trim() + "\" may already be in use."); return; }
            refreshSportTable();
            refreshClubTable();
            info("Sport updated.");
        });

        bDelete.addActionListener(e -> {
            int row = sportTable.getSelectedRow();
            if (row < 0) { error("Select a sport to delete."); return; }
            int id = (int) sportTableModel.getValueAt(row, 0);
            Sport s = getSportByID(id);
            if (s == null) return;
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Delete sport \"" + s.getSportName() + "\" and ALL its clubs/members?",
                    "Confirm Delete", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                boolean ok = store.deleteSport(s);
                refreshSportTable(); refreshClubTable(); refreshMemberTable();
                if (ok) info("Sport and all downstream data deleted.");
                else    error("Delete completed but trie reported an inconsistency.");
            }
        });

        return root;
    }

    private void refreshSportTable() {
        sportTableModel.setRowCount(0);
        for (Sport s : store.getAllSports()) {
            sportTableModel.addRow(new Object[]{
                    s.getSportID(), s.getSportName(),
                    String.format("%.1f", s.getReputation()),
                    s.getClubs().size()
            });
        }
    }

    private Sport getSportByID(int id) {
        for (Sport s : store.getAllSports()) if (s.getSportID() == id) return s;
        return null;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  TAB 2 — CLUBS
    // ═══════════════════════════════════════════════════════════════════════════

    private JPanel buildClubTab() {
        JPanel root = panel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel form = panel(new GridBagLayout());
        form.setBorder(titledBorder("Club Details"));
        GridBagConstraints gc = gbc();

        gc.gridx = 0; gc.gridy = 0; form.add(label("Club Name:"), gc);
        gc.gridx = 1; tfClubName        = field(); form.add(tfClubName, gc);
        gc.gridx = 0; gc.gridy = 1; form.add(label("Club Score:"), gc);
        gc.gridx = 1; tfClubScore       = field(); form.add(tfClubScore, gc);
        gc.gridx = 0; gc.gridy = 2; form.add(label("Parent Sport Name:"), gc);
        gc.gridx = 1; tfClubParentSport = field(); form.add(tfClubParentSport, gc);

        JPanel btns = panel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton bInsert = btn("Insert Club", GREEN);
        JButton bEdit   = btn("Edit Club",   YELLOW);
        JButton bDelete = btn("Delete Club", RED);
        btns.add(bInsert); btns.add(bEdit); btns.add(bDelete);

        clubTableModel = makeTableModel("ID", "Club Name", "Score", "Parent Sport", "# Members");
        clubTable = makeTable(clubTableModel);

        JPanel top = panel(new BorderLayout(0, 8));
        top.add(form, BorderLayout.CENTER);
        top.add(btns, BorderLayout.SOUTH);
        root.add(top,            BorderLayout.NORTH);
        root.add(scroll(clubTable), BorderLayout.CENTER);

        bInsert.addActionListener(e -> {
            String name  = tfClubName.getText().trim();
            String sport = tfClubParentSport.getText().trim();
            if (name.isEmpty() || sport.isEmpty()) { error("Club name and parent sport are required."); return; }
            int score = 0;
            try { score = Integer.parseInt(tfClubScore.getText().trim()); } catch (NumberFormatException ignored) {}

            Club c = store.insertClub(name, score, sport);
            if (c == null) { error("Insert failed: sport \"" + sport + "\" not found, or club already exists."); return; }
            refreshClubTable(); refreshSportTable();
            info("Club \"" + c.getClubName() + "\" inserted with ID " + c.getClubID());
            tfClubName.setText(""); tfClubScore.setText(""); tfClubParentSport.setText("");
        });

        bEdit.addActionListener(e -> {
            int row = clubTable.getSelectedRow();
            if (row < 0) { error("Select a club to edit."); return; }
            int id = (int) clubTableModel.getValueAt(row, 0);
            Club c = getClubByID(id);
            if (c == null) return;

            String newName = JOptionPane.showInputDialog(this, "New name:", c.getClubName());
            if (newName == null || newName.trim().isEmpty()) return;
            String scoreStr = JOptionPane.showInputDialog(this, "New score:", c.getClubScore());
            int newScore = c.getClubScore();
            try { newScore = Integer.parseInt(scoreStr); } catch (Exception ignored) {}

            boolean ok = store.editClub(c, newName.trim(), newScore);
            if (!ok) { error("Edit failed: name \"" + newName.trim() + "\" may already be in use."); return; }
            refreshClubTable(); refreshMemberTable();
            info("Club updated.");
        });

        bDelete.addActionListener(e -> {
            int row = clubTable.getSelectedRow();
            if (row < 0) { error("Select a club to delete."); return; }
            int id = (int) clubTableModel.getValueAt(row, 0);
            Club c = getClubByID(id);
            if (c == null) return;
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Delete club \"" + c.getClubName() + "\" and ALL its members?",
                    "Confirm Delete", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                store.deleteClub(c);
                refreshClubTable(); refreshSportTable(); refreshMemberTable();
                info("Club and all members deleted.");
            }
        });

        return root;
    }

    private void refreshClubTable() {
        clubTableModel.setRowCount(0);
        for (Club c : store.getAllClubs()) {
            clubTableModel.addRow(new Object[]{
                    c.getClubID(), c.getClubName(), c.getClubScore(),
                    c.getParentSport() != null ? c.getParentSport().getSportName() : "—",
                    c.getMembers().size()
            });
        }
    }

    private Club getClubByID(int id) {
        for (Club c : store.getAllClubs()) if (c.getClubID() == id) return c;
        return null;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  TAB 3 — MEMBERS
    // ═══════════════════════════════════════════════════════════════════════════

    private JPanel buildMemberTab() {
        JPanel root = panel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel form = panel(new GridBagLayout());
        form.setBorder(titledBorder("Member Details"));
        GridBagConstraints gc = gbc();

        gc.gridx = 0; gc.gridy = 0; form.add(label("Member Name:"), gc);
        gc.gridx = 1; tfMemberName        = field(); form.add(tfMemberName, gc);
        gc.gridx = 0; gc.gridy = 1; form.add(label("Scoring:"), gc);
        gc.gridx = 1; tfMemberScore       = field(); form.add(tfMemberScore, gc);
        gc.gridx = 0; gc.gridy = 2; form.add(label("Parent Sport Name:"), gc);
        gc.gridx = 1; tfMemberParentSport = field(); form.add(tfMemberParentSport, gc);
        gc.gridx = 0; gc.gridy = 3; form.add(label("Parent Club Name:"), gc);
        gc.gridx = 1; tfMemberParentClub  = field(); form.add(tfMemberParentClub, gc);

        JPanel btns = panel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton bInsert = btn("Insert Member", GREEN);
        JButton bEdit   = btn("Edit Member",   YELLOW);
        JButton bDelete = btn("Delete Member", RED);
        btns.add(bInsert); btns.add(bEdit); btns.add(bDelete);

        memberTableModel = makeTableModel("ID", "Member Name", "Scoring", "Club", "Sport");
        memberTable = makeTable(memberTableModel);

        JPanel top = panel(new BorderLayout(0, 8));
        top.add(form, BorderLayout.CENTER);
        top.add(btns, BorderLayout.SOUTH);
        root.add(top,              BorderLayout.NORTH);
        root.add(scroll(memberTable), BorderLayout.CENTER);

        bInsert.addActionListener(e -> {
            String name  = tfMemberName.getText().trim();
            String sport = tfMemberParentSport.getText().trim();
            String club  = tfMemberParentClub.getText().trim();
            if (name.isEmpty() || sport.isEmpty() || club.isEmpty()) {
                error("Name, sport, and club are all required."); return;
            }
            int score = 0;
            try { score = Integer.parseInt(tfMemberScore.getText().trim()); } catch (NumberFormatException ignored) {}

            Member m = store.insertMember(name, score, sport, club);
            if (m == null) { error("Insert failed: sport or club not found, or member already exists."); return; }
            refreshMemberTable(); refreshClubTable();
            info("Member \"" + m.getMemberName() + "\" inserted with ID " + m.getMemberID());
            tfMemberName.setText(""); tfMemberScore.setText("");
            tfMemberParentSport.setText(""); tfMemberParentClub.setText("");
        });

        bEdit.addActionListener(e -> {
            int row = memberTable.getSelectedRow();
            if (row < 0) { error("Select a member to edit."); return; }
            int id = (int) memberTableModel.getValueAt(row, 0);
            Member m = getMemberByID(id);
            if (m == null) return;

            String newName = JOptionPane.showInputDialog(this, "New name:", m.getMemberName());
            if (newName == null || newName.trim().isEmpty()) return;
            String scoreStr = JOptionPane.showInputDialog(this, "New score:", m.getScoring());
            int newScore = m.getScoring();
            try { newScore = Integer.parseInt(scoreStr); } catch (Exception ignored) {}

            boolean ok = store.editMember(m, newName.trim(), newScore);
            if (!ok) { error("Edit failed: name \"" + newName.trim() + "\" may already be in use."); return; }
            refreshMemberTable();
            info("Member updated.");
        });

        bDelete.addActionListener(e -> {
            int row = memberTable.getSelectedRow();
            if (row < 0) { error("Select a member to delete."); return; }
            int id = (int) memberTableModel.getValueAt(row, 0);
            Member m = getMemberByID(id);
            if (m == null) return;
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Delete member \"" + m.getMemberName() + "\"?",
                    "Confirm Delete", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                store.deleteMember(m);
                refreshMemberTable(); refreshClubTable();
                info("Member deleted.");
            }
        });

        return root;
    }

    private void refreshMemberTable() {
        memberTableModel.setRowCount(0);
        for (Member m : store.getAllMembers()) {
            memberTableModel.addRow(new Object[]{
                    m.getMemberID(), m.getMemberName(), m.getScoring(),
                    m.getParentClub() != null ? m.getParentClub().getClubName() : "—",
                    m.getParentClub() != null && m.getParentClub().getParentSport() != null
                            ? m.getParentClub().getParentSport().getSportName() : "—"
            });
        }
    }

    private Member getMemberByID(int id) {
        for (Member m : store.getAllMembers()) if (m.getMemberID() == id) return m;
        return null;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  TAB 4 — SEARCH & SORT
    // ═══════════════════════════════════════════════════════════════════════════

    private JPanel buildSearchTab() {
        JPanel root = panel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel topRow = panel(new GridLayout(1, 2, 10, 0));

        // ── Left: search controls ──────────────────────────────────────────────
        JPanel searchPanel = panel(new GridBagLayout());
        searchPanel.setBorder(titledBorder("Search"));
        GridBagConstraints gc = gbc();

        gc.gridx = 0; gc.gridy = 0; searchPanel.add(label("Search Type:"), gc);
        gc.gridx = 1;
        cbSearchType = new JComboBox<>(new String[]{"Trie (Index)", "Linear", "Binary Search"});
        styleCombo(cbSearchType);
        searchPanel.add(cbSearchType, gc);

        gc.gridx = 0; gc.gridy = 1; searchPanel.add(label("Entity:"), gc);
        gc.gridx = 1;
        cbEntityType = new JComboBox<>(new String[]{"Sport", "Club", "Member"});
        styleCombo(cbEntityType);
        searchPanel.add(cbEntityType, gc);

        gc.gridx = 0; gc.gridy = 2; searchPanel.add(label("Name to Search:"), gc);
        gc.gridx = 1; tfSearchName = field(); searchPanel.add(tfSearchName, gc);

        gc.gridx = 0; gc.gridy = 3; searchPanel.add(label("Sport Name (path):"), gc);
        gc.gridx = 1; tfSearchSport = field(); searchPanel.add(tfSearchSport, gc);

        gc.gridx = 0; gc.gridy = 4; searchPanel.add(label("Club Name (path):"), gc);
        gc.gridx = 1; tfSearchClub = field(); searchPanel.add(tfSearchClub, gc);

        gc.gridx = 0; gc.gridy = 5; gc.gridwidth = 2;
        gc.insets = new Insets(6, 0, 0, 0);
        JPanel sortRow = panel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        sortRow.add(label("Sort Algorithm:"));
        cbSortAlgo = new JComboBox<>(new String[]{"Merge Sort", "Selection Sort", "Quick Sort"});
        styleCombo(cbSortAlgo);
        sortRow.add(cbSortAlgo);
        sortRow.add(label("Sort by:"));
        cbSortField = new JComboBox<>(new String[]{"Name", "Score / Reputation"});
        styleCombo(cbSortField);
        sortRow.add(cbSortField);
        searchPanel.add(sortRow, gc);

        gc.gridy = 6;
        JPanel searchBtnRow = panel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton bSearch = btn("Search", ACCENT);
        searchBtnRow.add(bSearch);
        searchPanel.add(searchBtnRow, gc);

        // ── Right: select-all controls ─────────────────────────────────────────
        JPanel selectAllPanel = panel(new GridBagLayout());
        selectAllPanel.setBorder(titledBorder("Select All"));
        GridBagConstraints gc2 = gbc();

        gc2.gridx = 0; gc2.gridy = 0; selectAllPanel.add(label("Entity type:"), gc2);
        gc2.gridx = 1;
        JComboBox<String> cbAllEntity = new JComboBox<>(new String[]{"Sports", "Clubs", "Members"});
        styleCombo(cbAllEntity);
        selectAllPanel.add(cbAllEntity, gc2);

        gc2.gridx = 0; gc2.gridy = 1; selectAllPanel.add(label("Order:"), gc2);
        gc2.gridx = 1;
        JComboBox<String> cbAllOrder = new JComboBox<>(new String[]{"Unsorted", "Sorted"});
        styleCombo(cbAllOrder);
        selectAllPanel.add(cbAllOrder, gc2);

        gc2.gridx = 0; gc2.gridy = 2; selectAllPanel.add(label("Algorithm:"), gc2);
        gc2.gridx = 1;
        JComboBox<String> cbAllAlgo = new JComboBox<>(new String[]{"Merge Sort", "Selection Sort", "Quick Sort"});
        styleCombo(cbAllAlgo);
        selectAllPanel.add(cbAllAlgo, gc2);

        gc2.gridx = 0; gc2.gridy = 3; gc2.gridwidth = 2;
        gc2.insets = new Insets(10, 0, 0, 0);
        JButton bSelectAll = btn("Select All", GREEN);
        selectAllPanel.add(bSelectAll, gc2);

        topRow.add(searchPanel);
        topRow.add(selectAllPanel);

        // ── Results area ───────────────────────────────────────────────────────
        taResults = new JTextArea();
        taResults.setEditable(false);
        taResults.setFont(new Font("Consolas", Font.PLAIN, 13));
        taResults.setBackground(SURFACE);
        taResults.setForeground(GREEN);
        taResults.setCaretColor(GREEN);
        JPanel resultsPanel = panel(new BorderLayout());
        resultsPanel.setBorder(titledBorder("Results"));
        resultsPanel.add(scroll(taResults), BorderLayout.CENTER);

        root.add(topRow,       BorderLayout.NORTH);
        root.add(resultsPanel, BorderLayout.CENTER);

        bSearch.addActionListener(e -> handleSearch());

        bSelectAll.addActionListener(e -> {
            String entity  = (String) cbAllEntity.getSelectedItem();
            boolean sorted = "Sorted".equals(cbAllOrder.getSelectedItem());
            String algo    = (String) cbAllAlgo.getSelectedItem();
            handleSelectAll(entity, sorted, algo);
        });

        return root;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  SEARCH DISPATCH
    // ═══════════════════════════════════════════════════════════════════════════

    private void handleSearch() {
        String searchType = (String) cbSearchType.getSelectedItem();
        String entity     = (String) cbEntityType.getSelectedItem();
        String name       = tfSearchName.getText().trim();
        String sport      = tfSearchSport.getText().trim();
        String club       = tfSearchClub.getText().trim();
        String algo       = (String) cbSortAlgo.getSelectedItem();
        String sortField  = (String) cbSortField.getSelectedItem();

        if (name.isEmpty()) { error("Enter a name to search."); return; }

        StringBuilder sb = new StringBuilder();
        sb.append("── Search Results ──────────────────────────────\n");
        sb.append("Type: ").append(searchType).append("  |  Entity: ").append(entity).append("\n\n");

        switch (searchType) {
            case "Trie (Index)"  -> handleTrieSearch(entity, name, sb);
            case "Linear"        -> handleLinearSearch(entity, name, sport, club, sb);
            case "Binary Search" -> handleBinarySearch(entity, name, algo, sortField, sb);
        }

        taResults.setText(sb.toString());
    }

    // ─── Trie search ──────────────────────────────────────────────────────────
    // Routes through SearchEngine.trieSearchByName() — the only place that
    // calls trie.search(). No sorting needed or performed.

    private void handleTrieSearch(String entity, String name, StringBuilder sb) {
        // SearchEngine is the single point of contact with the trie
        Object result = SearchEngine.trieSearchByName(store.getNameTrie(), name);

        if (result == null) {
            sb.append("Not found: \"").append(name).append("\"\n");
            return;
        }

        // Verify the result is the correct entity type before displaying
        boolean typeMatch = switch (entity) {
            case "Sport"  -> result instanceof Sport;
            case "Club"   -> result instanceof Club;
            case "Member" -> result instanceof Member;
            default       -> true;
        };

        if (!typeMatch) {
            sb.append("Found a record for \"").append(name)
                    .append("\" but it is not a ").append(entity).append(".\n");
        } else {
            sb.append("Found via Trie:\n").append(result).append("\n");
        }
    }

    // ─── Linear search ────────────────────────────────────────────────────────
    // Routes through SearchEngine linear methods. No sorting needed or performed.

    private void handleLinearSearch(String entity, String name,
                                    String sportPath, String clubPath,
                                    StringBuilder sb) {
        switch (entity) {
            case "Sport" -> {
                // SearchEngine.linearSearchSport works directly on the master list copy
                Sport s = SearchEngine.linearSearchSport(store.getAllSports(), name);
                sb.append(s != null ? "Found: " + s : "Sport \"" + name + "\" not found.").append("\n");
            }
            case "Club" -> {
                if (sportPath.isEmpty()) { sb.append("Provide Sport Name for linear Club search.\n"); return; }
                Sport parent = SearchEngine.linearSearchSport(store.getAllSports(), sportPath);
                if (parent == null) { sb.append("Sport \"").append(sportPath).append("\" not found.\n"); return; }
                Club c = SearchEngine.linearSearchClub(parent, name);
                sb.append(c != null ? "Found: " + c : "Club \"" + name + "\" not found.").append("\n");
            }
            case "Member" -> {
                if (sportPath.isEmpty() || clubPath.isEmpty()) {
                    sb.append("Provide Sport Name and Club Name for linear Member search.\n"); return;
                }
                Member m = SearchEngine.linearSearchMember(store.getAllSports(), sportPath, clubPath, name);
                sb.append(m != null ? "Found: " + m : "Member \"" + name + "\" not found.").append("\n");
            }
        }
    }

    // ─── Binary search ────────────────────────────────────────────────────────
    // REQUIRED order: sort the copy first, then search with the SAME comparator.
    //
    // FIX: old code sorted with `cmp` (selected comparator) but then passed a
    //      hardcoded SPORT_BY_NAME / CLUB_BY_NAME / MEMBER_BY_NAME to binarySearch(),
    //      meaning the list was sorted by one field but searched by another.
    //      Now the same `cmp` variable is used for BOTH the sort and the search.

    private void handleBinarySearch(String entity, String name,
                                    String algo, String sortField,
                                    StringBuilder sb) {
        sb.append("Algorithm: ").append(algo)
                .append("  |  Sort field: ").append(sortField).append("\n\n");

        boolean byScore = "Score / Reputation".equals(sortField);

        switch (entity) {
            case "Sport" -> {
                // Step 1: choose ONE comparator for both sort and search
                Comparator<Sport> cmp = byScore
                        ? SortingAlgorithms.SPORT_BY_REPUTATION
                        : SortingAlgorithms.SPORT_BY_NAME;

                // Step 2: work on a copy of the master list
                List<Sport> list = new ArrayList<>(store.getAllSports());

                // Step 3: sort using the chosen algorithm and comparator
                applySort(list, algo, cmp);

                // Step 4: search using SearchEngine with the SAME comparator
                Sport dummy = new Sport(name, 0f);
                List<Sport> results = SearchEngine.binarySearch(list, dummy, cmp);

                if (results.isEmpty()) sb.append("Sport \"").append(name).append("\" not found.\n");
                else results.forEach(s -> sb.append("Found: ").append(s).append("\n"));
            }

            case "Club" -> {
                Comparator<Club> cmp = byScore
                        ? SortingAlgorithms.CLUB_BY_SCORE
                        : SortingAlgorithms.CLUB_BY_NAME;

                List<Club> list = new ArrayList<>(store.getAllClubs());
                applySort(list, algo, cmp);

                Club dummy = new Club(name, 0, null);
                List<Club> results = SearchEngine.binarySearch(list, dummy, cmp);

                if (results.isEmpty()) sb.append("Club \"").append(name).append("\" not found.\n");
                else results.forEach(c -> sb.append("Found: ").append(c).append("\n"));
            }

            case "Member" -> {
                Comparator<Member> cmp = byScore
                        ? SortingAlgorithms.MEMBER_BY_SCORE
                        : SortingAlgorithms.MEMBER_BY_NAME;

                List<Member> list = new ArrayList<>(store.getAllMembers());
                applySort(list, algo, cmp);

                Member dummy = new Member(name, 0, null);
                List<Member> results = SearchEngine.binarySearch(list, dummy, cmp);

                if (results.isEmpty()) sb.append("Member \"").append(name).append("\" not found.\n");
                else results.forEach(m -> sb.append("Found: ").append(m).append("\n"));
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  SELECT ALL
    //
    //  FIX: old code called SearchEngine.binarySearch(list, null, cmp, true)
    //       with printAll=true as a "get everything" shortcut — this was a
    //       misuse of binary search (null target, no actual searching done).
    //       Now we simply copy the master list and optionally sort it.
    //       No search engine call needed; retrieval is not a search operation.
    // ═══════════════════════════════════════════════════════════════════════════

    private void handleSelectAll(String entity, boolean sorted, String algo) {
        StringBuilder sb = new StringBuilder();
        sb.append("── Select All: ").append(entity)
                .append(sorted ? " [Sorted — " + algo + "]" : " [Unsorted]")
                .append(" ───────────────────────\n\n");

        switch (entity) {
            case "Sports" -> {
                // Work on a copy — never mutate the master list
                List<Sport> list = new ArrayList<>(store.getAllSports());
                if (sorted) applySort(list, algo, SortingAlgorithms.SPORT_BY_NAME);
                if (list.isEmpty()) sb.append("(No data)\n");
                else list.forEach(s -> sb.append(s).append("\n"));
            }
            case "Clubs" -> {
                List<Club> list = new ArrayList<>(store.getAllClubs());
                if (sorted) applySort(list, algo, SortingAlgorithms.CLUB_BY_NAME);
                if (list.isEmpty()) sb.append("(No data)\n");
                else list.forEach(c -> sb.append(c).append("\n"));
            }
            case "Members" -> {
                List<Member> list = new ArrayList<>(store.getAllMembers());
                if (sorted) applySort(list, algo, SortingAlgorithms.MEMBER_BY_NAME);
                if (list.isEmpty()) sb.append("(No data)\n");
                else list.forEach(m -> sb.append(m).append("\n"));
            }
        }

        taResults.setText(sb.toString());
    }
}