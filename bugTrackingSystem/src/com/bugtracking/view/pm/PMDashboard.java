package com.bugtracking.view.pm;

import com.bugtracking.controller.FileManager;
import com.bugtracking.model.Bug;
import com.bugtracking.model.User;
import com.bugtracking.view.MainFrame;
import com.bugtracking.view.components.NotificationDialog;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;

public class PMDashboard extends JPanel {
    private User pmUser;
    private MainFrame mainFrame;
    private FileManager fileManager;
    private JTable statsTable;
    private DefaultTableModel tableModel;
    private JTextField txtSearch;

    public PMDashboard(User pmUser, MainFrame mainFrame) {
        this.pmUser = pmUser;
        this.mainFrame = mainFrame;
        this.fileManager = new FileManager();

        setLayout(new BorderLayout());

        // 1. Header Section
        JPanel header = new JPanel(new BorderLayout());
        header.add(new JLabel("  Project Manager: " + pmUser.getUsername()), BorderLayout.WEST);

        JPanel rightHeader = new JPanel();

        // زر الإشعارات (Notification Button)
        JButton btnNotif = new JButton("🔔 Notifications");
        btnNotif.addActionListener(e -> new NotificationDialog(mainFrame, pmUser).setVisible(true));

        // زر الإحصائيات التفصيلية (Performance Report Button)
        JButton btnStats = new JButton("📊 User Stats");
        btnStats.addActionListener(e -> showDetailedStats());

        // زر تسجيل الخروج
        JButton btnLogout = new JButton("Logout");
        btnLogout.addActionListener(e -> mainFrame.logout());

        rightHeader.add(btnNotif);
        rightHeader.add(btnStats); // Added here
        rightHeader.add(btnLogout);
        header.add(rightHeader, BorderLayout.EAST);

        // 2. Statistics Panel (Top Cards)
        JPanel statsPanel = createStatsPanel();

        // تجميع الهيدر مع لوحة الإحصائيات في الجزء العلوي
        JPanel topContainer = new JPanel(new GridLayout(2, 1));
        topContainer.add(header);
        topContainer.add(statsPanel);
        add(topContainer, BorderLayout.NORTH);

        // 3. Search & Main Table Section
        JPanel centerPanel = new JPanel(new BorderLayout());

        // Search Bar
        JPanel searchPanel = new JPanel();
        searchPanel.add(new JLabel("Search Bug (ID/Title):"));
        txtSearch = new JTextField(20);
        JButton btnSearch = new JButton("Search");
        JButton btnReset = new JButton("Show All");

        searchPanel.add(txtSearch);
        searchPanel.add(btnSearch);
        searchPanel.add(btnReset);

        centerPanel.add(searchPanel, BorderLayout.NORTH);

        // Table Setup
        // التعديل هنا: عرض أسماء الأشخاص بدلاً من الأرقام
        String[] cols = {"Bug ID", "Title", "Status", "Reporter (Tester)", "Assigned To (Dev)"};
        tableModel = new DefaultTableModel(cols, 0);
        statsTable = new JTable(tableModel);
        loadBugData(""); // Load all initially

        centerPanel.add(new JScrollPane(statsTable), BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);

        // Events for Search
        btnSearch.addActionListener(e -> loadBugData(txtSearch.getText().toLowerCase()));
        btnReset.addActionListener(e -> { txtSearch.setText(""); loadBugData(""); });
    }

    // --- Helper Method: Create Stats Cards ---
    private JPanel createStatsPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 4, 10, 10));
        panel.setBorder(BorderFactory.createTitledBorder("Project Overview"));

        ArrayList<Bug> bugs = fileManager.loadBugs();
        int total = bugs.size();
        int open = 0, fixed = 0, closed = 0;

        for (Bug b : bugs) {
            if (b.getStatus().equalsIgnoreCase("Open")) open++;
            else if (b.getStatus().equalsIgnoreCase("Fixed")) fixed++;
            else if (b.getStatus().equalsIgnoreCase("Closed")) closed++;
        }

        panel.add(createStatLabel("Total Bugs", String.valueOf(total), Color.BLACK));
        panel.add(createStatLabel("Open", String.valueOf(open), Color.RED));
        panel.add(createStatLabel("Fixed", String.valueOf(fixed), new Color(0, 150, 0))); // Dark Green
        panel.add(createStatLabel("Closed", String.valueOf(closed), Color.GRAY));

        return panel;
    }

    private JLabel createStatLabel(String title, String value, Color color) {
        JLabel lbl = new JLabel("<html><center>" + title + "<br><font size='5'>" + value + "</font></center></html>", SwingConstants.CENTER);
        lbl.setForeground(color);
        return lbl;
    }

    // --- Helper Method: Load Data into Table ---
    private void loadBugData(String query) {
        tableModel.setRowCount(0);
        ArrayList<Bug> bugs = fileManager.loadBugs();

        for (Bug b : bugs) {
            boolean match = query.isEmpty()
                    || b.getId().toLowerCase().contains(query)
                    || b.getTitle().toLowerCase().contains(query);

            if (match) {
                // التعديل هنا: استخدام الدالة الجديدة لجلب الأسماء
                String testerName = fileManager.getUsernameById(b.getReporterId());
                String devName = fileManager.getUsernameById(b.getAssigneeId());

                tableModel.addRow(new Object[]{
                        b.getId(),
                        b.getTitle(),
                        b.getStatus(),
                        testerName, // عرض الاسم
                        devName     // عرض الاسم
                });
            }
        }
    }

    // --- Helper Method: Show Detailed Stats Popup ---
    private void showDetailedStats() {
        JDialog dialog = new JDialog(mainFrame, "Detailed Performance Report", true);
        dialog.setSize(500, 400);
        dialog.setLocationRelativeTo(mainFrame);

        JTextArea statsArea = new JTextArea();
        statsArea.setEditable(false);
        statsArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        statsArea.setMargin(new Insets(10, 10, 10, 10));

        ArrayList<Bug> bugs = fileManager.loadBugs();
        ArrayList<User> users = fileManager.loadUsers();

        StringBuilder report = new StringBuilder();
        report.append("=== PERFORMANCE REPORT ===\n\n");

        for(User u : users) {
            if(u.getRole().equals("DEV")) {
                long fixedCount = bugs.stream()
                        .filter(b -> b.getAssigneeId().equals(u.getId()) && b.getStatus().equals("Fixed"))
                        .count();
                long totalAssigned = bugs.stream()
                        .filter(b -> b.getAssigneeId().equals(u.getId()))
                        .count();

                report.append("[DEV] ").append(u.getUsername())
                        .append("\n   - Assigned: ").append(totalAssigned)
                        .append("\n   - Fixed:    ").append(fixedCount)
                        .append("\n--------------------------\n");
            }
            else if (u.getRole().equals("TESTER")) {
                long reportedCount = bugs.stream()
                        .filter(b -> b.getReporterId().equals(u.getId()))
                        .count();

                report.append("[TESTER] ").append(u.getUsername())
                        .append("\n   - Reported: ").append(reportedCount)
                        .append("\n--------------------------\n");
            }
        }

        statsArea.setText(report.toString());
        dialog.add(new JScrollPane(statsArea));
        dialog.setVisible(true);
    }
}