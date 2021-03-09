package com.floweytf.mappings;

import com.floweytf.betterlogger.BetterLogger;
import com.floweytf.betterlogger.FileTransport;
import com.floweytf.mappings.parser.Mappings;
import com.floweytf.mappings.parser.TsrgParser;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.AnnotatedEventManager;
import net.dv8tion.jda.api.hooks.SubscribeEvent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

class MappingSource {
    String type;
    String url;
}

class Meta {
    List<MappingSource> tsrg;
}

public class Main {
    public static Gson gson = new GsonBuilder().setPrettyPrinting().create();
    public static BetterLogger logger = new BetterLogger("MAPPINGS")
            .addTransport(BetterLogger.stdout)
            .addTransport(new FileTransport("logs/" + DateTimeFormatter.ofPattern("MM-dd-yyyy_hh-mm-ss").format(ZonedDateTime.now()) + ".log", BetterLogger.DEBUG));
    public static BiMap<String, Mappings> db = HashBiMap.create();


    public static void main(String... args) {
        logger.info("Downloading mappings...");
        logger.info("Parsing mappings map");
        Meta meta = null;
        try {
            URL url = new URL("https://raw.githubusercontent.com/FloweyTheFlower420/mappings-bot/master/meta.json");
            meta = gson.fromJson(new InputStreamReader(url.openStream()), Meta.class);
        }
        catch (Exception e) {
            logger.fatal(1, "Cannot obtain main meta file", e);
        }

        for (MappingSource elem: meta.tsrg) {
            try {
                Mappings tmp = TsrgParser.parse(new BufferedReader(new InputStreamReader(new URL(elem.url).openStream())));
                db.put(elem.type, tmp);
            }
            catch (Exception e) {
                logger.error("Unable to parse TSRG mapping", e);
            }
        }

        logger.info("Starting bot");
        logger.info("Running with {}", args[0]);
        try {
            JDA jda = JDABuilder.createDefault(args[0])
                    .setEventManager(new AnnotatedEventManager())
                    .addEventListeners(new Main())
                    .build();

            // optionally block until JDA is ready
            jda.awaitReady();
            logger.info("ready");
        }
        catch (Exception e) {
            logger.fatal(2, "Unable to create bot", e);
        }
    }

    Map<String, BiFunction<MessageReceivedEvent, String[], Integer>> map = new HashMap<String, BiFunction<MessageReceivedEvent, String[], Integer>>(){{
        put(
                "map",
                (event, args) -> {
                    String name = args[1];
                    String mapping = args[2];
                    Mappings tmp = db.get(name);
                    if(tmp == null) {
                        event.getChannel().sendMessage("Unknown mappings").submit();
                        return 1;
                    }

                    Mappings clazz = tmp;

                    // split by .
                    String[] path = name.split(" ");
                    for (String p : path) {
                        tmp = tmp.get(p);
                    }

                    event.getChannel().sendMessage(
                            "Class: " + clazz.name + '\n' +
                                 "Member: " + tmp.name + " (" + ((tmp.type == Mappings.Types.METHOD) ? "METHOD" : "FIELD")
                    ).submit();

                    return 0;
                }
        );
    }};

    @SubscribeEvent
    public void onMsg(MessageReceivedEvent event) {
        if(event.getMessage().getContentStripped().startsWith("m!")) {
            String message = event.getMessage().getContentStripped().replaceFirst("m!", "");
            String[] command = message.split(" ");
            BiFunction<MessageReceivedEvent, String[], Integer> f = map.get(command[0]);
            if(f == null)
                return;
            f.apply(event, command);
        }
    }
}
