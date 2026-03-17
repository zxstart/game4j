package com.game4j.core.annotation;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
/**
 * 标识一个类为控制器组件
 * <p>
 * 被此注解标记的类将被扫描并注册为游戏逻辑控制器，
 * 用于处理特定的游戏业务逻辑。
 * </p>
 */
public @interface G4JController {

    /**
     * 控制器名称或描述，默认为空
     * @return 控制器名称
     */
    String value() default "";
}
