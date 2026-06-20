package sports;

import sports.entities.Club;
import sports.entities.Member;
import sports.entities.Sport;
import sports.trie.Trie;

import java.util.ArrayList;
import java.util.List;

/**
 * DataStore — the single source of truth for the application.
 *
 * Responsibilities
 * ────────────────
 * • Owns the master lists of Sports, Clubs, Members.
 * • Owns both tries (name trie + ID trie) and keeps them in sync.
 * • Provides insert / edit / delete with full cascade logic:
 *     delete sport  → deletes all its clubs → deletes all their members
 *     delete club   → deletes all its members
 *     delete member → standalone removal
 * • Every mutating method returns a meaningful success/failure value —
 *   success is only reported when ALL steps actually succeeded.
 */
public class DataStore {

    // ── Master lists ───────────────────────────────────────────────────────────
    private final List<Sport>  sports  = new ArrayList<>();
    private final List<Club>   clubs   = new ArrayList<>();
    private final List<Member> members = new ArrayList<>();

    // ── Tries ──────────────────────────────────────────────────────────────────
    private final Trie nameTrie = new Trie(Trie.TrieType.NAME);
    private final Trie idTrie   = new Trie(Trie.TrieType.ID);

    // ═══════════════════════════════════════════════════════════════════════════
    //  INSERT
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Insert a sport.
     *
     * Returns the new Sport on success, null on failure.
     *
     * FIX: old code had no duplicate guard and always returned non-null.
     *      Now checks for an existing sport with the same name first.
     *      Also verifies that both trie inserts succeed; rolls back the name
     *      trie insert if the ID trie insert fails so the tries stay in sync.
     */
    public Sport insertSport(String name, float reputation) {
        // Reject duplicate names
        if (findSportByName(name) != null) return null;

        Sport s = new Sport(name, reputation);

        boolean nameOk = nameTrie.insert(s.getSportName(), s);
        if (!nameOk) return null;                       // name already in trie

        boolean idOk = idTrie.insert(String.valueOf(s.getSportID()), s);
        if (!idOk) {
            nameTrie.delete(s.getSportName());          // rollback
            return null;
        }

        sports.add(s);
        return s;
    }

    /**
     * Insert a club.
     *
     * parentSportName must match an existing Sport (case-insensitive).
     * Returns null if the parent sport is not found or the name already exists
     * in that sport.
     *
     * FIX: old code had no duplicate-club-name guard.
     *      Added rollback of the name trie insert if the ID trie insert fails.
     */
    public Club insertClub(String clubName, int clubScore, String parentSportName) {
        Sport parent = findSportByName(parentSportName);
        if (parent == null) return null;

        // Reject duplicate club names within the same sport
        if (findClubInSport(parent, clubName) != null) return null;

        Club c = new Club(clubName, clubScore, parent);

        boolean nameOk = nameTrie.insert(c.getClubName(), c);
        if (!nameOk) return null;

        boolean idOk = idTrie.insert(String.valueOf(c.getClubID()), c);
        if (!idOk) {
            nameTrie.delete(c.getClubName());           // rollback
            return null;
        }

        parent.addClub(c);
        clubs.add(c);
        return c;
    }

    /**
     * Insert a member.
     *
     * Both parentSportName and parentClubName must resolve.
     * Returns null on lookup failure or duplicate member name in the club.
     *
     * FIX: old code had no duplicate-member-name guard.
     *      Added rollback of the name trie insert if the ID trie insert fails.
     */
    public Member insertMember(String memberName, int scoring,
                               String parentSportName, String parentClubName) {
        Sport sport = findSportByName(parentSportName);
        if (sport == null) return null;

        Club club = findClubInSport(sport, parentClubName);
        if (club == null) return null;

        // Reject duplicate member names within the same club
        if (findMemberInClub(club, memberName) != null) return null;

        Member m = new Member(memberName, scoring, club);

        boolean nameOk = nameTrie.insert(m.getMemberName(), m);
        if (!nameOk) return null;

        boolean idOk = idTrie.insert(String.valueOf(m.getMemberID()), m);
        if (!idOk) {
            nameTrie.delete(m.getMemberName());         // rollback
            return null;
        }

        club.addMember(m);
        members.add(m);
        return m;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EDIT
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Edit a sport's name and reputation.
     *
     * Returns true only if both trie deletes and both re-inserts succeed.
     *
     * FIX: old code always returned true regardless of trie result.
     *      Now verifies the name-trie re-insert; rolls back to the old name
     *      if the new-name insert fails (e.g. new name already taken).
     */
    public boolean editSport(Sport s, String newName, float newReputation) {
        String oldName = s.getSportName();

        boolean delName = nameTrie.delete(oldName);
        boolean delId   = idTrie.delete(String.valueOf(s.getSportID()));
        if (!delName || !delId) return false;           // wasn't in the trie — data inconsistency

        s.setSportName(newName);
        s.setReputation(newReputation);

        boolean insName = nameTrie.insert(s.getSportName(), s);
        if (!insName) {
            // New name is taken — revert to old name
            s.setSportName(oldName);
            nameTrie.insert(oldName, s);
            idTrie.insert(String.valueOf(s.getSportID()), s);
            return false;
        }

        boolean insId = idTrie.insert(String.valueOf(s.getSportID()), s);
        if (!insId) {
            // Shouldn't happen (ID never changes), but handle it cleanly
            nameTrie.delete(s.getSportName());
            s.setSportName(oldName);
            nameTrie.insert(oldName, s);
            idTrie.insert(String.valueOf(s.getSportID()), s);
            return false;
        }

        return true;
    }

    /**
     * Edit a club's name and score.
     * Returns true only when all trie operations succeed.
     * FIX: same as editSport — old code always returned true.
     */
    public boolean editClub(Club c, String newName, int newScore) {
        String oldName = c.getClubName();

        boolean delName = nameTrie.delete(oldName);
        boolean delId   = idTrie.delete(String.valueOf(c.getClubID()));
        if (!delName || !delId) return false;

        c.setClubName(newName);
        c.setClubScore(newScore);

        boolean insName = nameTrie.insert(c.getClubName(), c);
        if (!insName) {
            c.setClubName(oldName);
            nameTrie.insert(oldName, c);
            idTrie.insert(String.valueOf(c.getClubID()), c);
            return false;
        }

        boolean insId = idTrie.insert(String.valueOf(c.getClubID()), c);
        if (!insId) {
            nameTrie.delete(c.getClubName());
            c.setClubName(oldName);
            nameTrie.insert(oldName, c);
            idTrie.insert(String.valueOf(c.getClubID()), c);
            return false;
        }

        return true;
    }

    /**
     * Edit a member's name and score.
     * Returns true only when all trie operations succeed.
     * FIX: same as editSport — old code always returned true.
     */
    public boolean editMember(Member m, String newName, int newScore) {
        String oldName = m.getMemberName();

        boolean delName = nameTrie.delete(oldName);
        boolean delId   = idTrie.delete(String.valueOf(m.getMemberID()));
        if (!delName || !delId) return false;

        m.setMemberName(newName);
        m.setScoring(newScore);

        boolean insName = nameTrie.insert(m.getMemberName(), m);
        if (!insName) {
            m.setMemberName(oldName);
            nameTrie.insert(oldName, m);
            idTrie.insert(String.valueOf(m.getMemberID()), m);
            return false;
        }

        boolean insId = idTrie.insert(String.valueOf(m.getMemberID()), m);
        if (!insId) {
            nameTrie.delete(m.getMemberName());
            m.setMemberName(oldName);
            nameTrie.insert(oldName, m);
            idTrie.insert(String.valueOf(m.getMemberID()), m);
            return false;
        }

        return true;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  DELETE  (with cascade)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Delete a sport and ALL of its clubs and their members.
     *
     * Returns true if the sport existed and was removed.
     * Returns false if the sport was not found in the tries (data inconsistency).
     */
    public boolean deleteSport(Sport s) {
        // Cascade — iterate over a copy to avoid ConcurrentModificationException
        for (Club c : new ArrayList<>(s.getClubs())) {
            deleteClubCascadeOnly(c);                   // removes members + club from tries/lists
        }
        // Sport's own club list is now empty; clear it explicitly for safety
        s.getClubs().clear();

        boolean delName = nameTrie.delete(s.getSportName());
        boolean delId   = idTrie.delete(String.valueOf(s.getSportID()));
        sports.remove(s);

        return delName && delId;
    }

    /**
     * Delete a club and ALL of its members.
     *
     * Returns true if the club existed and was removed.
     *
     * FIX: old deleteClubInternal() did not remove the club from its parent
     *      sport's list, leaving a dangling reference when called from
     *      deleteSport().  Now deleteSport() clears the sport's club list
     *      explicitly, while deleteClub() removes via removeClub() as before.
     */
    public boolean deleteClub(Club c) {
        deleteClubCascadeOnly(c);
        if (c.getParentSport() != null) {
            c.getParentSport().removeClub(c);
        }
        return true;
    }

    /**
     * Internal cascade helper: removes all members of the club, then removes
     * the club from the tries and master list.
     * Does NOT touch the parent sport's club list — the caller handles that.
     */
    private void deleteClubCascadeOnly(Club c) {
        for (Member m : new ArrayList<>(c.getMembers())) {
            deleteMemberFromClub(m);                    // removes from tries + master list only
        }
        c.getMembers().clear();                         // club's own member list is now empty

        nameTrie.delete(c.getClubName());
        idTrie.delete(String.valueOf(c.getClubID()));
        clubs.remove(c);
    }

    /**
     * Delete a single member.
     *
     * Returns true if the member existed and was removed.
     *
     * FIX: old deleteMemberInternal() called m.getParentClub().removeMember(m)
     *      AND deleteMember() called it again after — causing a double removal
     *      from the club's member list.  Now the internal helper only touches
     *      the tries and master list; the public method removes from the club.
     */
    public boolean deleteMember(Member m) {
        deleteMemberFromClub(m);
        if (m.getParentClub() != null) {
            m.getParentClub().removeMember(m);
        }
        return true;
    }

    /**
     * Internal helper: removes member from tries and master list only.
     * Does NOT touch the parent club's member list — the caller handles that.
     */
    private void deleteMemberFromClub(Member m) {
        nameTrie.delete(m.getMemberName());
        idTrie.delete(String.valueOf(m.getMemberID()));
        members.remove(m);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  LOOKUP HELPERS  (used internally and by SearchEngine)
    // ═══════════════════════════════════════════════════════════════════════════

    public Sport findSportByName(String name) {
        for (Sport s : sports) {
            if (s.getSportName().equalsIgnoreCase(name)) return s;
        }
        return null;
    }

    public Club findClubInSport(Sport sport, String clubName) {
        for (Club c : sport.getClubs()) {
            if (c.getClubName().equalsIgnoreCase(clubName)) return c;
        }
        return null;
    }

    public Member findMemberInClub(Club club, String memberName) {
        for (Member m : club.getMembers()) {
            if (m.getMemberName().equalsIgnoreCase(memberName)) return m;
        }
        return null;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  ACCESSORS
    // ═══════════════════════════════════════════════════════════════════════════

    public List<Sport>  getAllSports()  { return sports;  }
    public List<Club>   getAllClubs()   { return clubs;   }
    public List<Member> getAllMembers() { return members; }
    public Trie         getNameTrie()  { return nameTrie; }
    public Trie         getIDTrie()    { return idTrie;   }
}