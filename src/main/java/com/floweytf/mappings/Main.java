package com.floweytf.mappings;

import com.floweytf.betterlogger.BetterLogger;
import com.floweytf.betterlogger.FileTransport;
import com.floweytf.mappings.parser.CsrgParser;
import com.floweytf.mappings.parser.MappingsDatabase;
import com.floweytf.mappings.parser.MemberMapping;
import com.floweytf.mappings.parser.TsrgParser;
import com.google.gson.*;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.channel.MessageChannel;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
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
    public static Map<String, MappingsDatabase> db = new ConcurrentHashMap<>();

    private static Map<String, BiFunction<MessageCreateEvent, String[], Integer>> map = new HashMap<String, BiFunction<MessageCreateEvent, String[], Integer>>(){{
        put(
                "map",
                (event, args) -> {
                    final MessageChannel channel = Objects.requireNonNull(event.getMessage().getChannel().block());
                    String name = args[1];
                    String mapping = args[2];

                    // get database
                    MappingsDatabase database = db.get(mapping);
                    if(database == null) {
                        channel.createMessage("Unknown mappings");
                        return 1;
                    }

                    // split by .
                    String[] path = name.split("\\.");
                    String clazz = database.unobf(path[0]);
                    String classMapping = "Class: `" + path[0] + " -> " + clazz + "`";


                    if(path.length == 1) {
                        channel.createMessage(classMapping);
                        return 0;
                    }
                    else if (path.length == 2) {
                        List<MemberMapping> members = database.unobfMember(name);
                        if(members == null) {
                            channel.createMessage("Unknown mappings");
                            return 1;
                        }
                        StringBuilder builder = new StringBuilder();
                        for (MemberMapping memberMapping : members)
                            builder.append("Member: `").append(path[1]).append(" -> ").append(memberMapping.name).append("`").append('\n');

                        channel.createMessage(
                                classMapping + '\n' +
                                        builder.toString()
                        );
                    }
                    else
                        return 3;

                    return 0;
                }
        );
        put(
                "convert",
                (event, args) -> {
                    final MessageChannel channel = Objects.requireNonNull(event.getMessage().getChannel().block());

                    String name = args[1];
                    String from = args[2];
                    String to = args[3];

                    // get database
                    MappingsDatabase fromDatabase = db.get(from);
                    MappingsDatabase toDatabase = db.get(to);

                    if(fromDatabase == null || toDatabase == null) {
                        channel.createMessage("Unknown mappings");
                        return 1;
                    }

                    if(!from.split("-")[0].equals(to.split("-")[0])) {
                        channel.createMessage("Unmatched versions");
                        return 1;
                    }

                    // split by .
                    String[] path = name.split("\\.");

                    // obtain reobfusicated mappings
                    String fromClass = fromDatabase.reobf(path[0]);
                    String toMapping = toDatabase.unobf(fromClass);

                    String classMapping = "Class: `" + name + " -> " + fromClass + " -> " + toMapping + "`";

                    if(path.length == 1) {
                        channel.createMessage(classMapping);
                        return 0;
                    }
                    else if (path.length == 2) {
                        String fromMember = fromDatabase.reobfMember(name);
                        List<MemberMapping> members = toDatabase.unobfMember(fromMember);

                        if(members == null) {
                            channel.createMessage("Unknown mappings");
                            return 1;
                        }
                        StringBuilder builder = new StringBuilder();
                        for (MemberMapping memberMapping : members)
                            builder.append("Member: `").append(name).append(" -> ").append(fromMember).append(" -> ").append(memberMapping.name).append("`\n");

                        channel.createMessage(
                                classMapping + '\n' +
                                        builder.toString()
                        );
                    }
                    else
                        return 3;

                    return 0;
                }
        );
    }};


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

                l.info("Done parsing " + elem.type);
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

                l.info("Done parsing " + elem.type);
            });
        }

        try {
            pool.shutdown();
            pool.awaitTermination(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        logger.info("Starting bot");
        logger.info("Running with {}", args[0]);
        try {
            final String token = args[0];
            final DiscordClient client = DiscordClient.create(token);
            final GatewayDiscordClient gateway = client.login().block();

            gateway.on(MessageCreateEvent.class).subscribe(event -> {
                String message = event.getMessage().getContent();
                if (message.startsWith("m!")) {
                    message = message.replace("m!", "");
                    String[] command = message.split(" ");

                    BiFunction<MessageCreateEvent, String[], Integer> function = map.get(command[0]);
                    function.apply(event, command);
                }
            });
        }
        catch (Exception e) {
            logger.fatal(2, "Unable to create bot", e);
        }
    }
}
