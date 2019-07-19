package ru.grishagin.model;

public class Bonus {
    public final BonusType type;
    public final int duration;

    public Bonus(BonusType type, int duration) {
        this.type = type;
        this.duration = duration;
    }

    public enum BonusType {
        NITRO("n"),
        SLOW("s"),
        SAW("saw");

        private String value;

        BonusType(String value) {
            this.value = value;
        }

        public String getValue(){
            return value;
        }
    }
}
