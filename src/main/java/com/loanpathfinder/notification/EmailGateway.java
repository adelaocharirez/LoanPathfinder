package com.loanpathfinder.notification;

/** Injected boundary to the SMTP server, so tests can record emails instead of sending them. */
public interface EmailGateway {
    void send(String to, String subject, String body);
}
