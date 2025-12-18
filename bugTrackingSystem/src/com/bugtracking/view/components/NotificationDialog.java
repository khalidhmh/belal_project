package com.bugtracking.view.components;

import com.bugtracking.controller.FileManager;
import com.bugtracking.model.Notification;
import com.bugtracking.model.User;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public class NotificationDialog extends JDialog {

    public NotificationDialog(Frame owner, User user) {
        super(owner, "Inbox - " + user.getUsername(), true);
        setSize(500, 400); // كبرنا الحجم شوية
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());

        FileManager fm = new FileManager();
        ArrayList<Notification> notifs = fm.getNotificationsForUser(user.getId());

        DefaultListModel<String> listModel = new DefaultListModel<>();
        JList<String> list = new JList<>(listModel);

        if (notifs.isEmpty()) {
            listModel.addElement("No new notifications.");
        } else {
            for (int i = notifs.size() - 1; i >= 0; i--) {
                Notification n = notifs.get(i);
                // هنا التغيير: بنجيب الاسم بدل الرقم
                String senderName = fm.getUsernameById(n.getSenderId());
                listModel.addElement("From: " + senderName + " >> " + n.getMessage());
            }
        }

        add(new JLabel("  🔔 Your Notifications"), BorderLayout.NORTH);
        add(new JScrollPane(list), BorderLayout.CENTER);

        JButton btnClose = new JButton("Close");
        btnClose.addActionListener(e -> dispose());
        add(btnClose, BorderLayout.SOUTH);
    }
}