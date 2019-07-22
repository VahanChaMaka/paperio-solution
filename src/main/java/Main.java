import org.json.JSONException;
import org.json.JSONObject;
import ru.grishagin.Bot;
import ru.grishagin.utils.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.spec.ECField;
import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        Bot bot;
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        String line;
        try {
            boolean isLocalGame = args != null && args.length>0 && args[0].equals("local");
            Logger.isLocalRun = isLocalGame;
            long startTime = 0;
            Logger.log("\nStarting new game...");

            line = in.readLine();
            JSONObject config = new JSONObject(line);
            bot = new Bot(config);
            while ((line = in.readLine()) != null && line.length() != 0) {
                JSONObject parsed = new JSONObject(line);
                try {
                    if(isLocalGame){
                        startTime = System.currentTimeMillis();
                    }

                    JSONObject command = bot.onInput(parsed);

                    if(isLocalGame){
                        Logger.log("Execution time in ms: " + (System.currentTimeMillis() - startTime));
                    }

                    System.out.println(command.toString());
                } catch (Exception e){
                    Logger.log(e.toString());
                    Logger.log(Arrays.toString(e.getStackTrace()));
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}
