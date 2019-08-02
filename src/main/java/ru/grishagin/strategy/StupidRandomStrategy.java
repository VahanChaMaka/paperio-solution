package ru.grishagin.strategy;

import ru.grishagin.model.Direction;
import ru.grishagin.model.Params;
import ru.grishagin.utils.Helper;
import ru.grishagin.utils.Vector;

public class StupidRandomStrategy extends BasicStrategy {

    public StupidRandomStrategy(Params params) {
        super(params);
    }

    @Override
    protected Direction doSomething() {
        boolean isCorrectMove = false;
        Direction newDirection = null;
        while (!isCorrectMove) {
            int rand = (int) (Math.random() * 4);
            if (rand == 0) {
                newDirection = Direction.LEFT;
            } else if (rand == 1) {
                newDirection = Direction.DOWN;
            } else if (rand == 2) {
                newDirection = Direction.RIGHT;
            } else {
                newDirection = Direction.UP;
            }

            Vector oldPosition = me.getPosition();
            Vector nextPosition = Vector.sum(oldPosition, Helper.convertToIndexes(newDirection));

            isCorrectMove = isValidMove(oldPosition, nextPosition);
        }
        return newDirection;
    }
}
