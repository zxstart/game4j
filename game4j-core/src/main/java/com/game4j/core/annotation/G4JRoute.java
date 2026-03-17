package com.game4j.core.annotation;

import java.lang.annotation.*;

/**
 * 路由映射注解
 * <p>
 * 用于标记控制器中的方法，将其映射到特定的命令ID (cmd)。
 * 当收到对应 cmd 的消息时，框架会自动调用该方法。
 * </p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface G4JRoute {
    /**
     * 命令ID，用于唯一标识一条协议
     *
     * @return 命令ID
     */
    int cmd();
}
