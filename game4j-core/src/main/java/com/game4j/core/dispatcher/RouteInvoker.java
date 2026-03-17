package com.game4j.core.dispatcher;

import java.lang.invoke.MethodHandle;

public class RouteInvoker {
    public static void invoke(RouteDefinition route,
                              Object session,
                              Object body) {
        try {
            MethodHandle handle = route.getMethodHandle();
            handle.invokeExact(session, body);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
