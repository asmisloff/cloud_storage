package ru.asmisloff.cloudStorage.client.core;

import io.netty.channel.ChannelHandlerContext;
import ru.asmisloff.cloudStorage.common.BaseFileHandler;

public class ClientFileHandler extends BaseFileHandler {

    public ClientFileHandler(String root) {
        super(root);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
    }

    @Override
    protected void onFileReceived(String path) {
        System.out.printf("File received -- %s\n", path);
    }

    @Override
    protected void onFileSent(String path) {
        System.out.printf("File sent -- %s\n", path);
    }
}
