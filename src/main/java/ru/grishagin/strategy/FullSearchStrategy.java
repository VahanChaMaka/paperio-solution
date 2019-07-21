package ru.grishagin.strategy;

import ru.grishagin.model.Direction;
import ru.grishagin.model.Params;
import ru.grishagin.model.Player;
import ru.grishagin.utils.Helper;
import ru.grishagin.utils.Logger;
import ru.grishagin.utils.Vector;

import javax.swing.text.Position;
import java.util.*;

import static ru.grishagin.Const.I;

public class FullSearchStrategy implements Strategy {
    Params params;
    Player me;

    private boolean isRetreating = false;
    private Deque<Vector> retreatingPath = new LinkedList<>();

    public FullSearchStrategy(Params params) {
        this.params = params;
    }

    @Override
    public Direction onTick() {
        me = params.getPlayer(I);

        checkDanger();

        if(isRetreating){
            if(!retreatingPath.isEmpty()) {
                return Helper.convertToDirection(me.getPosition(), retreatingPath.pollLast());
            } else {
                isRetreating = false;
            }
        }

        return doSomething();
    }

    private void checkDanger(){
        if(me.getTerritory().contains(me.getPosition())){
            return;
        }

        Deque<Vector> pathToHome = buildFastestPath(TargetType.TERRITORY, I, I);
        Logger.log(pathToHome.toString());
        for (Map.Entry<String, Player> playerEntry : params.players.entrySet()) {
            if(!playerEntry.getKey().equals(I)) {//skip self
                if (buildFastestPath(TargetType.TAIL, playerEntry.getKey(), I).size() <= pathToHome.size() + 1) {
                    retreatingPath = pathToHome;
                    isRetreating = true;
                    Logger.log("I'm in danger!");
                    return;
                }
            }
        }
    }

    private Direction doSomething(){
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

    private Deque<Vector> buildFastestPath(TargetType type, String sourceId, String targetId){
        Vector sourcePosition = params.getPlayer(sourceId).getPosition();
        double shortestRay = Double.MAX_VALUE;
        List<Vector> targetCells = null;
        switch (type){
            case TERRITORY:
                targetCells = params.getPlayer(targetId).getTerritory();
                break;
            case TAIL:
                targetCells = params.getPlayer(targetId).getTail();
                break;
        }

        Vector movingTo = Helper.convertToIndexes(params.getPlayer(sourceId).getDirection()).invert();
        targetCells.remove(Vector.sum(movingTo, params.getPlayer(sourceId).getPosition()));

        Vector closestTargetCell = null;
        for (Vector cell : targetCells) {
            if(shortestRay > cell.distance(sourcePosition)){
                shortestRay = cell.distance(sourcePosition);
                closestTargetCell = cell;
            }
        }

        Deque<Vector> path = bsf(sourcePosition, closestTargetCell, sourceId);

        return path;
    }

    private LinkedList<Vector> bsf(Vector startPoint, Vector endPoint, String playerId){
        Vector movingTo = Helper.convertToIndexes(params.getPlayer(playerId).getDirection()).invert();
        Vector excludeCellBehind = Vector.sum(params.getPlayer(playerId).getPosition(), movingTo);

        LinkedList<Vector> path = new LinkedList<>();
        Queue<Vector> queue = new LinkedList<>();
        Map<Vector, Vector> visited = new HashMap<>(); //nextCell -> cameFrom (key->value)
        queue.offer(startPoint);

        while (!queue.isEmpty()){
            Vector current = queue.poll();
            if (current.equals(endPoint)) {//path was found
                path.add(endPoint);
                Vector cameFrom = visited.get(current);
                while (!cameFrom.equals(startPoint)){//return to start by path
                    path.add(cameFrom);
                    cameFrom = visited.get(cameFrom);
                }
                return path;
            } else {
                for (int i = current.x-1; i <= current.x+1; i++) {
                    for (int j = current.y - 1; j <= current.y + 1; j++) {
                        if(i == current.x || j == current.y){ //connection 4 like +
                            Vector neighbour = new Vector(i, j);
                            if(!visited.containsKey(neighbour)
                                    && !neighbour.equals(excludeCellBehind) //cannot turn 180 degrees
                                    && !params.getPlayer(playerId).getTail().contains(neighbour)) {//do not go through tail
                                visited.put(neighbour, current);
                                queue.offer(neighbour);
                            }
                        }
                    }
                }
            }
        }
        return path;
    }
}
