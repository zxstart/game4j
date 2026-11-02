# Game4j

Game4j 是一个基于 Java 和 Netty 的游戏服务器框架，提供消息路由、Protobuf 编解码、集群管理和插件机制。

## 环境要求

- JDK 25
- Maven 3

## 构建与运行示例

在项目根目录执行：

```shell
mvn clean package
java -jar examples/game4j-plugin-example/target/game4j-plugin-example-1.0-SNAPSHOT.jar
```

示例默认监听 `19000` 端口。客户端运行方式、插件注册和消息示例见[插件示例说明](examples/game4j-plugin-example/README.md)。

## 模块

- `game4j-core`：控制器、路由和上下文。
- `game4j-processor`：控制器索引注解处理器。
- `game4j-protocol`：消息协议。
- `game4j-netty`：Netty 服务、编解码和会话。
- `game4j-cluster`：集群管理。
- `game4j-plugin`：插件机制和集群管理控制台。
- `game4j-starter`：启动与配置。
- `examples/game4j-plugin-example`：插件与客户端示例。

默认配置见 [game4j.properties](game4j-starter/src/main/resources/game4j.properties)，更多部署说明见 [docs](docs/)。

## 许可证

本项目使用 [Apache License 2.0](LICENSE)。
