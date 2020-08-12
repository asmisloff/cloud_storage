package ru.asmisloff.cloudStorage.common;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.util.CharsetUtil;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;

public abstract class BaseFileHandler extends ChannelInboundHandlerAdapter {

    protected Channel channel;
    protected final ByteBuf bb;

    protected HashMap<Byte, ByteBufProcessor> dispMap;

    /*
    * Объекты-обработчики байтов, поступающих из входного канала.
    * Активный обработчик, завершив свою работу, назначает активным следующий в зависимости от контекста.
    */
    protected ByteBufProcessor activeProcessor;
    protected final ByteBufProcessor dispatcher;
    protected final ByteBufProcessor fileReceiver;
    protected final ByteBufProcessor receiveFileCmdReader;
    protected final ByteBufProcessor fileSender;
    protected final ByteBufProcessor sendFileCmdReader;
    protected final ByteBufProcessor serviceReportReader;

    private final String ROOT; // Путь к корневой папке

    public BaseFileHandler(String root) {
        ROOT = root;
        bb = Unpooled.buffer(2048);
        dispMap = new HashMap<>(3);

        dispatcher = new Dispatcher();

        receiveFileCmdReader = new ReceiveFileCmdReader();
        dispMap.put(CmdMsg.RECEIVE_FILE.value(), receiveFileCmdReader);

        fileReceiver = new FileReceiver();

        sendFileCmdReader = new SendFileCmdReader();
        dispMap.put(CmdMsg.SEND_FILE.value(), sendFileCmdReader);

        fileSender = new FileSender();

        serviceReportReader = new ServiceReportReader();
        dispMap.put(CmdMsg.SERVICE_REPORT.value(), serviceReportReader);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        this.channel = ctx.channel();
        activeProcessor = dispatcher;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf m = (ByteBuf)msg;
        bb.writeBytes(m);
        activeProcessor.execute(bb);
        m.release();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
    }

    protected abstract void onFileReceived(String path);

    protected abstract void onFileSent(String path);

    protected void onServiceReportReceived(String msg) {
        System.out.println(msg);
    }

    protected interface ByteBufProcessor {
        void execute(ByteBuf bb) throws Exception;
    }

    /**
     * Читает командный байт и делегирует работу соотв. обработчику
     **/
    class Dispatcher implements ByteBufProcessor {

        @Override
        public void execute(ByteBuf bb) throws Exception {
            if (bb.readableBytes() < 1) {
                return;
            }
            byte cmd = bb.readByte();
            bb.discardReadBytes();

            // TODO: 12.08.2020 Сделать диспетчеризацию через HashMap<CmdMsg, ByteBufProcessor>
//            if (cmd == CmdMsg.RECEIVE_FILE.value()) {
//                activeProcessor = receiveFileCmdReader;
//            } else if (cmd == CmdMsg.SEND_FILE.value()) {
//                activeProcessor = sendFileCmdReader;
//            } else if (cmd == CmdMsg.UPLOADED_SUCCESSFULLY.value()) {
//                activeProcessor = serviceReportReader;
            activeProcessor = dispMap.get(cmd);
            if (activeProcessor == null) {
                throw new Exception("Unknown command message: " + cmd);
            }

            activeProcessor.execute(bb);
        }
    }

    /**
     * Считывает из входного канала команду принять файл.
     * Протокол: <длина имени файла: int> -- <имя файла в utf-8> -- <размер файла: long>
     * По прочтении команды обработка передается объекту FileReceiver
     * */
    class ReceiveFileCmdReader implements ByteBufProcessor {

        @Override
        public void execute(ByteBuf bb) throws Exception {
            int pathLength = ChannelUtil.getStrLenIfReady(bb);
            if (pathLength == -1 || bb.readableBytes() < 4 + pathLength + 8) {
                return;
            }

            String path = ChannelUtil.readString(bb);
            long fileSize = bb.readLong();

            activeProcessor = fileReceiver;
            ((FileReceiver) fileReceiver).setup(path, fileSize);
            activeProcessor.execute(bb);
        }
    }

    /**
     * Читает файл. Имя и длина файла передаются предыдущим обработчиком в метод setup.
     * */
    class FileReceiver implements ByteBufProcessor {

        private long fileSize;
        private String path;
        private RandomAccessFile raf;

        @Override
        public void execute(ByteBuf bb) throws Exception {
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

                onFileReceived(path);

                activeProcessor = dispatcher;
                activeProcessor.execute(bb);
            }
        }

        public void setup(String path, long fileSize) throws IOException {
            this.path = path;
            this.fileSize = fileSize;
            raf = new RandomAccessFile(ROOT + path, "rw");
            raf.setLength(0);
            bb.discardReadBytes(); // Important! execute critically depends on that!
        }
    }

    /**
     * Принимает запрос на отправку файла.
     * Протокол: <длина имени файла: int> -- <Имя файла в utf-8>
     * */
    class SendFileCmdReader implements ByteBufProcessor {
        @Override
        public void execute(ByteBuf bb) throws Exception {
            String path = ChannelUtil.readString(bb);
            if (path == null) {
                return;
            }

            activeProcessor = fileSender;
            ((FileSender) fileSender).setup(path);
            activeProcessor.execute(bb);
        }
    }

    /**
     * Отправляет файл в выходной канал.
     * Протокол: <Длина имени файла: int> -- <Имя файла в utf-8> -- <Длина файла: long> -- <файл>
     * */
    class FileSender implements ByteBufProcessor {

        private String path;

        @Override
        public void execute(ByteBuf bb) throws Exception {
            File f = new File(ROOT + path);
            FileRegion fr = new DefaultFileRegion(f, 0, f.length());
            byte[] bytePath = path.getBytes(CharsetUtil.UTF_8);
            ByteBuf temp = channel.alloc().buffer(1 + 4 + bytePath.length + 8);
            temp.writeByte(CmdMsg.RECEIVE_FILE.value())
                    .writeInt(bytePath.length)
                    .writeBytes(bytePath)
                    .writeLong(f.length());
            channel.write(temp);
            channel.writeAndFlush(fr).addListener(
                    future -> onFileSent(path));
            activeProcessor = dispatcher;
            activeProcessor.execute(bb);
        }

        public void setup(String path) {
            this.path = path;
        }
    }

    /**
     * Читает строку сервисного сообщения или отчета.
     * Протокол: <длина строки: int> <строка в utf-8>.
     * Вызывает метод onServiceReportReceived() и передает обработоку канала диспетчеру.
     * */
    public class ServiceReportReader implements ByteBufProcessor {
        @Override
        public void execute(ByteBuf bb) throws Exception {
            String msg = ChannelUtil.readString(bb);
            if (msg != null) {
                onServiceReportReceived(msg);
                activeProcessor = dispatcher;
                activeProcessor.execute(bb);
            }
        }
    }

}
