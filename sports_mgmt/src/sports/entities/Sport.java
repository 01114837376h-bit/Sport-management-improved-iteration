package sports.entities;

import java.util.ArrayList;
import java.util.List;

public class Sport {
    private static int idCounter = 1;

    private int sportID;
    private String sportName;
    private float reputation;
    private List<Club> clubs;   // ordered list of clubs in this sport

    public Sport(String sportName, float reputation) {
        this.sportID = idCounter++;
        this.sportName = sportName;
        this.reputation = reputation;
        this.clubs = new ArrayList<>();
    }

    public Sport(int sportID, String sportName, float reputation) {
        this.sportID = sportID;
        this.sportName = sportName;
        this.reputation = reputation;
        this.clubs = new ArrayList<>();
        if (sportID >= idCounter) idCounter = sportID + 1;
    }

    // ── Club management ────────────────────────────────────────────────────────

    public void addClub(Club c) {
        if (!clubs.contains(c)) clubs.add(c);
    }

    public void removeClub(Club c) {
        clubs.remove(c);
    }

    /** Returns a copy so callers can sort/search without mutating the live list. */
    public List<Club> getClubsCopy() {
        return new ArrayList<>(clubs);
    }

    public List<Club> getClubs() { return clubs; }

    // ── Getters / Setters ──────────────────────────────────────────────────────

    public int getSportID()               { return sportID; }
    public String getSportName()          { return sportName; }
    public float getReputation()          { return reputation; }

    public void setSportName(String n)    { this.sportName = n; }
    public void setReputation(float r)    { this.reputation = r; }

    @Override
    public String toString() {
        return String.format("Sport[ID=%d, Name=%s, Reputation=%.1f, Clubs=%d]",
                sportID, sportName, reputation, clubs.size());
    }
}
