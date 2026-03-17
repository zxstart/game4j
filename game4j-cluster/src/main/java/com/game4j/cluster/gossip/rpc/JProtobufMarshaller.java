package com.game4j.cluster.gossip.rpc;

import com.baidu.bjf.remoting.protobuf.Codec;
import com.baidu.bjf.remoting.protobuf.ProtobufProxy;
import io.grpc.MethodDescriptor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class JProtobufMarshaller<T> implements MethodDescriptor.Marshaller<T> {
    private final Codec<T> codec;

    public JProtobufMarshaller(Class<T> type) {
        this.codec = ProtobufProxy.create(type);
    }

    @Override
    public InputStream stream(T value) {
        try {
            return new ByteArrayInputStream(codec.encode(value));
        } catch (IOException ex) {
            throw new IllegalStateException("JProtobuf encode failed", ex);
        }
    }

    @Override
    public T parse(InputStream stream) {
        try {
            return codec.decode(stream.readAllBytes());
        } catch (IOException ex) {
            throw new IllegalStateException("JProtobuf decode failed", ex);
        }
    }
}
