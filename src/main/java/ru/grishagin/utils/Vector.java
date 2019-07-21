package ru.grishagin.utils;

import java.util.Objects;

public class Vector {
    public int x;
    public int y;

    public Vector(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public Vector copy(){
        return new Vector(x, y);
    }

    public double distance(Vector another){
        return Math.sqrt((this.x-another.x)*(this.x-another.x) + (this.y-another.y)*(this.y-another.y));
    }

    public Vector invert(){
        this.x = -x;
        this.y = -y;

        return this;
    }

    public static Vector sum(Vector one, Vector another){
        return new Vector(one.x + another.x, one.y + another.y);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Vector vector = (Vector) o;
        return x == vector.x &&
                y == vector.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public String toString() {
        return "{" + x + ", " + y + "}";
    }
}
