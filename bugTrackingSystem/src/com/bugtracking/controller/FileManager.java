package com.bugtracking.controller;

import com.bugtracking.model.*;
import java.io.*;
import java.util.*;

public class FileManager {
    //constants for files names handles users , bugs and notifications data
    private static final String USERS_FILE = "users.txt";
    private static final String BUGS_FILE = "bugs.txt";
    private static final String NOTIFICATIONS_FILE = "notifications.txt";


    //constructor to create data .txt files
    public FileManager() {
        createFileIfNotExists(USERS_FILE);
        createFileIfNotExists(BUGS_FILE);
        createFileIfNotExists(NOTIFICATIONS_FILE);
    }

    //function createFileIfNotExists
    private void createFileIfNotExists(String fileName) {
        try {
            File file = new File(fileName);
            if (file.createNewFile()) {
                System.out.println("Created file: " + fileName);
                // Create user with role admin after users file created
                if (fileName.equals(USERS_FILE)) {
                    saveUser(new Admin("1", "admin", "admin123", "admin@sys.com"));
                    //saveUser function implementation in line 43
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //function loadUsers to obtain users from a .txt file as objects
    public ArrayList<User> loadUsers(){
        ArrayList<User> users = new ArrayList<>();
        try {
            Scanner scanner = new Scanner(new File(USERS_FILE));
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] parts = line.split(";");
                String role = parts[4];
                User user = null;
                // Factory Pattern logic
                switch (role) {
                    case "ADMIN":
                        user = new Admin(parts[0], parts[1], parts[2], parts[3]);
                        break;
                    case "TESTER":
                        user = new Tester(parts[0], parts[1], parts[2], parts[3]);
                        break;
                    case "DEV":
                        user = new Developer(parts[0], parts[1], parts[2], parts[3]);
                        break;
                    case "PM":
                        user = new ProjectManager(parts[0], parts[1], parts[2], parts[3]);
                        break;
                }
                if (user != null){
                    users.add(user);
                }
            }
            scanner.close();
        }
        catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        return users;
    }

    // function saveUser to save users in users.txt file
    public void saveUser(User user){
        try{
            PrintWriter printWriter = new PrintWriter(new FileWriter(USERS_FILE, true));
            printWriter.println(user.toString());
            printWriter.close();
        }
        catch (IOException e){
            System.out.println("Error saving user: " + e.getMessage());
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
    public void updateUserList(ArrayList<User> users){
        try{
            PrintWriter printWriter = new PrintWriter(new FileWriter(USERS_FILE));
            for (int i = 0; i < users.size(); i++) {
                printWriter.println(users.get(i).toString());
            }
            printWriter.close();
        }
        catch (IOException e){
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

    //getUserByRole function
    public ArrayList<User> getUsersByRole(String role) {
        ArrayList<User> users = loadUsers();
        ArrayList<User> filtered = new ArrayList<>();

        for (int i = 0; i < users.size(); i++) {
            User user = users.get(i);
            if (user.getRole().equals(role)) {
                filtered.add(user);
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



    // find user by id

    public String getUsernameById(String id) {
        ArrayList<User> users = loadUsers();
        for (User u : users) {
            if (u.getId().equals(id)) {
                return u.getUsername();
            }
        }
        return "Unknown User (" + id + ")";
    }

    //admin update function
    public void updateUser(User updatedUser){
        ArrayList<User> users = loadUsers();
        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).getId().equals(updatedUser.getId())) {
                users.set(i, updatedUser);
                break;
            }
        }
        updateUserList(users); //updateUserList function implementation in line 102
    }


    // clear bugs list
    public void deleteAllBugs() {
        try (PrintWriter writer = new PrintWriter("bugs.txt")) {
            writer.print(""); // مسح المحتوى
            System.out.println("All bugs deleted.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
