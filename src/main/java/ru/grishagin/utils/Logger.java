package ru.grishagin.utils;

import ru.grishagin.model.Params;
import ru.grishagin.model.Player;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Map;

public final class Logger {
    private static final Logger instance = new Logger();
    public static boolean isLocalRun = false;

    private Logger(){
    }

    public static Logger getInstance(){
        return instance;
    }

    public static void log(String message){
        if(isLocalRun) {
            try (FileWriter log = new FileWriter("local.log", true)) {
                long time = System.currentTimeMillis();
                Calendar calendar = new GregorianCalendar();
                calendar.setTimeInMillis(time);
                log.append("" + calendar.get(Calendar.HOUR) + ":" + calendar.get(Calendar.MINUTE) + ":" +
                        calendar.get(Calendar.SECOND) + " -> ");
                log.append(message);
                log.append("\n\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void drawSnapshot(Params params){
        if(isLocalRun) {
            StringBuilder builder = new StringBuilder();
            for (int j = 0; j < params.config.xSize; j++) {
                for (int i = 0; i < params.config.ySize; i++) {
                    boolean isEmpty = true;
                    for (Map.Entry<String, Player> playerEntry : params.players.entrySet()) {
                        for (Vector vector : playerEntry.getValue().getTerritory()) {
                            if (vector.x == i && vector.y == j) {
                                builder.append(" ").append(playerEntry.getKey());
                                isEmpty = false;
                            }
                        }

                        for (Vector vector : playerEntry.getValue().getTail()) {
                            if (vector.x == i && vector.y == j) {
                                if(playerEntry.getValue().getPosition().x == i && playerEntry.getValue().getPosition().y == j) {
                                    builder.append(" ").append("x");
                                    isEmpty = false;
                                } else {
                                    builder.append("-").append(playerEntry.getKey());
                                    isEmpty = false;
                                }
                            }
                        }

                        for (Map<String, Vector> bonus : params.bonuses) {
                            for (Map.Entry<String, Vector> bonusInstance : bonus.entrySet()) {
                                if (bonusInstance.getValue().x == i && bonusInstance.getValue().y == j) {
                                    builder.append(" ").append(bonusInstance.getKey());
                                    isEmpty = false;
                                }
                            }
                        }
                    }

                    if(isEmpty){
                        builder.append(" ").append(".");
                    }
                    builder.append(" ");
                }
                builder.append("\n");
            }

            log(builder.toString());
        }
    }

    public static void drawArray(double[][] arr){
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < arr.length; i++) {
            for (int j = 0; j < arr[i].length; j++) {
                if(arr[i][j] < 10 && arr[i][j] >= 0){
                    builder.append(" ");
                }

                builder.append(String.format("%.1f", arr[i][j]));
                builder.append(" ");
            }
            builder.append("\n");
        }

        log(builder.toString());
    }
}
