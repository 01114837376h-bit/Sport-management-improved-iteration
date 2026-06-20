package sports.search;

import sports.entities.Club;
import sports.entities.Member;
import sports.entities.Sport;
import sports.trie.Trie;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * SearchEngine — the ONLY place in the application that performs searches.
 *
 * Rules enforced here
 * ───────────────────
 * • Trie search   — no sorting required or performed. O(key-length).
 * • Linear search — no sorting required or performed. O(n).
 * • Binary search — the caller MUST supply a list that is already sorted with
 *                   the SAME comparator passed here. The engine never sorts
 *                   internally; sorting is the caller's responsibility and is
 *                   done via SortingAlgorithms before calling binarySearch().
 *
 * FIX: removed the printAll boolean from binarySearch(). Using binary search
 *      as a "retrieve everything" shortcut was semantically wrong and hid the
 *      real bug where a null target was passed. Callers that want all records
 *      should simply copy the master list directly.
 *
 * FIX: the typed convenience wrappers (binarySearchSportByName etc.) no longer
 *      sort internally — they require a pre-sorted list. This makes the
 *      sort/search contract explicit and visible at the call site.
 */
public class SearchEngine {

    // ═══════════════════════════════════════════════════════════════════════════
    //  TRIE SEARCH
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Look up an entity by name in the name trie. O(key-length).
     * No sorting required.
     *
     * @param nameTrie the application name trie
     * @param name     the exact name to look up
     * @return the stored Sport / Club / Member, or null if not found
     */
    public static Object trieSearchByName(Trie nameTrie, String name) {
        return nameTrie.search(name);
    }

    /**
     * Look up an entity by its numeric ID in the ID trie. O(id-length).
     * No sorting required.
     *
     * @param idTrie the application ID trie
     * @param id     the entity ID to look up
     * @return the stored Sport / Club / Member, or null if not found
     */
    public static Object trieSearchByID(Trie idTrie, int id) {
        return idTrie.search(String.valueOf(id));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  LINEAR SEARCH
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Scans all sports for a case-insensitive name match. O(n).
     * No sorting required.
     */
    public static Sport linearSearchSport(List<Sport> allSports, String sportName) {
        for (Sport s : allSports) {
            if (s.getSportName().equalsIgnoreCase(sportName)) return s;
        }
        return null;
    }

    /**
     * Scans the clubs owned by parentSport for a case-insensitive name match. O(n).
     * No sorting required.
     */
    public static Club linearSearchClub(Sport parentSport, String clubName) {
        if (parentSport == null) return null;
        for (Club c : parentSport.getClubs()) {
            if (c.getClubName().equalsIgnoreCase(clubName)) return c;
        }
        return null;
    }

    /**
     * Navigates sport → club → member via linear scan at each level. O(n).
     * No sorting required.
     * Returns null if any step of the path is not found.
     */
    public static Member linearSearchMember(List<Sport> allSports,
                                            String sportName,
                                            String clubName,
                                            String memberName) {
        Sport sport = linearSearchSport(allSports, sportName);
        if (sport == null) return null;

        Club club = linearSearchClub(sport, clubName);
        if (club == null) return null;

        for (Member m : club.getMembers()) {
            if (m.getMemberName().equalsIgnoreCase(memberName)) return m;
        }
        return null;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  BINARY SEARCH  (generic core)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Binary search on a pre-sorted list. O(log n).
     *
     * CONTRACT: sortedList MUST already be sorted with the same comparator
     * that is passed here. Violating this contract produces undefined results.
     *
     * Duplicate matches are expanded left and right from the hit position so
     * all equal elements are returned in list order.
     *
     * FIX: removed the printAll boolean entirely. Passing printAll=true with a
     *      null target was a misuse of this method; callers that want all
     *      records should copy the master list directly — no search needed.
     *
     * @param sortedList list already sorted by cmp
     * @param target     dummy instance whose key field(s) are used for comparison
     * @param cmp        comparator — MUST be the same one used to sort the list
     * @return all elements that compare equal to target, in list order; empty if none
     */
    public static <T> List<T> binarySearch(List<T> sortedList,
                                           T target,
                                           Comparator<T> cmp) {
        List<T> result = new ArrayList<>();
        if (sortedList == null || sortedList.isEmpty()) return result;

        int low  = 0;
        int high = sortedList.size() - 1;

        while (low <= high) {
            int mid    = low + (high - low) / 2;
            int cmpVal = cmp.compare(sortedList.get(mid), target);

            if (cmpVal == 0) {
                result.add(sortedList.get(mid));

                // Expand left for duplicates
                int left = mid - 1;
                while (left >= 0 && cmp.compare(sortedList.get(left), target) == 0) {
                    result.add(0, sortedList.get(left--));
                }
                // Expand right for duplicates
                int right = mid + 1;
                while (right < sortedList.size() && cmp.compare(sortedList.get(right), target) == 0) {
                    result.add(sortedList.get(right++));
                }
                return result;
            } else if (cmpVal < 0) {
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }

        return result;   // empty → not found
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  BINARY SEARCH  (typed convenience wrappers)
    //
    //  These wrappers require the list to be ALREADY SORTED by the corresponding
    //  name comparator before being called. The caller (MainWindow) is
    //  responsible for sorting via SortingAlgorithms before calling these.
    //
    //  Separating "sort" from "search" is intentional: the user chooses which
    //  sorting algorithm to use, and that choice belongs in the UI layer.
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Binary search for a sport by name.
     * Requires: list already sorted by SortingAlgorithms.SPORT_BY_NAME.
     */
    public static List<Sport> binarySearchSportByName(List<Sport> sortedList, String name) {
        Sport dummy = new Sport(name, 0f);
        return binarySearch(sortedList, dummy, sports.sorting.SortingAlgorithms.SPORT_BY_NAME);
    }

    /**
     * Binary search for a club by name.
     * Requires: list already sorted by SortingAlgorithms.CLUB_BY_NAME.
     */
    public static List<Club> binarySearchClubByName(List<Club> sortedList, String name) {
        Club dummy = new Club(name, 0, null);
        return binarySearch(sortedList, dummy, sports.sorting.SortingAlgorithms.CLUB_BY_NAME);
    }

    /**
     * Binary search for a member by name.
     * Requires: list already sorted by SortingAlgorithms.MEMBER_BY_NAME.
     */
    public static List<Member> binarySearchMemberByName(List<Member> sortedList, String name) {
        Member dummy = new Member(name, 0, null);
        return binarySearch(sortedList, dummy, sports.sorting.SortingAlgorithms.MEMBER_BY_NAME);
    }

    /**
     * Binary search for a sport by reputation.
     * Requires: list already sorted by SortingAlgorithms.SPORT_BY_REPUTATION.
     */
    public static List<Sport> binarySearchSportByReputation(List<Sport> sortedList, float reputation) {
        Sport dummy = new Sport("", reputation);
        return binarySearch(sortedList, dummy, sports.sorting.SortingAlgorithms.SPORT_BY_REPUTATION);
    }

    /**
     * Binary search for a club by score.
     * Requires: list already sorted by SortingAlgorithms.CLUB_BY_SCORE.
     */
    public static List<Club> binarySearchClubByScore(List<Club> sortedList, int score) {
        Club dummy = new Club("", score, null);
        return binarySearch(sortedList, dummy, sports.sorting.SortingAlgorithms.CLUB_BY_SCORE);
    }

    /**
     * Binary search for a member by score.
     * Requires: list already sorted by SortingAlgorithms.MEMBER_BY_SCORE.
     */
    public static List<Member> binarySearchMemberByScore(List<Member> sortedList, int score) {
        Member dummy = new Member("", score, null);
        return binarySearch(sortedList, dummy, sports.sorting.SortingAlgorithms.MEMBER_BY_SCORE);
    }
}