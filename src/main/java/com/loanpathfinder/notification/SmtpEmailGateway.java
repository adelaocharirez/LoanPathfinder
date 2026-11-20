package com.loanpathfinder.notification;

import org.springframework.stereotype.Component;

/** Production email gateway (SMTP). */
@Component
public class SmtpEmailGateway implements EmailGateway {
    @Override
    public void send(String to, String subject, String body) {
        throw new UnsupportedOperationException("Not implemented: SmtpEmailGateway.send");
    }
}
