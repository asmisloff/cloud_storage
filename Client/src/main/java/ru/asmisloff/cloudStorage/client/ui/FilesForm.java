package ru.asmisloff.cloudStorage.client.ui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.ActionEvent;
import java.io.File;

public class FilesForm extends JDialog {
    private JPanel contentPane;
    private JButton btnCopy;
    private JButton btnExit;
    private JTable tblLocal;
    private JTable tblRemote;
    private FileTableModel localTblModel;
    private FileTableModel remoteTblModel;
    private String[] data;
    private File clientRoot;
    private UIController controller;

    public FilesForm(UIController controller) {
        this.controller = controller;
        data = new String[2];

        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(btnCopy);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        btnCopy.addActionListener(this::onBtnCopyClicked);

        initTables();
        updateLocalTable();
    }

    private void onBtnCopyClicked(ActionEvent actionEvent) {
        String fname = (String) localTblModel.getValueAt(tblLocal.getSelectedRow(), 0);
        controller.getClient().uploadFile(fname);
    }

    private void initTables() {
        Object[] columns = {"Name","Size"};
        localTblModel = new FileTableModel();
        remoteTblModel = new FileTableModel();
        localTblModel.setColumnIdentifiers(new Object[] {"Name","Size"});
        remoteTblModel.setColumnIdentifiers(columns);
        tblLocal.setModel(localTblModel);
        tblRemote.setModel(remoteTblModel);
    }

    public void updateRemoteTable(String fileInfo) {
        remoteTblModel.setRowCount(0);
        String[] tokens = fileInfo.split("\\\\\\\\");
        for (int i = 0; i < tokens.length; i++) {
            String[] attr = tokens[i].split("\\\\");
            data[0] = attr[0];
            data[1] = attr[1];
            remoteTblModel.addRow(data);
        }
    }

    public void updateLocalTable() {
        clientRoot = new File(controller.getClient().getRoot());
        localTblModel.setRowCount(0);
        File[] files = clientRoot.listFiles();

        for (int i = 0; i < files.length; i++) {
            File f = files[i];
            data[0] = f.getName();
            data[1] = String.valueOf(f.length());
            localTblModel.addRow(data);
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        System.exit(0);
    }

    private class FileTableModel extends DefaultTableModel {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    }
}
