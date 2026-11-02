package com.game4j.starter.spring;

import org.springframework.context.annotation.Import;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enables Game4j in a Spring application.
 * <p>
 * When {@link #scanBasePackage()} is not set, controllers are scanned from the
 * package containing the annotated application class. The bootstrap is started
 * after the Spring context is initialized and stopped when that context closes.
 * </p>
 *
 * <pre>
 * &#64;SpringBootApplication
 * &#64;EnableGame4j
 * public class DemoApplication {
 * }
 * </pre>
 *
 * <p>For a standalone Java application, call
 * {@code GameBootstrap.run(DemoApplication.class)} from {@code main}.</p>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Import(Game4jBootstrapConfiguration.class)
public @interface EnableGame4j {

    /**
     * Controller scan root. An empty value uses the annotated class's package.
     */
    String scanBasePackage() default "";
}
