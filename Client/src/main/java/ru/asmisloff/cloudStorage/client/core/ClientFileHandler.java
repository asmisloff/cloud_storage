package ru.asmisloff.cloudStorage.client.core;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import ru.asmisloff.cloudStorage.common.BaseFileHandler;
import ru.asmisloff.cloudStorage.common.ChannelUtil;
import ru.asmisloff.cloudStorage.common.CmdMsg;
import ru.asmisloff.cloudStorage.common.FileHandlerEventListener;

public class ClientFileHandler extends BaseFileHandler {

    private class FileInfoReceiver implements ByteBufProcessor {

        @Override
        public void execute(ByteBuf bb) throws Exception {
            String s = ChannelUtil.readString(bb);
            if (s != null) {
                listener.onFileInfoReceived(s);
                activeProcessor = dispatcher;
            }
        }
    }

    public ClientFileHandler(String root, FileHandlerEventListener listener) {
        super(root, listener);

        // TODO: 13.08.2020 Возможно, для сообщений CmdMsg.Error нужен отдельный обработчик
        dispMap.put(CmdMsg.ERROR.value(), new ServiceReportReader());
        dispMap.put(CmdMsg.FILE_INFO.value(), new FileInfoReceiver());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
    }

}
