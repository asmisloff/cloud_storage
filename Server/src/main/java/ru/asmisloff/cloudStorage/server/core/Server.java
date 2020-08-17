package ru.asmisloff.cloudStorage.server.core;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import ru.asmisloff.cloudStorage.common.FileHandlerEventListener;

public class Server {
    private final int PORT;
    private final String ROOT = "./server_files/";
    private FileHandlerEventListener listenerFabric;

    public Server(int port, FileHandlerEventListener listener) {
        PORT = port;
        listenerFabric = listener;
    }

    public void run() throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap
                    .group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        protected void initChannel(SocketChannel socketChannel) {
                            socketChannel.pipeline().addLast(new AuthenticationHandler(listenerFabric.newInstance()));
                            socketChannel.pipeline().addLast(
                                    new ServerFileHandler(ROOT, listenerFabric.newInstance()));
                        }
                    })
            .option(ChannelOption.SO_BACKLOG, 128)
            .childOption(ChannelOption.SO_KEEPALIVE, true);
            ChannelFuture f = bootstrap.bind(PORT).sync();
            System.out.println("Server started");
            Database.connect();
            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws Exception {
        new Server(8181, new ServerFileHandlerEventListener()).run();
    }
}
