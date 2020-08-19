package ru.asmisloff.cloudStorage.client.ui;

import ru.asmisloff.cloudStorage.client.core.NetworkClient;
import ru.asmisloff.cloudStorage.common.FileHandlerEventListener;

import javax.swing.*;

public class UIController extends FileHandlerEventListener {

    private LoginDialog loginDialog;
    private FilesForm filesForm;

    private final NetworkClient client;
    public NetworkClient getClient() {
        return client;
    }

    public UIController() {
        client = new NetworkClient(this);
    }

    @Override
    public void onServiceReportReceived(String msg) {
        super.onServiceReportReceived(msg);

        if (msg.equals("Authentication accepted")) {
            loginDialog.dispose();
            SwingUtilities.invokeLater(() -> {
                filesForm = new FilesForm(this);
                filesForm.pack();
                filesForm.setVisible(true);
            });

            while (filesForm == null) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            client.requestFileInfo();
        } else if (msg.equals("Authentication rejected")) {
            JOptionPane.showMessageDialog(loginDialog, "Authentication rejected");
        } else if (msg.contains("successfully uploaded")) {
            filesForm.resetCursor();
            client.requestFileInfo();
        } else if (msg.contains("deleted")) {
            client.requestFileInfo();
        }
    }

    @Override
    public void onFileReceived(String path) {
        super.onFileReceived(path);
        filesForm.resetCursor();
        filesForm.updateLocalTable();
    }

    @Override
    public void onFileInfoReceived(String fileInfo) {
        log(String.format("FileInfo received: %s\n", fileInfo));
        filesForm.updateRemoteTable(fileInfo);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            UIController controller = new UIController();
            controller.loginDialog = new LoginDialog(controller);
            controller.loginDialog.pack();
            controller.loginDialog.setVisible(true);
        });
    }
}
