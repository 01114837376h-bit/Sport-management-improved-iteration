package sports.sorting;

import java.util.Comparator;
import java.util.List;

/**
 * SortingAlgorithms — all in-place sort implementations + shared comparators.
 *
 * Comparators are declared first so they are visible to both the sort methods
 * and every external caller without scrolling past implementation details.
 *
 * Naming convention
 * ─────────────────
 *   X_BY_NAME        → ascending A→Z  (case-insensitive)
 *   X_BY_SCORE       → descending (highest score first)
 *   X_BY_REPUTATION  → descending (highest reputation first)
 *
 * Descending direction is intentional and is explicit in the Javadoc so
 * callers are never surprised by the sort order.
 *
 * Binary-search contract
 * ──────────────────────
 * Pass the SAME comparator to both the sort call and the subsequent
 * SearchEngine.binarySearch() call. Using a different comparator for the
 * search than was used for the sort produces undefined results.
 */
public class SortingAlgorithms {

    // ═══════════════════════════════════════════════════════════════════════════
    //  COMPARATORS
    // ═══════════════════════════════════════════════════════════════════════════

    // ── Sport ──────────────────────────────────────────────────────────────────

    /** Ascending by sport name, case-insensitive. */
    public static final Comparator<sports.entities.Sport> SPORT_BY_NAME =
            (a, b) -> a.getSportName().compareToIgnoreCase(b.getSportName());

    /** Descending by reputation — highest reputation first. */
    public static final Comparator<sports.entities.Sport> SPORT_BY_REPUTATION =
            (a, b) -> Float.compare(b.getReputation(), a.getReputation());

    // ── Club ───────────────────────────────────────────────────────────────────

    /** Ascending by club name, case-insensitive. */
    public static final Comparator<sports.entities.Club> CLUB_BY_NAME =
            (a, b) -> a.getClubName().compareToIgnoreCase(b.getClubName());

    /** Descending by club score — highest score first. */
    public static final Comparator<sports.entities.Club> CLUB_BY_SCORE =
            (a, b) -> Integer.compare(b.getClubScore(), a.getClubScore());

    // ── Member ─────────────────────────────────────────────────────────────────

    /** Ascending by member name, case-insensitive. */
    public static final Comparator<sports.entities.Member> MEMBER_BY_NAME =
            (a, b) -> a.getMemberName().compareToIgnoreCase(b.getMemberName());

    /** Descending by member score — highest score first. */
    public static final Comparator<sports.entities.Member> MEMBER_BY_SCORE =
            (a, b) -> Integer.compare(b.getScoring(), a.getScoring());

    // ═══════════════════════════════════════════════════════════════════════════
    //  MERGE SORT  —  O(n log n), stable
    // ═══════════════════════════════════════════════════════════════════════════

    public static <T> void mergeSort(List<T> list, Comparator<T> cmp) {
        if (list == null || list.size() <= 1) return;
        mergeSortHelper(list, 0, list.size() - 1, cmp);
    }

    private static <T> void mergeSortHelper(List<T> list, int left, int right,
                                            Comparator<T> cmp) {
        if (left >= right) return;
        int mid = left + (right - left) / 2;
        mergeSortHelper(list, left,      mid,   cmp);
        mergeSortHelper(list, mid + 1, right,   cmp);
        merge(list, left, mid, right, cmp);
    }

    private static <T> void merge(List<T> list, int left, int mid, int right,
                                  Comparator<T> cmp) {
        int leftLen  = mid - left + 1;
        int rightLen = right - mid;

        @SuppressWarnings("unchecked") T[] L = (T[]) new Object[leftLen];
        @SuppressWarnings("unchecked") T[] R = (T[]) new Object[rightLen];

        for (int i = 0; i < leftLen;  i++) L[i] = list.get(left + i);
        for (int j = 0; j < rightLen; j++) R[j] = list.get(mid + 1 + j);

        int i = 0, j = 0, k = left;
        while (i < leftLen && j < rightLen) {
            if (cmp.compare(L[i], R[j]) <= 0) list.set(k++, L[i++]);
            else                               list.set(k++, R[j++]);
        }
        while (i < leftLen)  list.set(k++, L[i++]);
        while (j < rightLen) list.set(k++, R[j++]);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  SELECTION SORT  —  O(n²), in-place, not stable
    // ═══════════════════════════════════════════════════════════════════════════

    public static <T> void selectionSort(List<T> list, Comparator<T> cmp) {
        if (list == null || list.size() <= 1) return;
        int n = list.size();
        for (int i = 0; i < n - 1; i++) {
            int minIdx = i;
            for (int j = i + 1; j < n; j++) {
                if (cmp.compare(list.get(j), list.get(minIdx)) < 0) minIdx = j;
            }
            if (minIdx != i) {
                T tmp = list.get(i);
                list.set(i, list.get(minIdx));
                list.set(minIdx, tmp);
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  QUICK SORT  —  O(n log n) average, in-place, not stable
    // ═══════════════════════════════════════════════════════════════════════════

    public static <T> void quickSort(List<T> list, Comparator<T> cmp) {
        if (list == null || list.size() <= 1) return;
        quickSortHelper(list, 0, list.size() - 1, cmp);
    }

    private static <T> void quickSortHelper(List<T> list, int low, int high,
                                            Comparator<T> cmp) {
        if (low >= high) return;
        int pi = partition(list, low, high, cmp);
        quickSortHelper(list, low,    pi - 1, cmp);
        quickSortHelper(list, pi + 1, high,   cmp);
    }

    private static <T> int partition(List<T> list, int low, int high,
                                     Comparator<T> cmp) {
        T pivot = list.get(high);
        int i   = low - 1;
        for (int j = low; j < high; j++) {
            if (cmp.compare(list.get(j), pivot) <= 0) {
                i++;
                T tmp = list.get(i); list.set(i, list.get(j)); list.set(j, tmp);
            }
        }
        T tmp = list.get(i + 1); list.set(i + 1, list.get(high)); list.set(high, tmp);
        return i + 1;
    }
}