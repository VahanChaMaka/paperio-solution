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

        checkDanger();

        if(isRetreating){
            if(!retreatingPath.isEmpty()) {
                reset();
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

                //check if path to home will lead closer to an enemy
                boolean isPathUnsafe = false;
                for (Vector homePathCell : pathToHome) {
                    if(playerEntry.getValue().getPosition().equals(homePathCell) //enemy already stands on path
                            || bsf(playerEntry.getValue().getPosition(), homePathCell, playerEntry.getKey()).size() < pathToHome.size() + HOME_PATH_SAFETY_INCREMENT){
                        isPathUnsafe = true;
                        break;
                    }
                }

                if (isPathUnsafe || buildFastestPath(TargetType.TAIL, playerEntry.getKey(), I).size() <= pathToHome.size() + HOME_PATH_SAFETY_INCREMENT) {
                    retreatingPath = pathToHome;
                    isRetreating = true;
                    Logger.log("I'm in danger!");
                    return;
                }
            }
        }
    }

    //do something in case of danger
    protected void reset(){
        //do nothing
    }

    protected abstract Direction doSomething();

    protected boolean isValidMove(Vector currentPosition, Vector newPosition){
        boolean not180Turn = !Vector.sum(newPosition, Helper.convertToIndexes(me.getDirection()).invert()).equals(newPosition);

        boolean isHitsSelf = false;
        for (Vector position : me.getTail()) {
            if(position.equals(newPosition)){
                isHitsSelf = true;
                break;
            }
        }

        boolean isBorder = false;
        isBorder |= newPosition.x < 0; //or -1???
        isBorder |= newPosition.y < 0;
        isBorder |= newPosition.x >= params.config.xSize - 1;
        isBorder |= newPosition.y >= params.config.ySize - 1;

        //avoid going deep inside my territory
        int myNeighbours = 0;
        if(get8neighbours(currentPosition, me.getTerritory()) != 8){//I can be already inside my territory, it will cause infinite loop
            myNeighbours = get8neighbours(newPosition, me.getTerritory());
        }

        //do not trap self
        boolean isHomeAccessible = true;
        if(!me.getTail().isEmpty() && !me.getTerritory().contains(newPosition)) {
            isHomeAccessible = !bsf(newPosition, me.getTerritory().get(0), I).isEmpty();
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

    protected int get8neighbours(Vector cell, List<Vector> searchIn){
        int neighbours = 0;
        for (int i = cell.x-1; i <= cell.x+1; i++) {
            for (int j = cell.y - 1; j <= cell.y + 1; j++) {
                if (!(i == cell.x && j == cell.y)) { //skip center cell
                    if(searchIn.contains(new Vector(i, j))){
                        neighbours++;
                    } else if(i < 0 || j < 0 || i >= params.config.xSize || j >= params.config.ySize){//consider walls as neighbours
                        neighbours++;
                    }
                }
            }
        }

        return neighbours;
    }

    protected Deque<Vector> buildFastestPath(TargetType type, String sourceId, String targetId){
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

    protected LinkedList<Vector> bsf(Vector startPoint, Vector endPoint, String playerId){
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
                            if(!visited.containsKey(neighbour) && !queue.contains(neighbour)
                                    && !neighbour.equals(excludeCellBehind) //cannot turn 180 degrees
                                    && !params.getPlayer(playerId).getTail().contains(neighbour) //do not go through tail
                                    && neighbour.x >=0 && neighbour.x < params.config.xSize //stay within field
                                    && neighbour.y >=0 && neighbour.y < params.config.ySize) {
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
