package ru.grishagin.utils;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;

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
}
