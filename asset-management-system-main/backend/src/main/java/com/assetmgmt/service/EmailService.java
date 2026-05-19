package com.assetmgmt.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final String LOG_FILE = "sent_emails.log";

    public void sendWelcomeEmail(String toEmail, String loginUrl, String username, String tempPassword) {
        String content = String.format(
            "============================================================\n" +
            "Date/Time   : %s\n" +
            "Type        : WELCOME EMAIL\n" +
            "Recipient   : %s\n" +
            "Subject     : Welcome to AssetIQ - Your New Account is Ready\n" +
            "------------------------------------------------------------\n" +
            "Hello,\n\n" +
            "Your new account on AssetIQ has been successfully created by the Administrator.\n" +
            "Please use the credentials below to log in for the first time:\n\n" +
            "Login URL: %s\n" +
            "Username : %s\n" +
            "Password : %s\n\n" +
            "Note: You will be prompted to update your password upon your first login.\n" +
            "============================================================\n\n",
            LocalDateTime.now(), toEmail, loginUrl, username, tempPassword
        );

        log.info("Simulated Email Sent to: {}\n{}", toEmail, content);
        writeToLogFile(content);
    }

    public void sendPasswordResetEmail(String toEmail, String loginUrl, String username, String newPassword) {
        String content = String.format(
            "============================================================\n" +
            "Date/Time   : %s\n" +
            "Type        : PASSWORD RESET EMAIL\n" +
            "Recipient   : %s\n" +
            "Subject     : AssetIQ - Password Reset Successful\n" +
            "------------------------------------------------------------\n" +
            "Hello,\n\n" +
            "Your password on AssetIQ has been reset by the Administrator.\n" +
            "Please use the credentials below to log in:\n\n" +
            "Login URL: %s\n" +
            "Username : %s\n" +
            "Password : %s\n\n" +
            "Note: You will be prompted to update your password upon your next login.\n" +
            "============================================================\n\n",
            LocalDateTime.now(), toEmail, loginUrl, username, newPassword
        );

        log.info("Simulated Email Sent to: {}\n{}", toEmail, content);
        writeToLogFile(content);
    }

    private synchronized void writeToLogFile(String content) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(LOG_FILE, true))) {
            writer.write(content);
        } catch (IOException e) {
            log.error("Failed to write simulated email to log file: {}", e.getMessage());
        }
    }
}
