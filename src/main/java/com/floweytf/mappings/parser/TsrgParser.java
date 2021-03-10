package com.floweytf.mappings.parser;

import java.io.BufferedReader;
import java.io.IOException;

public class TsrgParser {
    public static MappingsDatabase parse(BufferedReader buffer) throws IOException {
        String currentClass = null;
        MappingsDatabase db = new MappingsDatabase();

        String line;
        while ((line = buffer.readLine()) != null) {
            if (!line.startsWith("\t")) {
                String[] names = line.split(" ");
                currentClass = names[0];
                db.insert(names[0], names[1]);
            }
            else {
                String[] data = line.substring(1).split(" ");
                assert currentClass != null;
                if (data.length == 2)
                    db.insert(currentClass + '.' + data[0], new MemberMapping(MemberMapping.FIELD, currentClass + '.' + data[1], ""));
                else
                    db.insert(currentClass + '.' + data[0], new MemberMapping(MemberMapping.FIELD, currentClass + '.' + data[2], data[1]));
            }
        }
        return db;
    }
}
