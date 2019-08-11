package ru.grishagin.model;

import ru.grishagin.utils.Vector;

import java.util.LinkedList;
import java.util.List;

public class Player {
    private String id;
    private int score;
    private List<Vector> territory;
    private Vector position;
    private Direction direction;
    private List<Vector> tail;
    private List<Bonus> activeBonuses;
    private int speed;

    public Player(String id,
                  int score,
                  List<Vector> territory,
                  Vector position,
                  Direction direction,
                  List<Vector> tail,
                  List<Bonus> activeBonuses,
                  int speed) {
        this.id = id;
        this.score = score;
        this.territory = territory;
        this.position = position;
        this.direction = direction;
        this.tail = tail;
        this.activeBonuses = activeBonuses;
        this.speed = speed;
    }

    public String getId() {
        return id;
    }

    public int getScore() {
        return score;
    }

    public List<Vector> getTerritory() {
        return territory;
    }

    public Vector getPosition() {
        return position;
    }

    public List<Vector> getTail() {
        return tail;
    }

    public List<Bonus> getActiveBonuses() {
        return activeBonuses;
    }

    public Direction getDirection() {
        return direction;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public void setPosition(Vector position) {
        this.position = position;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
    }

    public Player copy(){
        List<Vector> newTerritory = new LinkedList<>();
        for (Vector cell : territory) {
            newTerritory.add(cell.copy());
        }

        List<Vector> newTail = new LinkedList<>();
        for (Vector cell : tail) {
            newTail.add(cell.copy());
        }

        List<Bonus> newBonuses = new LinkedList<>();
        for (Bonus activeBonus : activeBonuses) {
            newBonuses.add(activeBonus.copy());
        }

        return new Player(this.id,
                this.score,
                newTerritory,
                this.position.copy(),
                this.direction,
                newTail,
                newBonuses,
                this.speed);
    }
}
