package com.floweytf.mappings;

import com.floweytf.betterlogger.BetterLogger;
import com.floweytf.betterlogger.FileTransport;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

import java.io.InputStreamReader;
import java.net.URL;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class Main {
    public static Gson gson = new GsonBuilder().setPrettyPrinting().create();
    public static BetterLogger logger = new BetterLogger("MAPPINGS")
            .addTransport(BetterLogger.stdout)
            .addTransport(new FileTransport("logs/" + DateTimeFormatter.ofPattern("MM-dd-yyyy_hh-mm-ss").format(ZonedDateTime.now()) + ".log", BetterLogger.DEBUG));
    public void main(String... args) {
        logger.info("Downloading mappings...");
        logger.info("Parsing mappings map");
        try {
            URL url = new URL("https://raw.githubusercontent.com/FloweyTheFlower420/mappings-bot/master/meta.json");
            JsonElement meta = gson.fromJson(new InputStreamReader(url.openStream()), JsonElement.class);
        }
        catch (Exception e) {
            logger.fatal(1, "Cannot obtain main meta file", e);
        }

    }
}
