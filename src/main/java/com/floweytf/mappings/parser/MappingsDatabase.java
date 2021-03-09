package com.floweytf.mappings.parser;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MappingsDatabase {
    private final Map<String, String> unobf = new HashMap<>();
    private final Map<String, String> reobf = new HashMap<>();
    public final Map<String, List<MemberMapping>> members = new HashMap<>();

    public void insert(String mc, MemberMapping mapping) {
        if(!members.containsKey(mc))
            members.put(mc, new ArrayList<>());

        members.get(mc).add(mapping);
    }

    public void insert(String mc, String mapping) {
        unobf.put(mc, mapping);
        reobf.put(mapping, mc);
    }

    /**
     * @param str The obfuscated name
     * @return deobfusacted name, according to a mapping
     */
    public String unobf(String str) {
        return unobf.get(str);
    }

    public String reobf(String str) {
        return reobf.get(str);
    }
}
