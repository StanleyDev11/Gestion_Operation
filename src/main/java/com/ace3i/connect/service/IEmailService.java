package com.ace3i.connect.service;

public interface IEmailService {
    void sendSimpleMessage(String to, String subject, String text);
}
