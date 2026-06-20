package sports.entities;

import java.util.ArrayList;
import java.util.List;

public class Club {
    private static int idCounter = 1;

    private int clubID;
    private String clubName;
    private int clubScore;
    private Sport parentSport;
    private List<Member> members;  // ordered list of members in this club

    public Club(String clubName, int clubScore, Sport parentSport) {
        this.clubID = idCounter++;
        this.clubName = clubName;
        this.clubScore = clubScore;
        this.parentSport = parentSport;
        this.members = new ArrayList<>();
    }

    public Club(int clubID, String clubName, int clubScore, Sport parentSport) {
        this.clubID = clubID;
        this.clubName = clubName;
        this.clubScore = clubScore;
        this.parentSport = parentSport;
        this.members = new ArrayList<>();
        if (clubID >= idCounter) idCounter = clubID + 1;
    }

    // ── Member management ──────────────────────────────────────────────────────

    public void addMember(Member m) {
        if (!members.contains(m)) members.add(m);
    }

    public void removeMember(Member m) {
        members.remove(m);
    }

    /** Returns a copy so callers can sort/search without mutating the live list. */
    public List<Member> getMembersCopy() {
        return new ArrayList<>(members);
    }

    public List<Member> getMembers() { return members; }

    // ── Getters / Setters ──────────────────────────────────────────────────────

    public int getClubID()              { return clubID; }
    public String getClubName()         { return clubName; }
    public int getClubScore()           { return clubScore; }
    public Sport getParentSport()       { return parentSport; }

    public void setClubName(String n)   { this.clubName = n; }
    public void setClubScore(int s)     { this.clubScore = s; }
    public void setParentSport(Sport s) { this.parentSport = s; }

    @Override
    public String toString() {
        return String.format("Club[ID=%d, Name=%s, Score=%d, Sport=%s, Members=%d]",
                clubID, clubName, clubScore,
                parentSport != null ? parentSport.getSportName() : "None",
                members.size());
    }
}
