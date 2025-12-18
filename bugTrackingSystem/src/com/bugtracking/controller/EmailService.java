package com.bugtracking.controller;

import com.bugtracking.model.Notification;
import java.io.*;
import java.util.ArrayList;

public class EmailService {
    private static final String FILE_NAME = "notifications.txt";

    public EmailService() {
        // Ensure file exists
        try {
            File f = new File(FILE_NAME);
            if(f.createNewFile()) System.out.println("Notification server started (File created).");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Send "Email" (Append to file)
    public void sendEmail(String recipientId, String senderId, String subject, String body) {
        // Format: RecipientID;SenderID;Message;IsRead
        String message = subject + " - " + body;
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(FILE_NAME, true))) {
            bw.write(recipientId + ";" + senderId + ";" + message + ";false");
            bw.newLine();
            System.out.println(">> Email sent to User ID: " + recipientId);
        } catch (IOException e) {
            System.err.println("Failed to send email: " + e.getMessage());
        }
    }

    // Read Inbox
    public ArrayList<Notification> getInbox(String userId) {
        ArrayList<Notification> inbox = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(FILE_NAME))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(";");
                if (parts.length >= 4 && parts[0].equals(userId)) {
                    inbox.add(new Notification(parts[0], parts[1], parts[2], Boolean.parseBoolean(parts[3])));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return inbox;
    }
}