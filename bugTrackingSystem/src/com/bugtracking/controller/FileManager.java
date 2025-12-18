package com.bugtracking.controller;

import com.bugtracking.model.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class FileManager {
    private static final String USERS_FILE = "users.txt";
    private static final String BUGS_FILE = "bugs.txt";

    // Initialize files if they don't exist
    public FileManager() {
        createFileIfNotExists(USERS_FILE);
        createFileIfNotExists(BUGS_FILE);
    }

    private void createFileIfNotExists(String fileName) {
        try {
            File file = new File(fileName);
            if (file.createNewFile()) {
                System.out.println("Created file: " + fileName);
                // Create a default admin if users file is new
                if (fileName.equals(USERS_FILE)) {
                    saveUser(new Admin("1", "admin", "admin123", "admin@sys.com"));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // READ USERS
    public ArrayList<User> loadUsers() {
        ArrayList<User> users = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(USERS_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(";");
                if (parts.length >= 5) {
                    String role = parts[4];
                    User u = null;
                    // Factory Pattern logic
                    switch (role) {
                        case "ADMIN": u = new Admin(parts[0], parts[1], parts[2], parts[3]); break;
                        case "TESTER": u = new Tester(parts[0], parts[1], parts[2], parts[3]); break; // Create Tester class
                        case "DEV": u = new Developer(parts[0], parts[1], parts[2], parts[3]); break; // Create Developer class
                        case "PM": u = new ProjectManager(parts[0], parts[1], parts[2], parts[3]); break; // Create PM class
                    }
                    if (u != null) users.add(u);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading users: " + e.getMessage());
        }
        return users;
    }

    // WRITE USER (APPEND)
    public void saveUser(User user) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(USERS_FILE, true))) {
            bw.write(user.toString());
            bw.newLine();
        } catch (IOException e) {
            System.err.println("Error saving user: " + e.getMessage());
        }
    }

    // ... inside FileManager class ...

    // NOTIFICATIONS SYSTEM
    // Saves a message: RecipientID;SenderID;Message;IsRead(true/false)
    public void saveNotification(String recipientId, String senderId, String message) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter("notifications.txt", true))) {
            bw.write(recipientId + ";" + senderId + ";" + message + ";false");
            bw.newLine();
            System.out.println("Notification sent to " + recipientId);
        } catch (IOException e) {
            System.err.println("Error saving notification: " + e.getMessage());
        }
    }

    // UPDATE/DELETE LOGIC
    // Since it's a text file, we must read all, remove/edit, and write all back.
    public void updateUserList(ArrayList<User> users) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(USERS_FILE))) {
            for (User u : users) {
                bw.write(u.toString());
                bw.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    // ... inside FileManager ...

    // 1. READ BUGS
    public ArrayList<Bug> loadBugs() {
        ArrayList<Bug> bugs = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader("bugs.txt"))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(";");
                if (parts.length >= 8) {
                    bugs.add(new Bug(parts[0], parts[1], parts[2], parts[3], parts[4], parts[5], parts[6], parts[7]));
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading bugs: " + e.getMessage());
        }
        return bugs;
    }

    // 2. SAVE BUG (APPEND)
    public void saveBug(Bug bug) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter("bugs.txt", true))) {
            bw.write(bug.toString());
            bw.newLine();
        } catch (IOException e) {
            System.err.println("Error saving bug: " + e.getMessage());
        }
    }

    // 3. GET USERS BY ROLE (Helper for Dropdowns)
    public ArrayList<User> getUsersByRole(String role) {
        ArrayList<User> allUsers = loadUsers();
        ArrayList<User> filtered = new ArrayList<>();
        for(User u : allUsers) {
            if(u.getRole().equalsIgnoreCase(role)) {
                filtered.add(u);
            }
        }
        return filtered;
    }

    // 4. UPDATE BUG STATUS (For later use in Dev Module)
    public void updateBugList(ArrayList<Bug> bugs) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter("bugs.txt"))) {
            for (Bug b : bugs) {
                bw.write(b.toString());
                bw.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }





    // ... inside FileManager ...

    // READ NOTIFICATIONS (Filter by Recipient)
    public ArrayList<Notification> getNotificationsForUser(String userId) {
        ArrayList<Notification> list = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader("notifications.txt"))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(";");
                // Format: Recipient;Sender;Message;IsRead
                if (parts.length >= 4) {
                    if (parts[0].equals(userId)) {
                        boolean isRead = Boolean.parseBoolean(parts[3]);
                        list.add(new Notification(parts[0], parts[1], parts[2], isRead));
                    }
                }
            }
        } catch (IOException e) {
            // File might not exist yet, which is fine
        }
        return list;
    }




    // ... (داخل الكلاس)

    // 1. دالة تجيب الاسم عن طريق الـ ID (عشان الإشعارات والـ PM)
    public String getUsernameById(String id) {
        ArrayList<User> users = loadUsers();
        for (User u : users) {
            if (u.getId().equals(id)) {
                return u.getUsername();
            }
        }
        return "Unknown User (" + id + ")";
    }

    // 2. دالة تعديل مستخدم (Admin Update)
    public void updateUser(User updatedUser) {
        ArrayList<User> users = loadUsers();
        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).getId().equals(updatedUser.getId())) {
                users.set(i, updatedUser); // استبدال القديم بالجديد
                break;
            }
        }
        updateUserList(users); // إعادة كتابة الملف
    }

    // 3. دالة مسح كل الـ Bugs (تصفير النظام)
    public void deleteAllBugs() {
        try (PrintWriter writer = new PrintWriter("bugs.txt")) {
            writer.print(""); // مسح المحتوى
            System.out.println("All bugs deleted.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
