package ru.grishagin.model;

public class Bonus {
    public final BonusType type;
    public final int duration;

    public Bonus(BonusType type, int duration) {
        this.type = type;
        this.duration = duration;
    }

    public static BonusType convertToType(String value){
        switch (value){
            case "s":
            case "S":
                return BonusType.SLOW;
            case "n":
            case "N":
                return BonusType.NITRO;
            case "saw":
            case "SAW":
                return BonusType.SAW;
            default:
                throw new IllegalArgumentException("Unknown bonus type " + value);
        }
    }

    public enum BonusType {
        NITRO,
        SLOW,
        SAW;
    }
}
