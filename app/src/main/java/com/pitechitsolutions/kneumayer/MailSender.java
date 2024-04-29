package com.pitechitsolutions.kneumayer;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

public class MailSender {
    private static final ExecutorService emailExecutor = Executors.newSingleThreadExecutor();

    // Callback interface for result handling
    public interface EmailResultCallback {
        void onResult(String message);
    }

    public static void sendEmailInBackground(final String recipientEmail, final String subject, final String body, final File attachment, final EmailResultCallback callback) {
        emailExecutor.execute(() -> {
            String result = sendEmail(recipientEmail, subject, body, attachment);
            new Handler(Looper.getMainLooper()).post(() -> callback.onResult(result));
        });
    }

    private static String sendEmail(String recipientEmail, String subject, String body, File attachment) {
        final String username = "kneureportservices@outlook.com"; // Your Outlook email
        final String password = "drbwjcakyqksjtvr"; // Your app password

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp-mail.outlook.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props, new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress("kneureportservices@outlook.com"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
            message.setSubject(subject);
            message.setText(body);

            if (attachment != null) {
                MimeBodyPart mimeBodyPart = new MimeBodyPart();
                mimeBodyPart.attachFile(attachment);
                Multipart multipart = new MimeMultipart();
                multipart.addBodyPart(mimeBodyPart);
                message.setContent(multipart);
            }

            Transport.send(message);
            Log.d("MailSender", "Email sent successfully to: " + recipientEmail);
            return "Email sent successfully to: " + recipientEmail; // Success message
        } catch (MessagingException | IOException e) {
            Log.e("MailSender", "Failed to send email: " + e.getMessage());
            e.printStackTrace();
            return "Failed to send email: " + e.getMessage(); // Error message
        }
    }

    public static void shutDownExecutor() {
        emailExecutor.shutdown();
    }
}
