import org.json.JSONException;
import org.json.JSONObject;
import ru.grishagin.Bot;
import ru.grishagin.utils.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Main {
    public static void main(String[] args) {
        Bot bot;
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        String line;
        try {
            boolean isLocalGame = args != null && args.length>0 && args[0].equals("local");
            Logger.isLocalRun = isLocalGame;
            Logger.getInstance().log("\nStarting new game...");

            line = in.readLine();
            JSONObject config = new JSONObject(line);
            bot = new Bot(config);
            while ((line = in.readLine()) != null && line.length() != 0) {
                JSONObject parsed = new JSONObject(line);
                JSONObject command = bot.onInput(parsed);
                System.out.println(command.toString());
            }
        }
        catch (IOException e) {
            System.err.println(e);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
