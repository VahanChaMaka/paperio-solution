package ru.grishagin.strategy;

import ru.grishagin.model.Direction;
import ru.grishagin.model.Params;
import ru.grishagin.model.Player;
import ru.grishagin.utils.Helper;
import ru.grishagin.utils.Logger;
import ru.grishagin.utils.Vector;

import java.util.*;
import java.util.stream.Collectors;

import static ru.grishagin.Const.I;

public class PotentialFieldStrategy extends StupidRandomStrategy {
    private static final long EXECUTION_TIME_RESTRICTION_MS = 100;

    private static final int FIELD_SIZE = 17;
    private static final int FIELD_CENTER = FIELD_SIZE/2;

    private static final int TAIL_PENALTY = -2;
    private static final int ENEMY_PENALTY = 400;
    private static final int ENEMY_TERRITORY_BONUS = 3;
    private static final int ENEMY_TAIL_BONUS = 10;
    private static final int BONUS_BONUS = 10;

    private static final int FROM_ME_DISTANCE_MODIFIER = 4;
    private static final int FROM_MY_CELL_DISTANCE_MODIFIER = 6;
    //private double[][] field = new double[FIELD_SIZE][FIELD_SIZE];
    private Deque<Vector> path = new LinkedList<>();
    private String threatenedBy = null;

    public PotentialFieldStrategy(Params params) {
        super(params);
    }

    @Override
    protected Direction doSomething() {
        Vector currentPosition = me.getPosition();
        threatenedBy = null;

        //i can leave my territory and get killed instantly
        if(!path.isEmpty() && me.getTerritory().contains(currentPosition)
                && !me.getTerritory().contains(path.getLast())){
            Logger.log("Leaving my territory, it's better to rebuild path");

            //int shortestEnemyPath = Integer.MAX_VALUE;
            for (Map.Entry<String, Player> player : params.players.entrySet()) {
                if(!player.getKey().equalsIgnoreCase(I)){
                    List<Vector> enemyPath = bsf(params, player.getValue().getPosition(), path.getLast(), player.getKey(), true).get(0);
                    if(enemyPath.size() < 4){
                        threatenedBy = player.getKey();
                    }
                }
            }

            path.clear();

        }

        if(path.isEmpty()) {
            List<Vector> fullyInternalTerritory = new LinkedList<Vector>();
            List<Vector> myBorders = new LinkedList<>();
            for (Vector cell : me.getTerritory()) {
                if (get8neighbours(cell, me.getTerritory()) == 8){
                    fullyInternalTerritory.add(cell);
                } else {
                    myBorders.add(cell);
                }
            }

            double[][] field = new double[FIELD_SIZE][FIELD_SIZE];
            for (int i = -FIELD_CENTER; i <= FIELD_CENTER; i++) {
                for (int j = -FIELD_CENTER; j <= FIELD_CENTER; j++) {
                    Vector cell = new Vector(currentPosition.x + i, currentPosition.y + j);
                    if (i == 0 && j == 0) {
                        field[i + FIELD_CENTER][j + FIELD_CENTER] = -99;
                        continue;
                    }

                    if (cell.x < 0 || cell.y < 0 || cell.x >= params.config.xSize-1 || cell.y >= params.config.ySize-1) {
                        field[i + FIELD_CENTER][j + FIELD_CENTER] = -98; //skip walls
                        continue;
                    }

                    if (me.getTail().contains(cell)) {
                        field[i + FIELD_CENTER][j + FIELD_CENTER] = -11; //don't go to tail
                        continue;
                    }

                    if(threatenedBy != null){
                        Vector distance = Vector.sum(me.getPosition().copy().invert(), params.getPlayer(threatenedBy).getPosition());
                        if(distance.x >= 0){
                            if(i >= distance.x){
                                field[i + FIELD_CENTER][j + FIELD_CENTER] = -50;
                                continue;
                            }
                        } else{
                            if(i <= distance.x){
                                field[i + FIELD_CENTER][j + FIELD_CENTER] = -50;
                                continue;
                            }
                        }

                        if(distance.y >= 0){
                            if(j >= distance.y){
                                field[i + FIELD_CENTER][j + FIELD_CENTER] = -50;
                                continue;
                            }
                        } else{
                            if(j <= distance.y){
                                field[i + FIELD_CENTER][j + FIELD_CENTER] = -50;
                                continue;
                            }
                        }
                    }

                    boolean hasEnemyTail = false;
                    boolean isEnemyTerritory = false;
                    for (Map.Entry<String, Player> player : params.players.entrySet()) {
                        if(!player.getKey().equals(I)) {
                            if (player.getValue().getTerritory().contains(cell)) {//go to enemy's territory
                                field[i + FIELD_CENTER][j + FIELD_CENTER] += ENEMY_TERRITORY_BONUS;
                                isEnemyTerritory = true;
                            }

                            //try to kill enemy
                            if(player.getValue().getTail().contains(cell)){
                                field[i + FIELD_CENTER][j + FIELD_CENTER] += ENEMY_TAIL_BONUS;
                                hasEnemyTail = true;
                            }

                            //don't come close to an enemy
                            double distanceToEnemy = player.getValue().getPosition().distance(cell);
                            if(distanceToEnemy < 1){
                                distanceToEnemy = 1;
                            }
                            if(distanceToEnemy < 5){
                                field[i + FIELD_CENTER][j + FIELD_CENTER] -= Math.log((1/(distanceToEnemy-1))*ENEMY_PENALTY);
                            }
                        }
                    }

                    if(!isEnemyTerritory){//if it's free
                        //move from my territory
                        double distanceFromOwnCell = Double.POSITIVE_INFINITY;
                        for (Vector myCell : me.getTerritory()) {
                            double distance = myCell.distance(cell);
                            if( distance < distanceFromOwnCell){
                                distanceFromOwnCell = distance;
                            }
                        }
                        if (distanceFromOwnCell > FROM_MY_CELL_DISTANCE_MODIFIER/2) {
                            field[i + FIELD_CENTER][j + FIELD_CENTER] += (FROM_MY_CELL_DISTANCE_MODIFIER - distanceFromOwnCell)*0.1;
                        } else {
                            field[i + FIELD_CENTER][j + FIELD_CENTER] += (distanceFromOwnCell)*0.2;
                        }
                    }

                    //try to kill enemy on my territory or leave
                    if (!hasEnemyTail && fullyInternalTerritory.contains(cell)) {
                        field[i + FIELD_CENTER][j + FIELD_CENTER] += -4;

                        //don't go too deep inside my territory
                        double closestBorderDistance = Double.POSITIVE_INFINITY;
                        for (Vector myBorderCell : myBorders) {
                            double distance = cell.distance(myBorderCell);
                            if(distance < closestBorderDistance){
                                closestBorderDistance = distance;
                            }
                        }
                        field[i + FIELD_CENTER][j + FIELD_CENTER] += -closestBorderDistance;
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
                        maxValueCoords.setX(i + FIELD_CENTER);
                        maxValueCoords.setY(j + FIELD_CENTER);
                    }
                }
            }

            Logger.drawArray(field);

            //bsf(params, currentPosition, maxValueCoords, I, false)

            List<Deque<Vector>> paths = new LinkedList<>();
            Deque<Vector> initialPath = new LinkedList<>();
            initialPath.offer(new Vector(8, 8));
            paths.add(initialPath);
            paths = getAllPaths(maxValueCoords, paths, field, System.currentTimeMillis())
                    .stream()
                    .filter(path -> path.contains(maxValueCoords)).peek(Deque::poll)
                    .collect(Collectors.toList());
            double evaluation = Double.NEGATIVE_INFINITY;
            for (Deque<Vector> path : paths) {
                double tmpEvaluation = 0;
                for (Vector vector : path) {
                    int i = vector.x;
                    int j = vector.y;
                    if(i < FIELD_SIZE && j < FIELD_SIZE && i >= 0 && j >= 0) {
                        tmpEvaluation = tmpEvaluation + field[i][j];
                    } else {//don't use paths outside potential fields
                      tmpEvaluation = Double.NEGATIVE_INFINITY;
                      break;
                    }
                }

                if(tmpEvaluation >= evaluation){
                    evaluation = tmpEvaluation;
                    this.path = path.stream().peek(cell -> {
                        cell.setX(cell.x + currentPosition.x - FIELD_CENTER);
                        cell.setY(cell.y + currentPosition.y - FIELD_CENTER);
                    }).collect(Collectors.toCollection(LinkedList::new));
                }
            }

            Logger.log("Target (in field):" + maxValueCoords + ", current direction: " + me.getDirection());

            if(path.isEmpty()){
                Logger.log("Failed to build optimal path, making shortest");
                maxValueCoords.setX(maxValueCoords.x - FIELD_CENTER + currentPosition.x);
                maxValueCoords.setY(maxValueCoords.y - FIELD_CENTER + currentPosition.y);
                List<Vector> fastestPath = bsf(params, currentPosition, maxValueCoords, I, false).get(0);
                Collections.reverse(fastestPath);
                path = new LinkedList<>(fastestPath);
            }

            Logger.log(path.toString());
            if(path.isEmpty()){
                Logger.log("Path is empty! Do random");
                return super.doSomething();
            }
        }

        Vector nextMove = path.poll();
        //Vector nextMove = getNextMove(field);
        Logger.log("Current: " + currentPosition.toString() + ", next: " + nextMove.toString());
        if(!isValidMove(params, currentPosition, nextMove)){
            Logger.log("Wrong new direction! Cleaning path!");
            path.clear();
            return super.doSomething();
        } else {
            try {
                return Helper.convertToDirection(currentPosition, nextMove);
            } catch (IllegalArgumentException e){
                Logger.log(e.getMessage());
                for (StackTraceElement stackTraceElement : e.getStackTrace()) {
                    Logger.log(stackTraceElement.toString());
                }
                return super.doSomething();
            }
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

    //start/end point in terms of i j in array. Path also contains i j
    private List<Deque<Vector>> getAllPaths(Vector endPoint, List<Deque<Vector>> allPaths, double[][] field, long startTime){
        for (int k = 0; k < allPaths.size(); k++) {
            Deque<Vector> path = allPaths.get(k);
            if(path.size() > 10 || System.currentTimeMillis() - startTime > EXECUTION_TIME_RESTRICTION_MS){
                return allPaths;
            }

            Vector lastCell = path.peekLast();
            if (lastCell.equals(endPoint)){
                return allPaths;
            }

            for (int i = lastCell.x - 1; i <= lastCell.x + 1; i++) {
                if (i < 0 || i >= field.length) {
                    continue;
                }
                for (int j = lastCell.y - 1; j <= lastCell.y + 1; j++) {
                    if (j < 0 || j >= field[i].length) {
                        continue;
                    }

                    if (!(i == lastCell.x && j == lastCell.y) && (i == lastCell.x || j == lastCell.y)) {
                        Vector neighbour = new Vector(i, j);

                        Vector movingTo = Helper.convertToIndexes(me.getDirection()).invert();
                        Vector excludeCellBehind = null;
                        if(path.size() == 1) {
                           excludeCellBehind = new Vector(8 + movingTo.x, 8 + movingTo.y);
                        }
                        if (!path.contains(neighbour) && !neighbour.equals(excludeCellBehind)) {
                            Deque<Vector> pathCopy = new LinkedList<>();
                            for (Vector pathElement : path) {
                                pathCopy.offer(pathElement);
                            }
                            allPaths.add(pathCopy);
                            pathCopy.add(neighbour);
                            boolean isFullCopy = false;
                            for (Deque<Vector> oneOfPaths : allPaths) {
                                if (oneOfPaths.equals(pathCopy)){
                                    isFullCopy = true;
                                }
                            }

                            if (!isFullCopy) {
                                getAllPaths(endPoint, allPaths, field, startTime);
                            }
                        }
                    }
                }
            }
        }
        return allPaths;
    }

    @Override
    protected void reset() {
        path.clear();
    }
}
