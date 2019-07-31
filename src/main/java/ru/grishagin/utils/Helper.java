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
                Logger.log("Warning! Wrong direction"); //lol what
                indexes = new Vector(0, 0);
        }
        return indexes;
    }

    public static Direction convertToDirection(Vector position, Vector destination){
        int deltaX = destination.x - position.x;
        int deltaY = destination.y - position.y;

        if(deltaX != 0 && deltaY != 0 || deltaX > 1 || deltaX < -1 || deltaY > 1 || deltaY < -1 || (deltaX == 0 && deltaY == 0)){
            throw new IllegalArgumentException("Trying to calculate direction between wrong cells!" +
                    "\nPosition: " + position +
                    "\nDestination: " + destination);
        }

        if(deltaX == 1){
            return Direction.RIGHT;
        } else if(deltaX == -1){
            return Direction.LEFT;
        } else if(deltaY == 1){
            return Direction.UP;
        } else if(deltaY == -1){
            return Direction.DOWN;
        } else {
            throw new IllegalArgumentException();
        }
    }
}
