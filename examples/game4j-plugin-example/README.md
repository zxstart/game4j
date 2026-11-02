# Game4j 插件示例

这是一个独立且可直接运行的插件 Maven 模块，演示 `GamePlugin` 的最小实现和 Java SPI 注册方式。
它会单独构建为 `game4j-plugin-example` 可执行 JAR，绝不会被打包进 `game4j-plugin`。

## 构建

在仓库根目录执行：

```shell
mvn package
```

示例插件产物位于：

```text
examples/game4j-plugin-example/target/game4j-plugin-example-1.0-SNAPSHOT.jar
```

## 运行

构建完成后，直接执行：

```shell
java -jar examples/game4j-plugin-example/target/game4j-plugin-example-1.0-SNAPSHOT.jar
```

示例服务默认监听 `19000` 端口，集群与管理控制台均已关闭。按 `Ctrl+C` 停止服务。

启动入口调用 `GameBootstrap.run(ExampleApplication.class)`。Starter 的 `PluginInitializer` 会读取 JAR 内的：

```text
META-INF/services/com.game4j.plugin.GamePlugin
```

并自动加载 `ExamplePlugin`，控制台会显示插件已加载和启用的日志。可通过实现 `name`、`order`、`supports` 和生命周期方法创建自己的插件。

## Ping-Pong 请求示例

服务启动后，在另一个终端执行：

```shell
java -cp examples/game4j-plugin-example/target/game4j-plugin-example-1.0-SNAPSHOT.jar com.game4j.demo.DemoClient alice
```

客户端会保持连接，并每隔 5 秒发送 `cmd=1001` 的 `LoginRequest`。`DemoController` 接收后打印用户名，并发送 `cmd=1002` 的 `LoginResponse`。按 `Ctrl+C` 停止客户端。客户端会重复输出：

```text
登录响应: cmd=1002, code=200
```

请求和响应的消息体分别见 `LoginRequest` 和 `LoginResponse`；字段通过 `@Protobuf(order = ...)` 定义编码顺序。
