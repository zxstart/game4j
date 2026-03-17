package com.game4j.core.dispatcher;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RouteDispatcher {

    private static final Map<Integer, RouteDefinition> ROUTES =
            new ConcurrentHashMap<>();

    public static void register(RouteDefinition def) {

        ROUTES.put(def.getCmd(), def);
    }

    public static RouteDefinition get(int cmd) {

        return ROUTES.get(cmd);
    }

}
