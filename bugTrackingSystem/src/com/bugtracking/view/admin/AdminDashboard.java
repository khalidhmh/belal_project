package com.bugtracking.view.admin;

import com.bugtracking.controller.FileManager;
import com.bugtracking.model.*;
import com.bugtracking.view.MainFrame;
import com.bugtracking.view.components.NotificationDialog; // Import Added

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;

public class AdminDashboard extends JPanel {
    private User adminUser;
    private MainFrame mainFrame;
    private JTable userTable;
    private DefaultTableModel tableModel;
    private FileManager fileManager;

    public AdminDashboard(User adminUser, MainFrame mainFrame) {
        this.adminUser = adminUser;
        this.mainFrame = mainFrame;
        this.fileManager = new FileManager();

        setLayout(new BorderLayout());

        // 1. Header (Updated with Notification Button)
        JPanel header = new JPanel(new BorderLayout());
        header.add(new JLabel("  Welcome Admin: " + adminUser.getUsername()), BorderLayout.WEST);

        // Right Header for Buttons
        JPanel rightHeader = new JPanel();

        JButton btnNotif = new JButton("🔔 Notifications");
        btnNotif.addActionListener(e -> new NotificationDialog(mainFrame, adminUser).setVisible(true));

        JButton btnLogout = new JButton("Logout");
        btnLogout.addActionListener(e -> mainFrame.logout());

        rightHeader.add(btnNotif);
        rightHeader.add(btnLogout);
        header.add(rightHeader, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);

        // 2. Table Setup
        String[] columns = {"ID", "Username", "Email", "Role"};
        tableModel = new DefaultTableModel(columns, 0);
        userTable = new JTable(tableModel);
        loadTableData();
        add(new JScrollPane(userTable), BorderLayout.CENTER);

        // 3. Action Buttons
        JPanel buttonPanel = new JPanel();

        JButton btnAdd = new JButton("Add User");
        JButton btnEdit = new JButton("Edit User");
        JButton btnDelete = new JButton("Remove User");
        JButton btnClearBugs = new JButton("⚠ Reset All Bugs");
        btnClearBugs.setForeground(Color.RED);

        btnAdd.addActionListener(e -> showAddUserDialog());
        btnEdit.addActionListener(e -> showEditUserDialog());
        btnDelete.addActionListener(e -> deleteSelectedUser());

        btnClearBugs.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Are you sure? This will delete ALL bug history permanently!",
                    "Critical Warning", JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                fileManager.deleteAllBugs();
                JOptionPane.showMessageDialog(this, "System Cleaned. All bugs deleted.");
            }
        });

        buttonPanel.add(btnAdd);
        buttonPanel.add(btnEdit);
        buttonPanel.add(btnDelete);
        buttonPanel.add(btnClearBugs);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void loadTableData() {
        tableModel.setRowCount(0);
        ArrayList<User> users = fileManager.loadUsers();
        for (User u : users) {
            tableModel.addRow(new Object[]{u.getId(), u.getUsername(), u.getEmail(), u.getRole()});
        }
    }

    // --- LOGIC: DELETE USER ---
    private void deleteSelectedUser() {
        int row = userTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a user.");
            return;
        }

        String userId = (String) tableModel.getValueAt(row, 0);

        if (userId.equals("1") || userId.equals(adminUser.getId())) {
            JOptionPane.showMessageDialog(this, "Cannot delete Super Admin or yourself!", "Access Denied", JOptionPane.ERROR_MESSAGE);
            return;
        }

        ArrayList<User> users = fileManager.loadUsers();
        users.removeIf(u -> u.getId().equals(userId));
        fileManager.updateUserList(users);
        loadTableData();
        JOptionPane.showMessageDialog(this, "User deleted.");
    }

    // --- LOGIC: EDIT USER ---
    private void showEditUserDialog() {
        int row = userTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select a user to edit.");
            return;
        }

        String id = (String) tableModel.getValueAt(row, 0);
        String currentName = (String) tableModel.getValueAt(row, 1);
        String currentEmail = (String) tableModel.getValueAt(row, 2);
        String currentRole = (String) tableModel.getValueAt(row, 3);

        JDialog dialog = new JDialog(mainFrame, "Edit User: " + currentName, true);
        dialog.setSize(350, 250);
        dialog.setLayout(new GridLayout(5, 2));
        dialog.setLocationRelativeTo(mainFrame);

        JTextField txtName = new JTextField(currentName);
        JTextField txtEmail = new JTextField(currentEmail);
        JButton btnUpdate = new JButton("Update User");

        dialog.add(new JLabel("Username:")); dialog.add(txtName);
        dialog.add(new JLabel("Email:"));    dialog.add(txtEmail);
        dialog.add(new JLabel("Role:"));     dialog.add(new JLabel(currentRole));
        dialog.add(new JLabel("ID:"));       dialog.add(new JLabel(id));
        dialog.add(new JLabel(""));          dialog.add(btnUpdate);

        btnUpdate.addActionListener(e -> {
            if(txtName.getText().isEmpty() || txtEmail.getText().isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Fields cannot be empty.");
                return;
            }

            ArrayList<User> allUsers = fileManager.loadUsers();
            String oldPass = "123";
            for(User u : allUsers) {
                if(u.getId().equals(id)) {
                    oldPass = u.getPassword();
                    break;
                }
            }

            User updatedUser = null;
            switch(currentRole) {
                case "ADMIN": updatedUser = new Admin(id, txtName.getText(), oldPass, txtEmail.getText()); break;
                case "TESTER": updatedUser = new Tester(id, txtName.getText(), oldPass, txtEmail.getText()); break;
                case "DEV": updatedUser = new Developer(id, txtName.getText(), oldPass, txtEmail.getText()); break;
                case "PM": updatedUser = new ProjectManager(id, txtName.getText(), oldPass, txtEmail.getText()); break;
            }

            if (updatedUser != null) {
                fileManager.updateUser(updatedUser);
                loadTableData();
                JOptionPane.showMessageDialog(dialog, "User Updated Successfully.");
                dialog.dispose();
            }
        });

        dialog.setVisible(true);
    }

    // --- LOGIC: ADD USER ---
    private void showAddUserDialog() {
        JDialog dialog = new JDialog(mainFrame, "Add New User", true);
        dialog.setSize(400, 300);
        dialog.setLayout(new GridLayout(6, 2));
        dialog.setLocationRelativeTo(mainFrame);

        JTextField txtUser = new JTextField();
        JPasswordField txtPass = new JPasswordField();
        JTextField txtEmail = new JTextField();
        String[] roles = {"TESTER", "DEV", "PM", "ADMIN"};
        JComboBox<String> comboRole = new JComboBox<>(roles);

        JCheckBox showPass = new JCheckBox("Show Password");
        showPass.addActionListener(e -> {
            if (showPass.isSelected()) txtPass.setEchoChar((char) 0);
            else txtPass.setEchoChar('•');
        });

        JButton btnSave = new JButton("Save User");

        dialog.add(new JLabel("Username:")); dialog.add(txtUser);
        dialog.add(new JLabel("Password:")); dialog.add(txtPass);
        dialog.add(new JLabel("Email:"));    dialog.add(txtEmail);
        dialog.add(new JLabel("Role:"));     dialog.add(comboRole);
        dialog.add(showPass);                dialog.add(new JLabel(""));
        dialog.add(btnSave);

        btnSave.addActionListener(e -> {
            String u = txtUser.getText();
            String p = new String(txtPass.getPassword());
            String em = txtEmail.getText();
            String r = (String) comboRole.getSelectedItem();

            if(u.isEmpty() || p.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Fields cannot be empty.");
                return;
            }

            String newId = String.valueOf(System.currentTimeMillis() % 10000);

            User newUser = null;
            switch(r) {
                case "ADMIN": newUser = new Admin(newId, u, p, em); break;
                case "TESTER": newUser = new Tester(newId, u, p, em); break;
                case "DEV": newUser = new Developer(newId, u, p, em); break;
                case "PM": newUser = new ProjectManager(newId, u, p, em); break;
            }

            fileManager.saveUser(newUser);
            fileManager.saveNotification(newId, adminUser.getId(), "Welcome to the Bug Tracking System!");

            loadTableData();
            dialog.dispose();
        });

        dialog.setVisible(true);
    }
}