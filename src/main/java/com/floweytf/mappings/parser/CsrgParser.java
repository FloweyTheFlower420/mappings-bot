package com.floweytf.mappings.parser;

import java.io.BufferedReader;
import java.io.IOException;

public class CsrgParser {
    public static MappingsDatabase parse(BufferedReader buffer, BufferedReader readerMember) throws IOException {
        MappingsDatabase db = new MappingsDatabase();

        String line = null;
        while ((line = buffer.readLine()) != null) {
            String[] names = line.split(" ");
            db.insert(names[0], names[1]);
        }

        while ((line = readerMember.readLine()) != null) {
            String[] names = line.split(" ");
            // ChatModifier a (Ljava/lang/String;)LChatModifier; setInsertion
            if(names.length == 3)
                db.insert(db.unobf(names[0]) + names[1], new MemberMapping(MemberMapping.FIELD, names[2], ""));
            else
                db.insert(db.unobf(names[0]) + names[1], new MemberMapping(MemberMapping.METHOD, names[3], names[2]));
        }

        return db;
    }
}
