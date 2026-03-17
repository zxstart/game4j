package com.game4j.starter.config;

/**
 * 服务器配置类
 * <p>
 * 存储服务器的启动配置，如端口号、扫描包路径等。
 * </p>
 */
public class ServerConfig {

    /**
     * 服务器端口，默认 9000
     */
    private int port = 9000;

    /**
     * 扫描包路径，用于自动发现控制器
     */
    private String scanPackage;

    public ServerConfig(String scanPackage) {
        this.scanPackage = scanPackage;
    }

    public int getPort() {
        return port;
    }

    public String getScanPackage() {
        return scanPackage;
    }

    public void setPort(int port) {
        this.port = port;
    }

}
