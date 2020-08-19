package ru.asmisloff.cloudStorage.common;

public class FileHandlerEventListener {

    protected BaseFileHandler handler;

    public FileHandlerEventListener newInstance() {
        return new FileHandlerEventListener();
    }

    public void onAuthenticationAccepted(String login, String pwd) {
        log("Authentication accepted");
    }

    public void onFileReceived(String path) {
        log(String.format("File received -- %s\n", path));
    }

    public void onFileSent(String path) {
        log(String.format("File sent -- %s", path));
    }

    public void onServiceReportReceived(String msg) {
        log(msg);
    }

    public void onFileInfoReceived(String fileinfo) {
        log(fileinfo);
    }

    protected void log(String s) {
        System.out.println(s);
    }

    public BaseFileHandler getHandler() {
        return handler;
    }

    public void setHandler(BaseFileHandler handler) {
        this.handler = handler;
    }

    public void onFileDeleted(String path) {
        log("File deleted -- " + path);
    }
}
