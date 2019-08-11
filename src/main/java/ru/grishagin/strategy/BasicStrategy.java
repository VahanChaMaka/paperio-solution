package ru.grishagin.strategy;

import ru.grishagin.model.Direction;
import ru.grishagin.model.Params;
import ru.grishagin.model.Player;
import ru.grishagin.utils.Helper;
import ru.grishagin.utils.Logger;
import ru.grishagin.utils.Vector;

import java.util.*;

import static ru.grishagin.Const.I;

public abstract class BasicStrategy implements Strategy {
    private static final int HOME_PATH_SAFETY_INCREMENT = 3; //add to my home path size to ensure I will be home in time

    Params params;
    Player me;

    private boolean isRetreating = false;
    private Deque<Vector> retreatingPath = new LinkedList<>();

    public BasicStrategy(Params params) {
        this.params = params;
    }

    @Override
    public Direction onTick() {
        me = params.getPlayer(I);

        Logger.drawSnapshot(params);
        Logger.log("Current position: " + me.getPosition());

        Direction nextMove = doSomething();
        Params newState = Helper.makeStep(params, I, nextMove);
        if(!me.getTerritory().contains(me.getPosition()) && checkDanger(newState)){
            retreatingPath = buildFastestPath(params, TargetType.TERRITORY, I, I);
            isRetreating = true;
        }

        if(isRetreating){
            if(!retreatingPath.isEmpty()) {
                reset();
                return Helper.convertToDirection(me.getPosition(), retreatingPath.pollLast());
            } else {
                isRetreating = false;
            }
        }

        return nextMove;
    }

    private boolean checkDanger(Params state){
        if(state.getPlayer(I).getTerritory().contains(state.getPlayer(I).getPosition())){
            return false;
        }

        Deque<Vector> pathToHome = buildFastestPath(state, TargetType.TERRITORY, I, I);
        Logger.log(pathToHome.toString());
        for (Map.Entry<String, Player> playerEntry : state.players.entrySet()) {
            if(!playerEntry.getKey().equals(I)) {//skip self

                //check if path to home will lead closer to an enemy
                boolean isPathUnsafe = false;
                for (Vector homePathCell : pathToHome) {
                    if(playerEntry.getValue().getPosition().equals(homePathCell) //enemy already stands on path
                            || bsf(state, playerEntry.getValue().getPosition(), homePathCell, playerEntry.getKey()).size() < pathToHome.size() + HOME_PATH_SAFETY_INCREMENT){
                        isPathUnsafe = true;
                        break;
                    }
                }

                if (isPathUnsafe || buildFastestPath(state, TargetType.TAIL, playerEntry.getKey(), I).size() <= pathToHome.size() + HOME_PATH_SAFETY_INCREMENT) {
                    Logger.log("I'm in danger!");
                    return true;
                }
            }
        }
        return false;
    }

    //do something in case of danger
    protected void reset(){
        //do nothing
    }

    protected abstract Direction doSomething();

    protected boolean isValidMove(Params state, Vector currentPosition, Vector newPosition){
        boolean not180Turn = !Vector.sum(newPosition, Helper.convertToIndexes(me.getDirection()).invert()).equals(newPosition);

        boolean isHitsSelf = false;
        for (Vector position : me.getTail()) {
            if(position.equals(newPosition)){
                isHitsSelf = true;
                break;
            }
        }

        boolean isBorder = isBorder(newPosition);

        //avoid going deep inside my territory
        int myNeighbours = 0;
        if(get8neighbours(currentPosition, me.getTerritory()) != 8){//I can be already inside my territory, it will cause infinite loop
            myNeighbours = get8neighbours(newPosition, me.getTerritory());
        }

        //do not trap self
        boolean isHomeAccessible = true;
        if(!me.getTail().isEmpty() && !me.getTerritory().contains(newPosition)) {
            isHomeAccessible = !bsf(state, newPosition, me.getTerritory().get(0), I).isEmpty();
        }
        
        //check player to player collision
        boolean isUnsafeCollision = false;
        for (Map.Entry<String, Player> playerEntity : params.players.entrySet()) {
            if(!playerEntity.getKey().equalsIgnoreCase(I)
                    && playerEntity.getValue().getPosition().distance(newPosition) <= 1
                    && playerEntity.getValue().getTail().size() <= me.getTail().size()){
                isUnsafeCollision = true;
            }
        }

        return not180Turn && !isHitsSelf && !isBorder /*&& myNeighbours < 8*/ && isHomeAccessible && !isUnsafeCollision;
    }

    private boolean isBorder(Vector cell){
        boolean isBorder = false;
        isBorder |= cell.x < 0; //or -1???
        isBorder |= cell.y < 0;
        isBorder |= cell.x >= params.config.xSize - 1;
        isBorder |= cell.y >= params.config.ySize - 1;
        return isBorder;
    }

    protected int get8neighbours(Vector cell, List<Vector> searchIn){
        int neighbours = 0;
        for (int i = cell.x-1; i <= cell.x+1; i++) {
            for (int j = cell.y - 1; j <= cell.y + 1; j++) {
                if (!(i == cell.x && j == cell.y)) { //skip center cell
                    Vector neighbour = new Vector(i, j);
                    if(searchIn.contains(neighbour) || isBorder(neighbour)){
                        neighbours++;
                    } else if(i < 0 || j < 0 || i >= params.config.xSize || j >= params.config.ySize){//consider walls as neighbours
                        neighbours++;
                    }
                }
            }
        }

        return neighbours;
    }

    protected Deque<Vector> buildFastestPath(Params state, TargetType type, String sourceId, String targetId){
        Vector sourcePosition = state.getPlayer(sourceId).getPosition();
        double shortestRay = Double.MAX_VALUE;
        List<Vector> targetCells = null;
        switch (type){
            case TERRITORY:
                targetCells = state.getPlayer(targetId).getTerritory();
                break;
            case TAIL:
                targetCells = state.getPlayer(targetId).getTail();
                break;
        }

        Vector movingTo = Helper.convertToIndexes(state.getPlayer(sourceId).getDirection()).invert();
        targetCells.remove(Vector.sum(movingTo, state.getPlayer(sourceId).getPosition()));

        Vector closestTargetCell = null;
        for (Vector cell : targetCells) {
            if(shortestRay > cell.distance(sourcePosition)){
                shortestRay = cell.distance(sourcePosition);
                closestTargetCell = cell;
            }
        }

        Deque<Vector> path = bsf(state, sourcePosition, closestTargetCell, sourceId);

        return path;
    }

    protected LinkedList<Vector> bsf(Params state, Vector startPoint, Vector endPoint, String playerId){
        Vector movingTo = Helper.convertToIndexes(state.getPlayer(playerId).getDirection()).invert();
        Vector excludeCellBehind = Vector.sum(state.getPlayer(playerId).getPosition(), movingTo);

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
                            if(!visited.containsKey(neighbour) && !queue.contains(neighbour)
                                    && !neighbour.equals(excludeCellBehind) //cannot turn 180 degrees
                                    && !state.getPlayer(playerId).getTail().contains(neighbour) //do not go through tail
                                    && neighbour.x >=0 && neighbour.x < state.config.xSize //stay within field
                                    && neighbour.y >=0 && neighbour.y < state.config.ySize) {
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

    protected Deque<Vector> squarify(List<Vector> path){
        Deque<Vector> newPath = new LinkedList<>();
        newPath.add(path.get(0));

        Vector diff = Vector.sum(path.get(path.size()-1).copy().invert(), path.get(0));
        int xSign = diff.x < 0 ? -1 : 1;
        int ySing = diff.y < 0 ? -1 : 1;

        for (int i = 1; i < diff.x * xSign; i++) {

        }

        return null;
    }
}
