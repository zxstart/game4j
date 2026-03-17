package com.game4j.protocol;

public record GameMessage(int cmd, byte[] body) {
}
