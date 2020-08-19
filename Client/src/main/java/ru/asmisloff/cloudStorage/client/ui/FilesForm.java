package ru.asmisloff.cloudStorage.client.ui;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class FilesForm extends JDialog {
    private JPanel contentPane;
    private JButton btnCopy;
    private JButton btnUpdate;
    private JTable tblLocal;
    private JTable tblRemote;
    private JButton btnDelete;
    private FileTableModel localTblModel;
    private FileTableModel remoteTblModel;
    private final String[] data;
    private File clientRoot;
    private final UIController controller;
    private final Cursor defaultCursor;
    private final Cursor waitCursor;

    public FilesForm(UIController controller) {
        this.controller = controller;
        data = new String[2];

        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(btnCopy);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        btnCopy.addActionListener(this::onBtnCopyClicked);
        btnUpdate.addActionListener(this::onBtnUpdateClicked);
        btnDelete.addActionListener(this::onBtnDeleteClicked);

        defaultCursor = getCursor();
        waitCursor = new Cursor(Cursor.WAIT_CURSOR);

        initTables();
        updateLocalTable();
    }

    private void onBtnDeleteClicked(ActionEvent actionEvent) {
        int cnt;

        if ((cnt = tblLocal.getSelectedRowCount()) > 0) {
            setCursor(waitCursor);
            int[] sr = tblLocal.getSelectedRows();
            for (int i = 0; i < cnt; i++) {
                String fname = (String) localTblModel.getValueAt(sr[i], 0);
                try {
                    Files.deleteIfExists(Paths.get(controller.getClient().getRoot() + fname));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            updateLocalTable();
            resetCursor();
            return;
        }

        if ((cnt = tblRemote.getSelectedRowCount()) > 0) {
            int[] sr = tblRemote.getSelectedRows();
            for (int i = 0; i < cnt; i++) {
                String fname = (String) remoteTblModel.getValueAt(sr[i], 0);
                controller.getClient().deleteFile(fname);
            }
            tblRemote.clearSelection();
            return;
        }
    }

    private void onBtnUpdateClicked(ActionEvent actionEvent) {
        updateLocalTable();
        controller.getClient().requestFileInfo();
    }

    private void onBtnCopyClicked(ActionEvent actionEvent) {
        int cnt;

        if ((cnt = tblLocal.getSelectedRowCount()) > 0) {
            setCursor(waitCursor);
            int[] sr = tblLocal.getSelectedRows();
            for (int i = 0; i < cnt; i++) {
                String fname = (String) localTblModel.getValueAt(sr[i], 0);
                controller.getClient().uploadFile(fname);
            }
            tblLocal.clearSelection();
//            controller.getClient().requestFileInfo();
            return;
        }

        if ((cnt = tblRemote.getSelectedRowCount()) > 0) {
            setCursor(waitCursor);
            int[] sr = tblRemote.getSelectedRows();
            for (int i = 0; i < cnt; i++) {
                String fname = (String) remoteTblModel.getValueAt(sr[i], 0);
                controller.getClient().downloadFile(fname);
            }
            tblRemote.clearSelection();
            return;
        }
    }

    private void initTables() {
        Object[] columns = {"Name","Size"};
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        rightRenderer.setBorder(BorderFactory.createEmptyBorder(0,0,0,10));

        localTblModel = new FileTableModel();
        localTblModel.setColumnIdentifiers(columns);
        tblLocal.setModel(localTblModel);
        tblLocal.getColumn("Name").setPreferredWidth(300);
        tblLocal.getColumn("Size").setPreferredWidth(30);
        tblLocal.getColumn("Size").setCellRenderer(rightRenderer);
        tblLocal.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == 27) {
                    tblLocal.clearSelection();
                }
            }
        });
        tblLocal.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int btn = e.getButton();
                switch (btn) {
                    case 1:
                        tblRemote.clearSelection();
                        int r = tblLocal.rowAtPoint(e.getPoint());
                        if (r == -1) {
                            tblLocal.clearSelection();
                        }
                        break;
                    case 3:
                        tblLocal.clearSelection();
                        break;
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                tblRemote.clearSelection();
            }
        });

        remoteTblModel = new FileTableModel();
        remoteTblModel.setColumnIdentifiers(columns);
        tblRemote.setModel(remoteTblModel);
        tblRemote.getColumn("Name").setPreferredWidth(300);
        tblRemote.getColumn("Size").setPreferredWidth(30);
        tblRemote.getColumn("Size").setCellRenderer(rightRenderer);
        tblRemote.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == 27) {
                    tblRemote.clearSelection();
                }
            }
        });
        tblRemote.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int btn = e.getButton();
                switch (btn) {
                    case 1:
                        tblLocal.clearSelection();
                        int r = tblRemote.rowAtPoint(e.getPoint());
                        if (r == -1) {
                            tblRemote.clearSelection();
                        }
                        break;
                    case 3:
                        tblRemote.clearSelection();
                        break;
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                tblLocal.clearSelection();
            }
        });
    }

    public void updateRemoteTable(String fileInfo) {
        if (fileInfo.isEmpty()) {
            remoteTblModel.setRowCount(0);
            return;
        }
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

    public void resetCursor() {
        setCursor(defaultCursor);
    }

    private class FileTableModel extends DefaultTableModel {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    }
}
