package ru.asmisloff.cloudStorage.server.core;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.util.CharsetUtil;
import ru.asmisloff.cloudStorage.common.CmdMsg;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

// TODO: 10.08.2020 Избавиться от дублирования кода здесь и в клиентском Handler-е. Вынести общую логику в базовый класс.
public class ServerFileHandler extends ChannelInboundHandlerAdapter {

    private Channel channel;
    private final ByteBuf bb;

    /*
    * Объекты-обработчики байт, поступающих из входного канала.
    * Активный обработчик, завершив свою работу, назначает активным следующий в зависимости от контекста.
    */
    private RequestProcessor activeRequestExecutor;
    private final RequestProcessor dispatcher;
    private final RequestProcessor fileUploader;
    private final RequestProcessor uploadCmdReader;
    private final RequestProcessor fileDownloader;
    private final RequestProcessor downloadCommandReader;

    public ServerFileHandler() {
        bb = Unpooled.buffer(2048);
        dispatcher = new Dispatcher();
        uploadCmdReader = new UploadCmdReader();
        fileUploader = new FileUploader();
        downloadCommandReader = new DownloadCommandReader();
        fileDownloader = new FileDownloader();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        System.out.println("Client disconnected");
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        this.channel = ctx.channel();
        activeRequestExecutor = dispatcher;
        System.out.println("Client connected");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf m = (ByteBuf)msg;
        bb.writeBytes(m);
        activeRequestExecutor.execute(bb);
        m.release();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        System.out.println("Network error on client");
    }

    /*Request processors*/
    // TODO: 10.08.2020 Избавиться от дублирования кода: вынести их в модуль Common.

    interface RequestProcessor {
        void execute(ByteBuf bb) throws Exception;
    }

    class Dispatcher implements RequestProcessor {
        @Override
        public void execute(ByteBuf bb) throws Exception {
            if (bb.readableBytes() < 1) {
                return;
            }
            byte cmd = bb.readByte();
            bb.discardReadBytes();

            if (cmd == CmdMsg.UPLOAD.value()) {
                activeRequestExecutor = uploadCmdReader;
            } else if (cmd == CmdMsg.DOWNLOAD.value()) {
                activeRequestExecutor = downloadCommandReader;
            } else {
                throw new Exception("Unknown command message: " + cmd);
            }

            activeRequestExecutor.execute(bb);
        }
    }

    class UploadCmdReader implements RequestProcessor {
        @Override
        public void execute(ByteBuf bb) throws Exception {
            int rb = bb.readableBytes();
            if (rb < 4) {
                return;
            }

            int pathLength = bb.getInt(0);
            if (rb < 4 + pathLength + 8) {
                return;
            }

            long fileSize = bb.getLong(4 + pathLength);
            String path = bb.toString(4, pathLength, CharsetUtil.UTF_8);
            bb.skipBytes(4 + pathLength + 8);
            bb.discardReadBytes();

            activeRequestExecutor = fileUploader;
            ((FileUploader)fileUploader).setup(path, fileSize);
            activeRequestExecutor.execute(bb);
        }
    }

    class FileUploader implements RequestProcessor {

        private long fileSize;
        private RandomAccessFile raf;

        @Override
        public void execute(ByteBuf bb) throws IOException {
            int rb = bb.readableBytes();
            if (rb > 0 && fileSize != 0) {
                int qty = (int)(fileSize > rb ? rb : fileSize);
                fileSize -= raf.getChannel().write(bb.nioBuffer(0, qty));
                bb.skipBytes(qty);
                bb.discardReadBytes();
            }
            if (fileSize <= 0) {
                raf.getChannel().force(true);
                raf.close();
                // TODO: 10.08.2020 send response (or not)
                System.out.println("File uploaded");

                activeRequestExecutor = dispatcher;
            }
        }

        public void setup(String path, long fileSize) throws IOException {
            this.fileSize = fileSize;
            raf = new RandomAccessFile("./server_files/" + path, "rw");
            raf.setLength(0);
        }
    }

    class DownloadCommandReader implements RequestProcessor {
        @Override
        public void execute(ByteBuf bb) throws Exception {
            int rb = bb.readableBytes();
            if (rb < 4) {
                return;
            }

            int pathLength = bb.getInt(0);
            System.out.println(pathLength);
            if (rb < 4 + pathLength) {
                return;
            }

            String path = bb.toString(4, pathLength, CharsetUtil.UTF_8);
            System.out.println(path);
            bb.skipBytes(4 + pathLength);
            bb.discardReadBytes();

            activeRequestExecutor = fileDownloader;
            ((FileDownloader)fileDownloader).setup(path);
            activeRequestExecutor.execute(bb);
        }
    }

    class FileDownloader implements RequestProcessor {

        private String path;

        @Override
        public void execute(ByteBuf bb) {
            File f = new File("./server_files/" + path);
            FileRegion fr = new DefaultFileRegion(f, 0, f.length());
            byte[] bytePath = path.getBytes(CharsetUtil.UTF_8);
            ByteBuf temp = channel.alloc().buffer(1 + 4 + bytePath.length + 8);
            temp.writeByte(CmdMsg.UPLOAD.value())
                .writeInt(bytePath.length)
                .writeBytes(bytePath)
                .writeLong(f.length());
            channel.write(temp);
            channel.writeAndFlush(fr).addListener(
                    (ChannelFutureListener) future -> System.out.println("File sent")
            );
            activeRequestExecutor = dispatcher;
        }

        public void setup(String path) {
            this.path = path;
        }
    }
}
