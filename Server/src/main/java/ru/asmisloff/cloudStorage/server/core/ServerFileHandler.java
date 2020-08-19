package ru.asmisloff.cloudStorage.server.core;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import ru.asmisloff.cloudStorage.common.BaseFileHandler;
import ru.asmisloff.cloudStorage.common.ChannelUtil;
import ru.asmisloff.cloudStorage.common.CmdMsg;
import ru.asmisloff.cloudStorage.common.FileHandlerEventListener;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ServerFileHandler extends BaseFileHandler {

    /**
     * Отправляет клиенту список файлов в форме строки с разделителями:
     * file1\size1\\file2\size\file3\[dir]\file4\size4
     * */
    private class FileInfoSender implements ByteBufProcessor {

        @Override
        public void execute(ByteBuf bb) throws Exception {
            File[] files = new File(ROOT).listFiles();
            StringBuilder b = new StringBuilder();
            for (int i = 0; i < files.length; i++) {
                File f = files[i];
                String attr = f.isDirectory() ? "[DIR]" : String.valueOf(f.length());
                b.append(String.format("%s\\%s\\\\", f.getName(), attr));
            }
            ChannelUtil.writeString(channel, CmdMsg.FILE_INFO.value(), b.toString(), true);

            activeProcessor = dispatcher;
            activeProcessor.execute(bb);
        }
    }

    private class FileDeleter implements ByteBufProcessor {
        @Override
        public void execute(ByteBuf bb) throws Exception {
            String path = ChannelUtil.readString(bb);
            String msg;
            if (Files.deleteIfExists(Paths.get(ROOT + path))) {
                msg = String.format("File \"%s\" deleted", path);
            } else {
                msg = String.format("File \"%s\" can't be deleted", path);
            }
            ChannelUtil.writeString(channel, CmdMsg.SERVICE_REPORT.value(), msg, true);
            listener.onFileDeleted(path);
            activeProcessor = dispatcher;
            activeProcessor.execute(bb);
        }
    }

    public ServerFileHandler(String root, FileHandlerEventListener listener) {
        super(root, listener);
        dispMap.put(CmdMsg.FILE_INFO.value(), new FileInfoSender());
        dispMap.put(CmdMsg.DELETE.value(), new FileDeleter());
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

}
