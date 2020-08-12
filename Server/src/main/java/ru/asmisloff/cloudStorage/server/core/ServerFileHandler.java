package ru.asmisloff.cloudStorage.server.core;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.CharsetUtil;
import ru.asmisloff.cloudStorage.common.BaseFileHandler;
import ru.asmisloff.cloudStorage.common.CmdMsg;

public class ServerFileHandler extends BaseFileHandler {

    public ServerFileHandler(String root) {
        super(root);
//        dispMap.put(CmdMsg.LOGIN.value(), new AuthenticationProcessor());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        System.out.println("Client disconnected");
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        super.channelActive(ctx);
        System.out.println("Client connected");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        System.out.println("-------------------------------");
        System.out.println(cause.getMessage());
        System.out.println("-------------------------------");
    }

    @Override
    protected void onFileReceived(String path) {
        System.out.printf("File received -- %s\n", path);
        String resp = String.format("SERVICE REPORT: \"%s\" successfully uploaded", path);
        byte[] arr = resp.getBytes(CharsetUtil.UTF_8);
        ByteBuf tmp = channel.alloc().buffer(5 + arr.length);
        tmp.writeByte(CmdMsg.SERVICE_REPORT.value());
        tmp.writeInt(arr.length);
        tmp.writeBytes(arr);
        channel.writeAndFlush(tmp);
    }

    @Override
    protected void onFileSent(String path) {
        System.out.printf("File sent -- %s\n", path);
    }

}
