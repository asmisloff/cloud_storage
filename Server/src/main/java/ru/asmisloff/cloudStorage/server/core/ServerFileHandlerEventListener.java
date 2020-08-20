package ru.asmisloff.cloudStorage.server.core;

import io.netty.buffer.ByteBuf;
import io.netty.util.CharsetUtil;
import ru.asmisloff.cloudStorage.common.CmdMsg;
import ru.asmisloff.cloudStorage.common.FileHandlerEventListener;

import java.io.File;

public class ServerFileHandlerEventListener extends FileHandlerEventListener {

    @Override
    public ServerFileHandlerEventListener newInstance() {
        return new ServerFileHandlerEventListener();
    }

    @Override
    public void onAuthenticationAccepted(String login, String pwd) {
        super.onAuthenticationAccepted(login, pwd);
        String newRoot = handler.getROOT() + login + "/";
        handler.setROOT(newRoot);
        System.out.println("ROOT = " + handler.getROOT());
        File f = new File(newRoot);
        if (!f.exists()) {
            f.mkdir();
        }
    }

    @Override
    public void onFileReceived(String path) {
        System.out.printf("File received -- %s\n", path);
        String resp = String.format("SERVICE REPORT: \"%s\" successfully uploaded", path);
        byte[] arr = resp.getBytes(CharsetUtil.UTF_8);
        ByteBuf tmp = handler.channel().alloc().buffer(5 + arr.length);
        tmp.writeByte(CmdMsg.SERVICE_REPORT.value());
        tmp.writeInt(arr.length);
        tmp.writeBytes(arr);
        handler.channel().writeAndFlush(tmp);
    }
}
