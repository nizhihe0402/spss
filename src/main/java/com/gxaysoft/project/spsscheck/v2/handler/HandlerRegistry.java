package com.gxaysoft.project.spsscheck.v2.handler;

import com.gxaysoft.project.spsscheck.v2.model.RuleHandler;
import com.gxaysoft.project.spsscheck.v2.model.RuleType;

import java.util.*;

public class HandlerRegistry {
    private static final Map<RuleType, RuleHandler> handlers = new LinkedHashMap<>();

    static {
        register(new ComputeHandler());
        register(new DuplicateMarkHandler());
        register(new OutputGroupHandler());
        for (RuleType t : RuleType.values()) {
            if (!handlers.containsKey(t)) {
                register(new CheckHandler(t));
            }
        }
    }

    public static void register(RuleHandler h) { handlers.put(h.handles(), h); }

    public static RuleHandler get(RuleType type) { return handlers.get(type); }

    public static Collection<RuleHandler> all() { return handlers.values(); }
}
