package com.floweytf.mappings.parser;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MappingsDatabase {
    private final Map<String, String> unobfclass = new HashMap<>();
    private final Map<String, String> reobfclass = new HashMap<>();
    private final Map<String, List<MemberMapping>> unobfmembers = new HashMap<>();
    private final Map<MemberMapping, String> reobfmembers = new HashMap<>();

    public void insert(String mc, MemberMapping mapping) {
        if(!unobfmembers.containsKey(mc))
            unobfmembers.put(mc, new ArrayList<>());

        unobfmembers.get(mc).add(mapping);
        reobfmembers.put(mapping, mc);
    }

    public void insert(String mc, String mapping) {
        unobfclass.put(mc, mapping);
        reobfclass.put(mapping, mc);
    }

    public String unobf(String str) {
        return unobfclass.get(str);
    }

    public String reobf(String str) {
        return reobfclass.get(str);
    }

    public List<MemberMapping> unobfMember(String str) {
        return unobfmembers.get(str);
    }

    public String reobfMember(String str) {
        return reobfmembers.get(str);
    }
}
