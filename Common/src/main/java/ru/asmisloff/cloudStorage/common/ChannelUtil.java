package ru.asmisloff.cloudStorage.common;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.util.CharsetUtil;

public class ChannelUtil {
    public static ChannelFuture writeString(Channel channel, byte cmdByte, String s, boolean flush) {
        byte[] bs = s.getBytes(CharsetUtil.UTF_8);
        ByteBuf tmp = channel.alloc().buffer(5 + bs.length);
        tmp.writeByte(cmdByte);
        tmp.writeInt(bs.length);
        tmp.writeBytes(bs);
        ChannelFuture f = flush ? channel.writeAndFlush(tmp) : channel.write(tmp);
        return f;
    }

    public static String readString(ByteBuf bb) {
        int strLength = getStrLenIfReady(bb);

        if (strLength == -1) {
            return null;
        }

        String result = bb.toString(4, strLength, CharsetUtil.UTF_8);
        bb.skipBytes(4 + strLength);
        bb.discardReadBytes();

        return result;
    }

    public static int getStrLenIfReady(ByteBuf bb) {
        bb.discardReadBytes();
        int rb = bb.readableBytes();
        if (rb < 4) {
            return -1;
        }

        int strLength = bb.getInt(0);
        if (rb < 4 + strLength) {
            return -1;
        }

        return strLength;
    }
}
