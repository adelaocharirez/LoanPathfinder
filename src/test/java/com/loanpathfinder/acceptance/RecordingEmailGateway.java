package com.loanpathfinder.acceptance;

import com.loanpathfinder.notification.EmailGateway;
import java.util.ArrayList;
import java.util.List;

/** Test double for the injected EmailGateway. Records emails instead of sending them. */
public class RecordingEmailGateway implements EmailGateway {

    public record SentEmail(String to, String subject, String body) {
    }

    private final List<SentEmail> sent = new ArrayList<>();

    @Override
    public void send(String to, String subject, String body) {
        sent.add(new SentEmail(to, subject, body));
    }

    public List<SentEmail> sent() {
        return sent;
    }
}
