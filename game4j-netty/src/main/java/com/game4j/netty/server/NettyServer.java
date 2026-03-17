package com.game4j.netty.server;

import com.game4j.netty.codec.ProtoDecoder;
import com.game4j.netty.codec.ProtoEncoder;
import com.game4j.netty.handler.GameChannelHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

/**
 * Netty 服务端
 * <p>
 * 负责启动 Netty 服务，配置 ChannelPipeline。
 * </p>
 */
public class NettyServer {

    /**
     * 启动服务器
     * 
     * @param port 监听端口
     * @throws Exception 启动异常
     */
    public void start(int port) throws Exception {

        EventLoopGroup boss = new NioEventLoopGroup(1);
        EventLoopGroup worker = new NioEventLoopGroup();

        ServerBootstrap bootstrap = new ServerBootstrap();

        bootstrap.group(boss, worker)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<>() {

                    @Override
                    protected void initChannel(Channel ch) {

                        ch.pipeline()
                                .addLast(new ProtoDecoder())
                                .addLast(new ProtoEncoder())
                                .addLast(new GameChannelHandler());

                    }

                });

        bootstrap.bind(port).sync();

        System.out.println("Game Server Start : " + port);
    }

}
