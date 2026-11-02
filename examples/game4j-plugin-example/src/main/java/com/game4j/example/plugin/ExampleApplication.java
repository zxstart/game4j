package com.game4j.example.plugin;

import com.game4j.starter.GameBootstrap;
import com.game4j.starter.spring.EnableGame4j;

/**
 * 可直接运行的插件示例服务入口。
 */
@EnableGame4j(scanBasePackage = "com.game4j.demo")
public final class ExampleApplication {

    private ExampleApplication() {
    }

    public static void main(String[] args) {
        GameBootstrap.run(ExampleApplication.class);
    }
}
