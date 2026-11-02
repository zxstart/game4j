package com.game4j.starter.spring;

import com.game4j.starter.GameBootstrap;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportAware;
import org.springframework.core.type.AnnotationMetadata;

import java.util.Map;

/**
 * Registers the Game4j lifecycle with the enclosing Spring context.
 */
@Configuration(proxyBeanMethods = false)
public class Game4jBootstrapConfiguration implements ImportAware {
    private String scanBasePackage;

    @Override
    public void setImportMetadata(AnnotationMetadata importingClassMetadata) {
        Map<String, Object> attributes = importingClassMetadata.getAnnotationAttributes(
                EnableGame4j.class.getName(), false
        );
        String configuredPackage = attributes == null ? "" : (String) attributes.get("scanBasePackage");
        scanBasePackage = configuredPackage == null || configuredPackage.isBlank()
                ? packageOf(importingClassMetadata.getClassName())
                : configuredPackage.trim();
    }

    @Bean
    public GameBootstrap gameBootstrap() {
        if (scanBasePackage == null) {
            throw new IllegalStateException("@EnableGame4j import metadata was not initialized");
        }
        return new GameBootstrap(scanBasePackage);
    }

    /**
     * Starts Game4j after Spring finishes refreshing the application context.
     */
    @Bean
    public SmartLifecycle game4jLifecycle(GameBootstrap gameBootstrap) {
        return new Game4jLifecycle(gameBootstrap);
    }

    private static String packageOf(String className) {
        int separator = className.lastIndexOf('.');
        if (separator <= 0) {
            throw new IllegalArgumentException(
                    "@EnableGame4j requires the annotated application class to be in a named package"
            );
        }
        return className.substring(0, separator);
    }

    private static final class Game4jLifecycle implements SmartLifecycle {
        private final GameBootstrap gameBootstrap;
        private volatile boolean running;

        private Game4jLifecycle(GameBootstrap gameBootstrap) {
            this.gameBootstrap = gameBootstrap;
        }

        @Override
        public void start() {
            if (!running) {
                gameBootstrap.start();
                running = true;
            }
        }

        @Override
        public void stop() {
            if (running) {
                try {
                    gameBootstrap.stop();
                } finally {
                    running = false;
                }
            }
        }

        @Override
        public void stop(Runnable callback) {
            try {
                stop();
            } finally {
                callback.run();
            }
        }

        @Override
        public boolean isRunning() {
            return running;
        }

        @Override
        public boolean isAutoStartup() {
            return true;
        }
    }
}
