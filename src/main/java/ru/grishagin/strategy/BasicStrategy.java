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

    protected abstract Direction doSomething();

    protected boolean isValidMove(Vector currentPosition, Vector newPosition){
        boolean not180Turn = !(currentPosition.x + newPosition.x == 0 && currentPosition.y + newPosition.y == 0);


        Vector positionAfterMove = new Vector(me.getPosition().x + newPosition.x,
                me.getPosition().y + newPosition.y);

        boolean isHitsSelf = false;
        for (Vector position : me.getTail()) {
            if(position.equals(positionAfterMove)){
                isHitsSelf = true;
                break;
            }
        }

        boolean isBorder = false;
        isBorder |= positionAfterMove.x < 0; //or -1???
        isBorder |= positionAfterMove.y < 0;
        isBorder |= positionAfterMove.x >= params.config.xSize;
        isBorder |= positionAfterMove.y >= params.config.ySize;

        //avoid going deep inside my territory
        int myNeighbours = 0;
        if(get8neighbours(me.getPosition(), me.getTerritory()) != 8){//I can be already inside my territory, it will cause infinite loop
            myNeighbours = get8neighbours(positionAfterMove, me.getTerritory());
        }

        //do not trap self
        boolean isHomeAccessible = true;
        if(!me.getTail().isEmpty() && !me.getTerritory().contains(positionAfterMove)) {
            isHomeAccessible = !bsf(positionAfterMove, me.getTerritory().get(0), I).isEmpty();
        }

        return not180Turn && !isHitsSelf && !isBorder && myNeighbours < 8 && isHomeAccessible;
    }

    protected int get8neighbours(Vector cell, List<Vector> searchIn){
        int neighbours = 0;
        for (int i = cell.x-1; i <= cell.x+1; i++) {
            for (int j = cell.y - 1; j <= cell.y + 1; j++) {
                if (!(i == cell.x && j == cell.y)) { //skip center cell
                    if(searchIn.contains(new Vector(i, j))){
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
