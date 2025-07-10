package com.reliefnet.util;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;
import java.util.Random;

/**
 * EmailService - Handles email verification for ReliefNet
 * Provides two-factor authentication via email verification codes
 */
public class EmailService {
    // Gmail SMTP settings
    private static final String HOST = "smtp.gmail.com";
    private static final String PORT = "587";
    private static final String FROM_EMAIL = "sumaiyaaanika09@gmail.com";
    private static final String APP_PASSWORD = "ikxfriednjcgapud";

    /**
     * Sends a verification code to the specified email address
     * @param toEmail The recipient's email address
     * @param purpose The purpose of the verification (registration/password reset)
     * @return The verification code sent, or null if sending failed
     */
    public static String sendVerificationCode(String toEmail, String purpose) {
        System.out.println("🔄 Starting email verification process for: " + toEmail);
        String verificationCode = generateVerificationCode();

        // Configure SMTP properties
        Properties properties = new Properties();
        properties.put("mail.smtp.host", HOST);
        properties.put("mail.smtp.port", PORT);
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.debug", "true"); // Enable debug logging
        
        System.out.println("📧 SMTP Configuration:");
        System.out.println("   Host: " + HOST);
        System.out.println("   Port: " + PORT);
        System.out.println("   From: " + FROM_EMAIL);

        // Create authenticated session
        Session session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(FROM_EMAIL, APP_PASSWORD);
            }
        });

        try {
            // Create and configure email message
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(FROM_EMAIL));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            
            // Set subject based on purpose
            if ("registration".equals(purpose)) {
                message.setSubject("ReliefNet - Email Verification Code");
            } else {
                message.setSubject("ReliefNet - Password Reset Code");
            }

            // Create HTML content for better formatting
            String htmlContent = createEmailContent(verificationCode, purpose);
            message.setContent(htmlContent, "text/html; charset=utf-8");

            // Send the email
            System.out.println("🔄 Attempting to send email to: " + toEmail);
            Transport.send(message);
            System.out.println("✅ Verification code sent successfully to: " + toEmail);
            return verificationCode;
        } catch (MessagingException e) {
            System.err.println("❌ Failed to send verification email to: " + toEmail);
            System.err.println("Error details: " + e.getMessage());
            System.err.println("Error type: " + e.getClass().getSimpleName());
            e.printStackTrace();
            return null;
        } catch (Exception e) {
            System.err.println("❌ Unexpected error sending email: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Generates a 6-digit verification code
     * @return A random 6-digit code as a string
     */
    private static String generateVerificationCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }

    /**
     * Creates the HTML content for the verification email
     * @param verificationCode The verification code to include
     * @param purpose The purpose of the verification
     * @return HTML formatted email content
     */
    private static String createEmailContent(String verificationCode, String purpose) {
        String title = "registration".equals(purpose) ? "Email Verification" : "Password Reset";
        String description = "registration".equals(purpose) ? 
            "Please enter this code to complete your registration." :
            "Please enter this code to reset your password.";
        
        return String.format(
            "<html>" +
            "<head>" +
            "<style>" +
            "body { font-family: Arial, sans-serif; background-color: #f4f4f4; margin: 0; padding: 20px; }" +
            ".container { max-width: 600px; margin: 0 auto; background-color: #ffffff; padding: 30px; border-radius: 10px; box-shadow: 0 4px 6px rgba(0,0,0,0.1); }" +
            ".header { text-align: center; margin-bottom: 30px; }" +
            ".logo { font-size: 24px; font-weight: bold; color: #1e3c72; margin-bottom: 10px; }" +
            ".code-box { background-color: #f8f9fa; border: 2px solid #e9ecef; border-radius: 8px; padding: 20px; text-align: center; margin: 20px 0; }" +
            ".code { font-size: 32px; font-weight: bold; color: #2c3e50; letter-spacing: 5px; }" +
            ".footer { text-align: center; margin-top: 30px; color: #6c757d; font-size: 12px; }" +
            "</style>" +
            "</head>" +
            "<body>" +
            "<div class='container'>" +
            "<div class='header'>" +
            "<div class='logo'>ReliefNet</div>" +
            "<h2>%s</h2>" +
            "</div>" +
            "<p>Your verification code is:</p>" +
            "<div class='code-box'>" +
            "<div class='code'>%s</div>" +
            "</div>" +
            "<p>%s</p>" +
            "<p>This code will expire in 10 minutes.</p>" +
            "<p>If you didn't request this code, please ignore this email.</p>" +
            "<div class='footer'>" +
            "<p>© 2024 ReliefNet - Disaster Relief System</p>" +
            "</div>" +
            "</div>" +
            "</body>" +
            "</html>",
            title, verificationCode, description
        );
    }

    /**
     * Validates if a verification code is still valid (within 10 minutes)
     * @param codeTimestamp The timestamp when the code was generated
     * @return true if the code is still valid, false otherwise
     */
    public static boolean isCodeValid(long codeTimestamp) {
        long currentTime = System.currentTimeMillis();
        long timeDifference = currentTime - codeTimestamp;
        // Code is valid for 10 minutes (600,000 milliseconds)
        return timeDifference <= 600000;
    }
}
