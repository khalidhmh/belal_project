package com.bugtracking.view.dev;

import com.bugtracking.controller.FileManager;
import com.bugtracking.model.Bug;
import com.bugtracking.model.User;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public class BugDetailsDialog extends JDialog {
    private Bug bug;
    private FileManager fileManager;
    private User devUser;

    public BugDetailsDialog(Frame owner, Bug bug, FileManager fileManager, User devUser) {
        super(owner, "Manage Bug: " + bug.getId(), true);
        this.bug = bug;
        this.fileManager = fileManager;
        this.devUser = devUser;

        setSize(400, 400);
        setLocationRelativeTo(owner);
        setLayout(new GridLayout(7, 2, 10, 10));

        // Display Info (Read Only)
        add(new JLabel("  Title:")); add(new JLabel(bug.getTitle()));
        add(new JLabel("  Description:")); add(new JScrollPane(new JTextArea(bug.getDescription())));
        add(new JLabel("  Priority:")); add(new JLabel(bug.getPriority()));

        // Image Path (Requirement: Show path string)
        add(new JLabel("  Screenshot:"));
        JTextField txtPath = new JTextField(bug.getImagePath());
        txtPath.setEditable(false);
        add(txtPath);

        // Status Update (Editable)
        add(new JLabel("  Current Status:"));
        String[] statuses = {"Open", "In Progress", "Fixed", "Closed"};
        JComboBox<String> comboStatus = new JComboBox<>(statuses);
        comboStatus.setSelectedItem(bug.getStatus());
        add(comboStatus);

        JButton btnUpdate = new JButton("Update Status");
        JButton btnCancel = new JButton("Cancel");

        add(btnCancel);
        add(btnUpdate);

        btnCancel.addActionListener(e -> dispose());

        // --- UPDATE LOGIC ---
        btnUpdate.addActionListener(e -> {
            String newStatus = (String) comboStatus.getSelectedItem();

            // 1. Update Object in Memory
            bug.setStatus(newStatus);

            // 2. Read ALL bugs, Swap the old one with new one, Save All
            ArrayList<Bug> allBugs = fileManager.loadBugs();
            for(int i=0; i<allBugs.size(); i++) {
                if(allBugs.get(i).getId().equals(bug.getId())) {
                    allBugs.set(i, bug); // Replace
                    break;
                }
            }
            fileManager.updateBugList(allBugs); // Write to file

            // 3. REQUIREMENT: Send Notification back to Tester
            // Logic: If status changed to Fixed or Closed, notify Reporter
            if (!newStatus.equals("Open")) {
                fileManager.saveNotification(
                        bug.getReporterId(),
                        devUser.getId(),
                        "Update on Bug " + bug.getId() + ": Status changed to " + newStatus
                );
            }

            JOptionPane.showMessageDialog(this, "Status Updated & Tester Notified!");
            dispose();
        });
    }
}