package com.game4j.core.annotation;

import java.lang.annotation.*;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
/**
 * 标识方法参数为请求体
 * <p>
 * 在控制器方法中，使用此注解标记的参数将自动绑定为
 * 客户端发送的消息体（Protobuf对象）。
 * </p>
 */
public @interface G4JRequestBody {
}
