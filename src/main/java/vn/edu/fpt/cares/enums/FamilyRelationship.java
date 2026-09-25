package vn.edu.fpt.cares.enums;

public enum FamilyRelationship {
    CHILD("Con"),
    SPOUSE("Vợ/chồng"),
    PARENT("Cha/mẹ"),
    SIBLING("Anh/chị/em"),
    GRANDPARENT("Ông/bà"),
    OTHER("Khác");

    private final String displayName;

    FamilyRelationship(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
