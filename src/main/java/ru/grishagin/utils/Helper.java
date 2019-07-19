package ru.grishagin.utils;

import ru.grishagin.model.Direction;

public class Helper {

    public static Vector convertToIndexes(Direction direction){
        Vector indexes;
        switch (direction) {
            case UP:
                indexes = new Vector(0, 1);
                break;
            case LEFT:
                indexes = new Vector(-1, 0);
                break;
            case DOWN:
                indexes = new Vector(0, -1);
                break;
            case RIGHT:
                indexes = new Vector(1, 0);
                break;
            default:
                System.out.println("Warning! Wrong direction"); //lol what
                indexes = new Vector(0, 0);
        }
        return indexes;
    }
}
