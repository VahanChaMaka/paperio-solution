package ru.grishagin.model;

public class Config {
    public final int MAX_TICKS = 1499;

    public final int speed;
    public final int xSize;
    public final int ySize;
    public final int cellSize;

    public Config(int speed, int xSize, int ySize, int cellSize) {
        this.speed = speed;
        this.xSize = xSize;
        this.ySize = ySize;
        this.cellSize = cellSize;
    }
}
