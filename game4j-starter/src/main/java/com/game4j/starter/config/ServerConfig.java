package com.game4j.starter.config;

public class ServerConfig {

    private int port = 9000;

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
