package ru.asmisloff.cloudStorage.client.core;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.CharsetUtil;
import ru.asmisloff.cloudStorage.common.ChannelUtil;
import ru.asmisloff.cloudStorage.common.CmdMsg;
import ru.asmisloff.cloudStorage.common.FileHandlerEventListener;

import java.io.File;

public class NetworkClient {

    boolean connected;

    private final String ROOT = "./client_files/";

    ChannelFuture cf;
    EventLoopGroup workerGroup;
    ClientFileHandler handler;
    private FileHandlerEventListener listener;

    public NetworkClient(FileHandlerEventListener listener) {
        this.listener = listener;
        connected = false;
    }

    private void run(String host, int port) throws Exception {
        workerGroup = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(workerGroup)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(new ClientFileHandler(ROOT, listener));
                        }
                    });

            cf = b.connect(host, port).sync();
            System.out.println("Connected");
            connected = true;

            cf.channel().closeFuture().sync();
            System.out.println("Stopped");
        } finally {
            workerGroup.shutdownGracefully();
        }
    }

    public void start(String host, int port) {
        new Thread(() -> {
            try {
                run(host, port);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void stop() {
        workerGroup.shutdownGracefully();
    }

    public Channel channel() {
        return cf.channel();
    }

    public String getRoot() {
        return ROOT;
    }

    public boolean isConnected() {
        return connected;
    }

    public void uploadFile(String path) {
        File f = new File(ROOT + path);
        FileRegion fr = new DefaultFileRegion(f, 0, f.length());
        byte[] arr = path.getBytes(CharsetUtil.UTF_8);
        ByteBuf bb = cf.channel().alloc().buffer(1 + 4 + arr.length + 8);
        bb  .writeByte(CmdMsg.RECEIVE_FILE.value())
            .writeInt(arr.length)
            .writeBytes(arr)
            .writeLong(f.length());
        cf.channel().write(bb);
        cf.channel().writeAndFlush(fr).addListener(
                future -> {
                    if (future.isSuccess()) {
                        System.out.printf("File sent -- %s\n", f.getPath());
                    }
                    else {
                        System.out.printf("Error while sending file -- %s\n", f.getPath());
                        System.out.println(future.cause().getMessage());
                    }
                });
    }

    public void downloadFile(String path) {
        ByteBuf temp = channel().alloc().buffer();
        temp.writeByte(CmdMsg.SEND_FILE.value());
        byte[] arr = path.getBytes(CharsetUtil.UTF_8);
        temp.writeInt(arr.length);
        temp.writeBytes(arr);
        channel().writeAndFlush(temp);
    }

    public void login(String login, String pwd) {
        ChannelUtil.writeString(channel(),
                                  CmdMsg.LOGIN.value(),
                                  String.format("%s/%s",login, pwd),
                             true);
    }

    public void requestFileInfo() {
        ByteBuf bb = channel().alloc().buffer(1);
        bb.writeByte(CmdMsg.FILE_INFO.value());
        channel().writeAndFlush(bb);
    }

}
