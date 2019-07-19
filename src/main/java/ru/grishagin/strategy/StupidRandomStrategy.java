package ru.grishagin.strategy;

import ru.grishagin.utils.Helper;
import ru.grishagin.utils.Logger;
import ru.grishagin.utils.Vector;
import ru.grishagin.model.Direction;
import ru.grishagin.model.Params;
import ru.grishagin.model.Player;

import static ru.grishagin.Const.I;

public class StupidRandomStrategy implements Strategy {

    Params params;
    Player me;

    public StupidRandomStrategy(Params params) {
        this.params = params;
    }

    @Override
    public Direction onTick() {
        me = params.getPlayer(I);
        Logger.getInstance().log("Tail: " + me.getTail().toString());

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

            Vector oldMove = Helper.convertToIndexes(me.getDirection());
            Vector nextMove = Helper.convertToIndexes(newDirection);
            if(oldMove.x + nextMove.x == 0 && oldMove.y + nextMove.y == 0){
                continue;
            }

            Vector positionAfterMove = new Vector(me.getPosition().x + nextMove.x,
                    me.getPosition().y + nextMove.y);

            boolean isHitsSelf = false;
            for (Vector position : me.getTail()) {
                if(position.equals(positionAfterMove)){
                    isHitsSelf = true;
                    break;
                }
            }

            isCorrectMove = !isHitsSelf;
                    //&& me.[positionAfterMove.x][positionAfterMove.y] != WALL;
        }
        return newDirection;
    }
}
