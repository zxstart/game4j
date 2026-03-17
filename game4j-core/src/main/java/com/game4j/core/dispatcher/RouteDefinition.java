package com.game4j.core.dispatcher;

import lombok.Getter;
import lombok.Setter;

import java.lang.invoke.MethodHandle;

@Setter
@Getter
public class RouteDefinition {

    private int cmd;

    private MethodHandle methodHandle;

    private Object controller;

    private Class<?> bodyType;

}
