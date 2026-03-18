package com.game4j.cluster.gossip.rpc;

import com.baidu.bjf.remoting.protobuf.Codec;
import com.baidu.bjf.remoting.protobuf.ProtobufProxy;
import io.grpc.MethodDescriptor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * JProtobuf 序列化器
 * 实现 gRPC 的 Marshaller 接口，使用 JProtobuf 进行消息的序列化和反序列化
 * 支持泛型，可用于任何 JProtobuf 支持的消息类型
 *
 * @param <T> 消息类型
 */
public class JProtobufMarshaller<T> implements MethodDescriptor.Marshaller<T> {
    /**
     * JProtobuf 编解码器
     */
    private final Codec<T> codec;

    /**
     * 构造 JProtobuf 序列化器
     *
     * @param type 消息类型
     */
    public JProtobufMarshaller(Class<T> type) {
        this.codec = ProtobufProxy.create(type);
    }

    /**
     * 将消息序列化为输入流
     *
     * @param value 要序列化的消息对象
     * @return 包含序列化数据的输入流
     */
    @Override
    public InputStream stream(T value) {
        try {
            return new ByteArrayInputStream(codec.encode(value));
        } catch (IOException ex) {
            throw new IllegalStateException("JProtobuf encode failed", ex);
        }
    }

    /**
     * 从输入流反序列化消息
     *
     * @param stream 包含序列化数据的输入流
     * @return 反序列化后的消息对象
     */
    @Override
    public T parse(InputStream stream) {
        try {
            return codec.decode(stream.readAllBytes());
        } catch (IOException ex) {
            throw new IllegalStateException("JProtobuf decode failed", ex);
        }
    }
}
