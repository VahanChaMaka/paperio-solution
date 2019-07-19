package ru.grishagin.model;

public enum Direction {
    UP("up"),
    DOWN("down"),
    LEFT("left"),
    RIGHT("right");

    private String value;

    Direction(String value) {
        this.value = value;
    }

    public String getValue(){
        return value;
    }
}
