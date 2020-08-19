package ru.asmisloff.cloudStorage.common;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;

/**
 * Костыль. Определен ради поля msg.
 * Решает проблему с некорректным логированием из FileHandlerEventListener.onFileSent.
 * Проблема возникает из-за асинхроного доступа к BaseFileHandler.FileSender.path.
 * */
public class CachedChannelFutureListener implements ChannelFutureListener {

    private final String msg;

    public CachedChannelFutureListener(String msg) {
        this.msg = msg;
    }

    @Override
    public void operationComplete(ChannelFuture future) throws Exception {

    }

    public String getMsg() {
        return msg;
    }
}
