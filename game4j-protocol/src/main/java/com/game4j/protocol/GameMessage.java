package com.game4j.protocol;

/**
 * 游戏消息记录
 * <p>
 * 用于在网络传输中封装消息的命令ID和消息体。
 * 使用 Java 14+ 的 record 特性。
 * </p>
 *
 * @param cmd 命令ID
 * @param body 消息体字节数组 (通常是 Protobuf 序列化后的数据)
 */
public record GameMessage(int cmd, byte[] body) {
}
