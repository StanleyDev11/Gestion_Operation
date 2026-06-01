package com.ace3i.connect.service.impl;

import com.ace3i.connect.entity.AppConfig;
import com.ace3i.connect.repository.AppConfigRepository;
import com.ace3i.connect.service.IEmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

import java.util.Properties;

@Service
public class EmailService implements IEmailService {

    @Autowired
    private AppConfigRepository configRepository;

    @Override
    public void sendSimpleMessage(String to, String subject, String text) {
        AppConfig config = configRepository.findById(1L).orElse(null);
        if (config == null || config.getSmtpHost() == null || config.getNotificationEmail() == null) {
            System.err.println("Configuration Email non complète. Impossible d'envoyer l'email.");
            return;
        }

        try {
            JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
            mailSender.setHost(config.getSmtpHost());
            mailSender.setPort(config.getSmtpPort());
            mailSender.setUsername(config.getSmtpUsername());
            mailSender.setPassword(config.getSmtpPassword());

            Properties props = mailSender.getJavaMailProperties();
            props.put("mail.transport.protocol", "smtp");
            props.put("mail.smtp.auth", String.valueOf(config.getSmtpAuth() != null ? config.getSmtpAuth() : true));
            props.put("mail.smtp.starttls.enable", String.valueOf(config.getSmtpStartTls() != null ? config.getSmtpStartTls() : true));
            props.put("mail.debug", "false");

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(config.getSmtpUsername()); // Souvent le même que l'user SMTP
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);

            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Erreur lors de l'envoi de l'email : " + e.getMessage());
        }
    }
}
