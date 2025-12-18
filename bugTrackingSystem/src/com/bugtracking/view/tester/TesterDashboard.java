package com.bugtracking.view.tester;

import com.bugtracking.controller.FileManager;
import com.bugtracking.model.Bug;
import com.bugtracking.model.User;
import com.bugtracking.view.MainFrame;
import com.bugtracking.view.components.NotificationDialog; // Import Added

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;

public class TesterDashboard extends JPanel {
    private User currentUser;
    private MainFrame mainFrame;
    private FileManager fileManager;

    // UI Components
    private JTextField txtTitle;
    private JTextArea txtDesc;
    private JComboBox<String> comboPriority;
    private JComboBox<String> comboDevs;
    private ArrayList<User> developersList;
    private JLabel lblSelectedFile;
    private String selectedImagePath = "None";

    private JTable historyTable;
    private DefaultTableModel tableModel;

    public TesterDashboard(User user, MainFrame frame) {
        this.currentUser = user;
        this.mainFrame = frame;
        this.fileManager = new FileManager();

        setLayout(new BorderLayout());

        // --- Header (Updated with Notification Button) ---
        JPanel header = new JPanel(new BorderLayout());
        header.add(new JLabel("  Tester Panel: " + user.getUsername()), BorderLayout.WEST);

        JPanel rightHeader = new JPanel();

        JButton btnNotif = new JButton("🔔");
        btnNotif.addActionListener(e -> new NotificationDialog(mainFrame, currentUser).setVisible(true));

        JButton btnLogout = new JButton("Logout");
        btnLogout.addActionListener(e -> mainFrame.logout());

        rightHeader.add(btnNotif);
        rightHeader.add(btnLogout);
        header.add(rightHeader, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);

        // --- Tabs ---
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Report New Bug", createReportPanel());
        tabs.addTab("My Reported Bugs", createHistoryPanel());

        tabs.addChangeListener(e -> loadHistoryData());

        add(tabs, BorderLayout.CENTER);
    }

    private JPanel createReportPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        txtTitle = new JTextField(20);
        txtDesc = new JTextArea(5, 20);
        txtDesc.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        String[] priorities = {"Low", "Medium", "High", "Critical"};
        comboPriority = new JComboBox<>(priorities);

        comboDevs = new JComboBox<>();
        developersList = fileManager.getUsersByRole("DEV");
        if (developersList.isEmpty()) {
            comboDevs.addItem("No Developers Found");
        } else {
            for(User dev : developersList) {
                comboDevs.addItem(dev.getUsername() + " (ID: " + dev.getId() + ")");
            }
        }

        JButton btnAttach = new JButton("Attach Screenshot");
        lblSelectedFile = new JLabel("No file selected");
        btnAttach.addActionListener(e -> chooseFile());

        JButton btnSubmit = new JButton("Submit Bug");
        btnSubmit.addActionListener(e -> submitBug());

        gbc.gridx = 0; gbc.gridy = 0; panel.add(new JLabel("Bug Title:"), gbc);
        gbc.gridx = 1; panel.add(txtTitle, gbc);

        gbc.gridx = 0; gbc.gridy = 1; panel.add(new JLabel("Description:"), gbc);
        gbc.gridx = 1; panel.add(new JScrollPane(txtDesc), gbc);

        gbc.gridx = 0; gbc.gridy = 2; panel.add(new JLabel("Priority:"), gbc);
        gbc.gridx = 1; panel.add(comboPriority, gbc);

        gbc.gridx = 0; gbc.gridy = 3; panel.add(new JLabel("Assign to Dev:"), gbc);
        gbc.gridx = 1; panel.add(comboDevs, gbc);

        gbc.gridx = 0; gbc.gridy = 4; panel.add(btnAttach, gbc);
        gbc.gridx = 1; panel.add(lblSelectedFile, gbc);

        gbc.gridx = 1; gbc.gridy = 5; panel.add(btnSubmit, gbc);

        return panel;
    }

    private JPanel createHistoryPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        String[] cols = {"Bug ID", "Title", "Status", "Priority", "Assigned To"};
        tableModel = new DefaultTableModel(cols, 0);
        historyTable = new JTable(tableModel);
        panel.add(new JScrollPane(historyTable), BorderLayout.CENTER);
        return panel;
    }

    private void chooseFile() {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            selectedImagePath = selectedFile.getAbsolutePath();
            lblSelectedFile.setText(selectedFile.getName());
        }
    }

    private void submitBug() {
        if(txtTitle.getText().isEmpty() || developersList.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Title and Developer are required!");
            return;
        }

        String bugId = "BUG-" + System.currentTimeMillis() % 10000;
        int devIndex = comboDevs.getSelectedIndex();
        if (devIndex < 0 || devIndex >= developersList.size()) return;

        String assignedDevId = developersList.get(devIndex).getId();

        Bug newBug = new Bug(
                bugId,
                txtTitle.getText(),
                txtDesc.getText(),
                (String) comboPriority.getSelectedItem(),
                "Open",
                currentUser.getId(),
                assignedDevId,
                selectedImagePath
        );

        fileManager.saveBug(newBug);

        fileManager.saveNotification(
                assignedDevId,
                currentUser.getId(),
                "New Bug Assigned: " + txtTitle.getText() + " (" + bugId + ")"
        );

        JOptionPane.showMessageDialog(this, "Bug Reported Successfully!");

        txtTitle.setText("");
        txtDesc.setText("");
        lblSelectedFile.setText("No file selected");
        selectedImagePath = "None";
    }

    private void loadHistoryData() {
        tableModel.setRowCount(0);
        ArrayList<Bug> bugs = fileManager.loadBugs();
        for(Bug b : bugs) {
            if(b.getReporterId().equals(currentUser.getId())) {
                // UPDATE: Using getUsernameById to show name instead of ID
                String devName = fileManager.getUsernameById(b.getAssigneeId());
                tableModel.addRow(new Object[]{
                        b.getId(), b.getTitle(), b.getStatus(), b.getPriority(), devName
                });
            }
        }
    }
}