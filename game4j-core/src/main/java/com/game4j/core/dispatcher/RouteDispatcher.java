package com.game4j.core.dispatcher;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 路由分发器
 * <p>
 * 负责管理和查找路由定义。
 * 它维护了一个命令ID到路由定义的映射关系。
 * </p>
 */
public class RouteDispatcher {

    /**
     * 路由表，Key为命令ID，Value为路由定义
     */
    private static final Map<Integer, RouteDefinition> ROUTES =
            new ConcurrentHashMap<>();

    /**
     * 注册路由
     * 
     * @param def 路由定义
     */
    public static void register(RouteDefinition def) {

        ROUTES.put(def.getCmd(), def);
    }

    /**
     * 获取路由
     * 
     * @param cmd 命令ID
     * @return 路由定义，如果找不到则返回 null
     */
    public static RouteDefinition get(int cmd) {

        return ROUTES.get(cmd);
    }

}
