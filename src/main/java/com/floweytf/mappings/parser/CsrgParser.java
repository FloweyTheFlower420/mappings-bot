package com.floweytf.mappings.parser;

import java.io.BufferedReader;
import java.io.IOException;

public class CsrgParser {
    public static MappingsDatabase parse(BufferedReader buffer, BufferedReader readerMember) throws IOException {
        MappingsDatabase db = new MappingsDatabase();

        String line = null;
        while ((line = buffer.readLine()) != null) {
            if(line.startsWith("#"))
                continue;
            String[] names = line.split(" ");
            names[1] = "/net/minecraft/server/" + names[1];
            db.insert(names[0], names[1]);
        }

        while ((line = readerMember.readLine()) != null) {
            if(line.startsWith("#"))
                continue;
            String[] names = line.split(" ");
            // ChatModifier a (Ljava/lang/String;)LChatModifier; setInsertion
            names[0] = "/net/minecraft/server/" + names[0];
            if(names.length == 3)
                db.insert(db.reobf(names[0]) + '.' + names[1], new MemberMapping(MemberMapping.FIELD, names[0] + '.' + names[2], ""));
            else
                db.insert(db.reobf(names[0]) + '.' + names[1], new MemberMapping(MemberMapping.METHOD, names[0] + '.' + names[3], names[2]));
        }

        return db;
    }
}
