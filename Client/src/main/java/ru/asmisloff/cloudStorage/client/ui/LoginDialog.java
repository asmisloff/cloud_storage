package ru.asmisloff.cloudStorage.client.ui;

import ru.asmisloff.cloudStorage.client.core.NetworkClient;

import javax.swing.*;

public class LoginDialog extends JDialog {

    UIController controller;

    private JPanel contentPane;
    private JButton btnLogin;
    private JTextField tfHost;
    private JTextField tfPort;
    private JTextField tfLogin;
    private JTextField tfPwd;
    private JLabel lbHost;
    private JLabel lbPort;
    private JLabel lbLogin;
    private JLabel lbPwd;

    public LoginDialog(UIController controller) {
        this.controller = controller;
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(btnLogin);

        btnLogin.addActionListener(e -> onBtnLoginClicked());

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
    }

    private void onBtnLoginClicked() {
        NetworkClient client = controller.getClient();
        if (!client.isConnected()) {
            client.start(tfHost.getText(), Integer.parseInt(tfPort.getText()));
        }
        while (!client.isConnected()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        client.login(tfLogin.getText(), tfPwd.getText());
    }
}
