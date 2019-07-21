package ru.grishagin.model;

import org.json.JSONObject;
import ru.grishagin.utils.Vector;

import java.util.*;

import static ru.grishagin.Const.*;

public class Params {
    public final Config config;
    public final Map<String, Player> players = new HashMap<>();
    public final List<Map<String, Vector>> bonuses = new ArrayList<>();

    public Params(JSONObject config) {
        this.config = new Config(config.getInt(SPEED), config.getInt(X_CELLS_COUNT), config.getInt(Y_CELLS_COUNT), config.getInt(CELL_SIZE));
    }

    public void parse(JSONObject params){
        players.clear();
        bonuses.clear();//TODO: parse bonuses

        for (Map.Entry<String, Object> playerEntry : ((params.getJSONObject(PLAYERS).toMap())).entrySet()) {
            Map<String, Object> playerRawData = (Map<String, Object>)playerEntry.getValue();

            List<Vector> territory = new ArrayList<>();
            for (List<Integer> cell : (List<List<Integer>>) playerRawData.get(TERRITORY)) {
                territory.add(makeVectorFromArr(cell));
            }

            List<Vector> tail = new ArrayList<>();
            for (List<Integer> cell : (List<List<Integer>>) playerRawData.get(TAIL)) {
                tail.add(makeVectorFromArr(cell));
            }

            int speed = config.speed;

            List<Bonus> bonuses = new ArrayList<>();
            for (Map<String, Object> bonus : (List<Map<String, Object>>) playerRawData.get(BONUSES)) {
                String key = bonus.get(TYPE_KEY).toString();
                Bonus.BonusType type = Bonus.BonusType.valueOf(key);
                bonuses.add(new Bonus(type, Integer.parseInt(bonus.get(key).toString())));

                //todo: adjust speed
            }

            players.put(playerEntry.getKey(), new Player(playerEntry.getKey(),
                    (int)playerRawData.get(SCORE),
                    territory,
                    makeVectorFromArr((ArrayList<Integer>)playerRawData.get(POSITION)),
                    Direction.valueOf(playerRawData.get(DIRECTION).toString().toUpperCase()),
                    tail,
                    bonuses,
                    speed));
        }
    }

    private Vector makeVectorFromArr(List<Integer> arr){
        return new Vector(arr.get(0)/config.cellSize, arr.get(1)/config.cellSize);
    }

    public Player getPlayer(String id){
        return players.get(id);
    }
}
