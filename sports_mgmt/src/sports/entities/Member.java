package sports.entities;

public class Member {
    private static int idCounter = 1;

    private int memberID;
    private String memberName;
    private int scoring;
    private Club parentClub;

    public Member(String memberName, int scoring, Club parentClub) {
        this.memberID = idCounter++;
        this.memberName = memberName;
        this.scoring = scoring;
        this.parentClub = parentClub;
    }

    // Used when loading/restoring with known ID
    public Member(int memberID, String memberName, int scoring, Club parentClub) {
        this.memberID = memberID;
        this.memberName = memberName;
        this.scoring = scoring;
        this.parentClub = parentClub;
        if (memberID >= idCounter) idCounter = memberID + 1;
    }

    public int getMemberID()            { return memberID; }
    public String getMemberName()       { return memberName; }
    public int getScoring()             { return scoring; }
    public Club getParentClub()         { return parentClub; }

    public void setMemberName(String n) { this.memberName = n; }
    public void setScoring(int s)       { this.scoring = s; }
    public void setParentClub(Club c)   { this.parentClub = c; }

    @Override
    public String toString() {
        return String.format("Member[ID=%d, Name=%s, Score=%d, Club=%s]",
                memberID, memberName, scoring,
                parentClub != null ? parentClub.getClubName() : "None");
    }
}
