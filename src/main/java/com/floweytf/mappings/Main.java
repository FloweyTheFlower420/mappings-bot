package com.floweytf.mappings;

import com.floweytf.betterlogger.BetterLogger;
import com.floweytf.betterlogger.FileTransport;
import com.floweytf.mappings.parser.CsrgParser;
import com.floweytf.mappings.parser.MappingsDatabase;
import com.floweytf.mappings.parser.MemberMapping;
import com.floweytf.mappings.parser.TsrgParser;
import com.google.gson.*;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.AnnotatedEventManager;
import net.dv8tion.jda.api.hooks.SubscribeEvent;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

class MappingSource {
    String type;
    String url;
}

class Meta {
    List<MappingSource> tsrg;
    List<MappingSource> csrg;
}

public class Main {
    public static Gson gson = new GsonBuilder().setPrettyPrinting().create();
    public static BetterLogger logger = new BetterLogger("MAPPINGS")
            .addTransport(BetterLogger.stdout)
            .addTransport(new FileTransport("logs/" + DateTimeFormatter.ofPattern("MM-dd-yyyy_hh-mm-ss").format(ZonedDateTime.now()) + ".log", BetterLogger.DEBUG));
    public static Map<String, MappingsDatabase> db = new HashMap<>();

    public static void main(String... args) {
        // downloading meta mappings
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
        assert meta != null;

        ExecutorService pool = Executors.newFixedThreadPool(32);

        // parse CSRG (Bukkit) Wrappings
        for (MappingSource elem: meta.csrg) {
            pool.submit(() -> {
                BetterLogger l = new BetterLogger(logger);
                l.loggerName += "/" + Thread.currentThread().getName();

                try {
                    l.info("Parsing " + elem.type + " at " + elem.url);
                    String[] strings = elem.url.split(" ");
                    db.put(elem.type, CsrgParser.parse(
                            new BufferedReader(new InputStreamReader(new URL(strings[0]).openStream())),
                            new BufferedReader(new InputStreamReader(new URL(strings[1]).openStream()))
                    ));
                } catch (Exception e) {
                    logger.error("Unable to parse CSRG mapping", e);
                }
            });
        }

        // parse TSRG (Forge) Wrappings
        for (MappingSource elem: meta.tsrg) {
            pool.submit(() -> {
                BetterLogger l = new BetterLogger(logger);
                l.loggerName += "/" + Thread.currentThread().getName();

                try {
                    l.info("Parsing " + elem.type + " at " + elem.url);
                    db.put(elem.type, TsrgParser.parse(new BufferedReader(new InputStreamReader(new URL(elem.url).openStream()))));
                } catch (Exception e) {
                    logger.error("Unable to parse TSRG mapping", e);
                }
            });
        }

        try {
            pool.awaitTermination(120, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
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

                    // get database
                    MappingsDatabase database = db.get(mapping);
                    if(database == null) {
                        event.getChannel().sendMessage("Unknown mappings").submit();
                        return 1;
                    }

                    // split by .
                    String[] path = name.split("\\.");
                    String clazz = database.unobf(path[0]);
                    String classMapping = "Class: `" + path[0] + " -> " + clazz + "`";


                    if(path.length == 1) {
                        event.getChannel().sendMessage(classMapping).submit();
                        return 0;
                    }
                    else if (path.length == 2) {
                        List<MemberMapping> members = database.members.get(name);
                        if(members == null) {
                            event.getChannel().sendMessage("Unknown mappings").submit();
                            return 1;
                        }
                        StringBuilder builder = new StringBuilder();
                        for (MemberMapping memberMapping : members)
                            builder.append("Member: `").append(path[1]).append(" -> ").append(memberMapping.name).append("`").append('\n');

                        event.getChannel().sendMessage(
                            classMapping + '\n' +
                            builder.toString()
                        ).submit();
                    }
                    else
                        return 3;

                    return 0;
                }
        );
        put(
                "convert",
                (event, args) -> {
                    String name = args[1];
                    String mapping = args[2];

                    // get database
                    MappingsDatabase database = db.get(mapping);
                    if(database == null) {
                        event.getChannel().sendMessage("Unknown mappings").submit();
                        return 1;
                    }

                    // split by .
                    String[] path = name.split("\\.");
                    String clazz = database.reobf(path[0]);
                    String classMapping = "Class: `" + path[0] + " -> " + clazz + "`";


                    if(path.length == 1) {
                        event.getChannel().sendMessage(classMapping).submit();
                        return 0;
                    }
                    else if (path.length == 2) {
                        List<MemberMapping> members = database.members.get(name);
                        if(members == null) {
                            event.getChannel().sendMessage("Unknown mappings").submit();
                            return 1;
                        }
                        StringBuilder builder = new StringBuilder();
                        for (MemberMapping memberMapping : members)
                            builder.append("Member: `").append(path[1]).append(" -> ").append(memberMapping.name).append("`").append('\n');

                        event.getChannel().sendMessage(
                                classMapping + '\n' +
                                        builder.toString()
                        ).submit();
                    }
                    else
                        return 3;

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
