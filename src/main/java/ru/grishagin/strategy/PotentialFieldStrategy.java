package ru.grishagin.strategy;

import ru.grishagin.model.Direction;
import ru.grishagin.model.Params;
import ru.grishagin.utils.Helper;
import ru.grishagin.utils.Logger;
import ru.grishagin.utils.Vector;

import java.util.LinkedList;
import java.util.List;

import static ru.grishagin.Const.I;

public class PotentialFieldStrategy extends StupidRandomStrategy {
    private static final int FIELD_SIZE = 17;
    private static final int FIELD_CENTER = 8;
    private double[][] field = new double[FIELD_SIZE][FIELD_SIZE];

    public PotentialFieldStrategy(Params params) {
        super(params);
    }

    @Override
    protected Direction doSomething() {
        Logger.drawSnapshot(params);

        Vector currentPosition = me.getPosition();

        for (int i = -FIELD_CENTER; i <= FIELD_CENTER; i++) {
            for (int j = -FIELD_CENTER; j <= FIELD_CENTER; j++) {
                Vector cell = new Vector(currentPosition.x + i, currentPosition.y + j);
                if(i == 0 && j == 0){
                    field[i+FIELD_CENTER][j+FIELD_CENTER] = -99;
                    continue;
                }

                if(cell.x < 0 || cell.y < 0 || cell.x >= params.config.xSize || cell.y >= params.config.ySize){
                    field[i+FIELD_CENTER][j+FIELD_CENTER] = -98; //skip walls
                    continue;
                }

                if (get8neighbours(cell, me.getTerritory()) == 8) {
                    field[i+FIELD_CENTER][j+FIELD_CENTER] = -10; //force to move out from my territory
                    continue;
                }

                if(me.getTail().contains(cell)){
                    field[i+FIELD_CENTER][j+FIELD_CENTER] = -11; //don't go to tail
                    continue;
                }

                double distance = Math.sqrt((currentPosition.x - (currentPosition.x + i))*(currentPosition.x - (currentPosition.x + i))
                        + (currentPosition.y - (currentPosition.y + j))*(currentPosition.y - (currentPosition.y + j)));
                if(distance > 3){
                    field[i+FIELD_CENTER][j+FIELD_CENTER] = 6 - distance;
                } else {
                    field[i+FIELD_CENTER][j+FIELD_CENTER] = distance;
                }
            }
        }

        double maxValue = Double.NEGATIVE_INFINITY;
        Vector maxValueCoords = new Vector(0,0);
        for (int i = -FIELD_CENTER; i <= FIELD_CENTER; i++) {
            for (int j = -FIELD_CENTER; j <= FIELD_CENTER; j++) {
                if(field[i + FIELD_CENTER][j + FIELD_CENTER] > maxValue){
                    maxValue = field[i + FIELD_CENTER][j + FIELD_CENTER];
                    maxValueCoords.setX(currentPosition.x + i);
                    maxValueCoords.setY(currentPosition.y + j);
                }
            }
        }

        Logger.drawArray(field);

        LinkedList<Vector> path = bsf(currentPosition, maxValueCoords, I);
        Logger.log("c:" + currentPosition + ", next:" + maxValueCoords);
        Logger.log(path.toString());
        Vector nextMove = path.getLast();
        if(!isValidMove(currentPosition, nextMove)){
            Logger.log("Wrong new direction");
            return super.doSomething();
        } else {
            return Helper.convertToDirection(currentPosition, nextMove);
        }
    }

}
