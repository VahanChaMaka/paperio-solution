package ru.grishagin.strategy;

import ru.grishagin.model.Direction;
import ru.grishagin.model.Params;
import ru.grishagin.model.Player;
import ru.grishagin.utils.Helper;
import ru.grishagin.utils.Logger;
import ru.grishagin.utils.Vector;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static ru.grishagin.Const.I;

public class PotentialFieldStrategy extends StupidRandomStrategy {
    private static final int FIELD_SIZE = 17;
    private static final int FIELD_CENTER = 8;

    private static final int TAIL_PENALTY = -2;
    private static final int ENEMY_PENALTY = -4;
    private static final int ENEMY_TERRITORY_BONUS = 3;
    private static final int ENEMY_TAIL_BONUS = 5;
    private static final int BONUS_BONUS = 10;

    private static final int FROM_ME_DISTANCE_MODIFIER = 6;
    private static final int FROM_MY_CELL_DISTANCE_MODIFIER = 6;
    //private double[][] field = new double[FIELD_SIZE][FIELD_SIZE];
    private Deque<Vector> path = new LinkedList<>();

    public PotentialFieldStrategy(Params params) {
        super(params);
    }

    @Override
    protected Direction doSomething() {
        Logger.drawSnapshot(params);

        Vector currentPosition = me.getPosition();

        if(path.isEmpty()) {
            double[][] field = new double[FIELD_SIZE][FIELD_SIZE];
            for (int i = -FIELD_CENTER; i <= FIELD_CENTER; i++) {
                for (int j = -FIELD_CENTER; j <= FIELD_CENTER; j++) {
                    Vector cell = new Vector(currentPosition.x + i, currentPosition.y + j);
                    if (i == 0 && j == 0) {
                        field[i + FIELD_CENTER][j + FIELD_CENTER] = -99;
                        continue;
                    }

                    if (cell.x < 0 || cell.y < 0 || cell.x >= params.config.xSize || cell.y >= params.config.ySize) {
                        field[i + FIELD_CENTER][j + FIELD_CENTER] = -98; //skip walls
                        continue;
                    }

                    if (me.getTail().contains(cell)) {
                        field[i + FIELD_CENTER][j + FIELD_CENTER] = -11; //don't go to tail
                        continue;
                    }

                    //move from my territory
                    double distanceFromOwnCell = Double.POSITIVE_INFINITY;
                    for (Vector myCell : me.getTerritory()) {
                        double distance = myCell.distance(cell);
                        if( distance < distanceFromOwnCell){
                            distanceFromOwnCell = distance;
                        }
                    }
                    if (distanceFromOwnCell > FROM_MY_CELL_DISTANCE_MODIFIER/2) {
                        field[i + FIELD_CENTER][j + FIELD_CENTER] = (FROM_MY_CELL_DISTANCE_MODIFIER - distanceFromOwnCell)*0.1;
                    } else {
                        field[i + FIELD_CENTER][j + FIELD_CENTER] = (distanceFromOwnCell)*0.2;
                    }

                    boolean hasEnemyTail = false;
                    for (Map.Entry<String, Player> player : params.players.entrySet()) {
                        if(!player.getKey().equals(I)) {
                            if (player.getValue().getTerritory().contains(cell)) {//go to enemy's territory
                                field[i + FIELD_CENTER][j + FIELD_CENTER] += ENEMY_TERRITORY_BONUS;
                            }

                            //try to kill enemy
                            if(player.getValue().getTail().contains(cell)){
                                field[i + FIELD_CENTER][j + FIELD_CENTER] += ENEMY_TAIL_BONUS;
                                hasEnemyTail = true;
                            }

                            //don't come close to an enemy
                            double distanceToEnemy = player.getValue().getPosition().distance(cell);
                            if(distanceToEnemy < FIELD_CENTER){
                                field[i + FIELD_CENTER][j + FIELD_CENTER] += (1/(distanceToEnemy))*ENEMY_PENALTY;
                            }
                        }
                    }

                    //try to kill enemy on my territory or leave
                    if (!hasEnemyTail && get8neighbours(cell, me.getTerritory()) == 8) {
                        field[i + FIELD_CENTER][j + FIELD_CENTER] = -9.99;
                        continue;
                    }

                    //collect bonuses
                    for (Map<String, Vector> bonus : params.bonuses) {
                        for (Map.Entry<String, Vector> bonusEntry : bonus.entrySet()) {
                            if(!bonusEntry.getKey().equalsIgnoreCase("s")){//exept slow
                                field[i + FIELD_CENTER][j + FIELD_CENTER] += BONUS_BONUS;
                            }
                        }
                    }

                    //prefer closest to me best points
                    double distanceFromMe = currentPosition.distance(cell);
                    if (distanceFromMe > FROM_ME_DISTANCE_MODIFIER/2) {
                        field[i + FIELD_CENTER][j + FIELD_CENTER] += FROM_ME_DISTANCE_MODIFIER - distanceFromMe;
                    } else {
                        field[i + FIELD_CENTER][j + FIELD_CENTER] += distanceFromMe;
                    }


                    //don't come close to own tail
                    double tailSumCoeff = 0;
                    for (Vector tailCell : me.getTail()) {
                        double distance = tailCell.distance(cell);
                        tailSumCoeff = tailSumCoeff + TAIL_PENALTY/distance;
                    }
                    field[i + FIELD_CENTER][j + FIELD_CENTER] += tailSumCoeff;
                }
            }

            double maxValue = Double.NEGATIVE_INFINITY;
            Vector maxValueCoords = new Vector(0, 0);
            for (int i = -FIELD_CENTER; i <= FIELD_CENTER; i++) {
                for (int j = -FIELD_CENTER; j <= FIELD_CENTER; j++) {
                    if (field[i + FIELD_CENTER][j + FIELD_CENTER] > maxValue) {
                        maxValue = field[i + FIELD_CENTER][j + FIELD_CENTER];
                        maxValueCoords.setX(currentPosition.x + i);
                        maxValueCoords.setY(currentPosition.y + j);
                    }
                }
            }

            Logger.drawArray(field);

            path = bsf(currentPosition, maxValueCoords, I);

            Logger.log("c:" + currentPosition + ", target:" + maxValueCoords + ", current direction: " + me.getDirection());
            Logger.log(path.toString());
            if(path.isEmpty()){
                Logger.log("Path is empty! Do random");
                return super.doSomething();
            }
        }

        Vector nextMove = path.pollLast();
        //Vector nextMove = getNextMove(field);
        Logger.log("Current: " + currentPosition.toString() + ", next: " + nextMove.toString());
        if(!isValidMove(currentPosition, nextMove)){
            Logger.log("Wrong new direction! Cleaning path!");
            path.clear();
            return super.doSomething();
        } else {
            return Helper.convertToDirection(currentPosition, nextMove);
        }
    }

    private Vector getNextMove(double[][] field){
        Vector currentPosition = me.getPosition();
        double maxValue = Double.NEGATIVE_INFINITY;
        Vector maxValueCoords = new Vector(0,0);
        for (int i = FIELD_CENTER-1; i <= FIELD_CENTER+1; i++) {
            for (int j = FIELD_CENTER-1; j <=FIELD_CENTER+1 ; j++) {
                if(field[i][j] > maxValue  && (i == FIELD_CENTER || j == FIELD_CENTER)){
                    maxValueCoords.setX(currentPosition.x + i - FIELD_CENTER);
                    maxValueCoords.setY(currentPosition.y + j - FIELD_CENTER);
                    boolean not180Turn = !(currentPosition.x + maxValueCoords.x == 0 && currentPosition.y + maxValueCoords.y == 0);
                    if(not180Turn) {
                        maxValue = field[i][j];
                    }
                }
            }
        }
        return maxValueCoords;
    }

    @Override
    protected void reset() {
        path.clear();
    }
}
