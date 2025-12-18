package com.bugtracking.view.dev;

import com.bugtracking.controller.FileManager;
import com.bugtracking.model.Bug;
import com.bugtracking.model.User;
import com.bugtracking.view.MainFrame;
import com.bugtracking.view.components.NotificationDialog; // Import Added

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class DevDashboard extends JPanel {
    private User devUser;
    private MainFrame mainFrame;
    private FileManager fileManager;
    private JTable bugTable;
    private DefaultTableModel tableModel;
    private ArrayList<Bug> myBugs;

    public DevDashboard(User devUser, MainFrame mainFrame) {
        this.devUser = devUser;
        this.mainFrame = mainFrame;
        this.fileManager = new FileManager();

        setLayout(new BorderLayout());

        // 1. Header (Updated with Notification Button)
        JPanel header = new JPanel(new BorderLayout());
        header.add(new JLabel("  Developer Dashboard: " + devUser.getUsername()), BorderLayout.WEST);

        JPanel rightHeader = new JPanel();

        JButton btnNotif = new JButton("🔔");
        btnNotif.addActionListener(e -> new NotificationDialog(mainFrame, devUser).setVisible(true));

        JButton btnLogout = new JButton("Logout");
        btnLogout.addActionListener(e -> mainFrame.logout());

        rightHeader.add(btnNotif);
        rightHeader.add(btnLogout);
        header.add(rightHeader, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);

        // 2. Table Setup
        String[] cols = {"ID", "Title", "Priority", "Status", "Reporter"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        bugTable = new JTable(tableModel);

        bugTable.getColumnModel().getColumn(2).setCellRenderer(new PriorityRenderer());

        add(new JScrollPane(bugTable), BorderLayout.CENTER);

        // 3. Click Listener
        bugTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = bugTable.getSelectedRow();
                    if (row != -1) {
                        String bugId = (String) tableModel.getValueAt(row, 0);
                        openBugDetails(bugId);
                    }
                }
            }
        });

        loadAssignedBugs();
    }

    private void loadAssignedBugs() {
        tableModel.setRowCount(0);
        ArrayList<Bug> allBugs = fileManager.loadBugs();
        myBugs = new ArrayList<>();

        for (Bug b : allBugs) {
            if (b.getAssigneeId().equals(devUser.getId())) {
                myBugs.add(b);
            }
        }

        Collections.sort(myBugs, new Comparator<Bug>() {
            @Override
            public int compare(Bug b1, Bug b2) {
                return getPriorityWeight(b2.getPriority()) - getPriorityWeight(b1.getPriority());
            }
        });

        for (Bug b : myBugs) {
            // UPDATE: Using getUsernameById to show Reporter Name
            String reporterName = fileManager.getUsernameById(b.getReporterId());
            tableModel.addRow(new Object[]{b.getId(), b.getTitle(), b.getPriority(), b.getStatus(), reporterName});
        }
    }

    private int getPriorityWeight(String p) {
        if (p.equalsIgnoreCase("Critical")) return 4;
        if (p.equalsIgnoreCase("High")) return 3;
        if (p.equalsIgnoreCase("Medium")) return 2;
        return 1;
    }

    private void openBugDetails(String bugId) {
        Bug selectedBug = null;
        for(Bug b : myBugs) {
            if(b.getId().equals(bugId)) selectedBug = b;
        }

        if(selectedBug != null) {
            new BugDetailsDialog(mainFrame, selectedBug, fileManager, devUser).setVisible(true);
            loadAssignedBugs();
        }
    }

    class PriorityRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            String priority = (String) value;

            if ("High".equalsIgnoreCase(priority) || "Critical".equalsIgnoreCase(priority)) {
                c.setForeground(Color.RED);
                c.setFont(c.getFont().deriveFont(Font.BOLD));
            } else {
                c.setForeground(Color.BLACK);
            }
            return c;
        }
    }
}