package ru.asmisloff.cloudStorage.server.core;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import ru.asmisloff.cloudStorage.common.ChannelUtil;
import ru.asmisloff.cloudStorage.common.CmdMsg;

public class AuthenticationHandler extends ChannelInboundHandlerAdapter {

    private boolean authorized;

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
            authorized = Database.checkLoginAndPassword(regData[0], regData[1]);
            if (authorized) {
                ChannelUtil.writeString(ctx.channel(), CmdMsg.SERVICE_REPORT.value(), "Authentication accepted", true);
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

    public AuthenticationHandler() {
        authorized = false;
    }
}
