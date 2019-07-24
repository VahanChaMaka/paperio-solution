package ru.grishagin;

import org.json.JSONObject;
import ru.grishagin.model.Direction;
import ru.grishagin.model.Params;
import ru.grishagin.strategy.BasicStrategy;
import ru.grishagin.strategy.PotentialFieldStrategy;
import ru.grishagin.strategy.Strategy;
import ru.grishagin.strategy.StupidRandomStrategy;
import ru.grishagin.utils.Logger;

import static ru.grishagin.Const.*;

public class Bot {
    private Logger logger = Logger.getInstance();

    private Strategy strategy;
    private Params params;
    private int tick = 0;

    public Bot(JSONObject config) {
        logger.log("Initial config: " + config.toString());

        params = new Params(config.getJSONObject(PARAMS_KEY));
        strategy = new PotentialFieldStrategy(params);
    }

    public JSONObject onInput(JSONObject input){
        logger.log("New data: " + input.toString());

        if(input.get(TYPE_KEY).equals(TICK)) {
            tick++;

            params.parse(input.getJSONObject(PARAMS_KEY));

            JSONObject command = new JSONObject();
            command.put(COMMAND, strategy.onTick().getValue());
            return command;
        } else {
            logger.log("wtf");
            return new JSONObject().put(COMMAND, Direction.UP.getValue());
        }
    }
}
