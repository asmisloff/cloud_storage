package ru.asmisloff.cloudStorage.server.core;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import ru.asmisloff.cloudStorage.common.ChannelUtil;
import ru.asmisloff.cloudStorage.common.CmdMsg;
import ru.asmisloff.cloudStorage.common.FileHandlerEventListener;

public class AuthenticationHandler extends ChannelInboundHandlerAdapter {

    private boolean authorized;
    private FileHandlerEventListener listener;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (authorized) {
            ctx.fireChannelRead(msg);
            return;
        }

        ByteBuf m = (ByteBuf)msg;
        if (m.readByte() == CmdMsg.LOGIN.value()) {
            String s = ChannelUtil.readString(m);
            if (s == null) return;

            String[] regData = s.split("/");
            String login = regData[0];
            String pwd = regData[1];
            authorized = Database.checkLoginAndPassword(login, pwd);
            if (authorized) {
                ChannelUtil.writeString(ctx.channel(), CmdMsg.SERVICE_REPORT.value(), "Authentication accepted", true);
                ((ServerFileHandler)(ctx.pipeline().get(ServerFileHandler.class))).getListener().onAuthenticationAccepted(login, pwd);
            } else {
                ChannelUtil.writeString(ctx.channel(), CmdMsg.SERVICE_REPORT.value(), "Authentication rejected", true);
            }
        } else {
            ChannelUtil.writeString(ctx.channel(), CmdMsg.ERROR.value(), "Error: authorization is required", true)
                    .addListener(f -> {
                        Thread.sleep(100);
                        ctx.close();
                    });
        }
        m.release();
    }

    public AuthenticationHandler(FileHandlerEventListener listener) {
        this.listener = listener;
        authorized = false;
    }
}
