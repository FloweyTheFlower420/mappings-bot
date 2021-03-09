package com.floweytf.mappings.parser;

import lombok.EqualsAndHashCode;

public class MemberMapping {
    public static final int FIELD = 1;
    public static final int METHOD = 1;
    @EqualsAndHashCode.Include
    public String name;
    @EqualsAndHashCode.Include
    public int type;

    @EqualsAndHashCode.Include
    public String param;

    MemberMapping(int type, String name, String param) {
        this.name = name;
        this.type = type;
        this.param = param;
    }
}
