package ru.asmisloff.cloudStorage.client.core;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.CharsetUtil;
import ru.asmisloff.cloudStorage.common.CmdMsg;

import java.io.File;

public class NetworkClient {

    boolean connected;
    private final String ROOT = "./client_files/";

    ChannelFuture cf;
    EventLoopGroup workerGroup;
    public NetworkClient() {
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
                            ch.pipeline().addLast(new ClientFileHandler(ROOT));
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

    void stop() {
        workerGroup.shutdownGracefully();
    }

    public Channel channel() {
        return cf.channel();
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
                future -> System.out.printf("File sent -- %s\n", f.getPath()));
    }

    public void downloadFile(String path) {
        ByteBuf temp = channel().alloc().buffer();
        temp.writeByte(CmdMsg.SEND_FILE.value());
        byte[] arr = path.getBytes(CharsetUtil.UTF_8);
        temp.writeInt(arr.length);
        temp.writeBytes(arr);
        channel().writeAndFlush(temp);
    }
}