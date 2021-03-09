package com.floweytf.mappings.parser;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class Mappings {
    public static enum Types {
        METHOD,
        FIELD,
        CLASS,
        ROOT
    }

    public Mappings(Types type, String name) {
        assert type != Types.FIELD;
        this.type = type;
        this.name = name;
    }

    public Mappings(Types type, String name, String functionSignature) {
        assert type == Types.FIELD;
        this.type = type;
        this.name = name;
        this.functionSignature = functionSignature;
    }

    public void insertMapping(String mc, Mappings mapped) {
        assert type == Types.CLASS || type == Types.ROOT;
        mapping.put(mc, mapped);
    }

    public Mappings get(String mc) {
        return mapping.get(mc);
    }

    @EqualsAndHashCode.Exclude
    public Types type;
    public String name;

    @Nullable
    public String functionSignature;

    // MC name -> Mapped name
    private final BiMap<String, Mappings> mapping = HashBiMap.create();
}
