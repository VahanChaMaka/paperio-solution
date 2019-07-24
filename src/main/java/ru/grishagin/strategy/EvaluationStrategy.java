package ru.grishagin.strategy;

import ru.grishagin.model.Direction;
import ru.grishagin.utils.Vector;

public class EvaluationStrategy implements Strategy {

    private static final int DEPTH = 100;

    @Override
    public Direction onTick() {
        return null;
    }

    private int evaluate(Vector position, int step){
        return 0;
    }
}
