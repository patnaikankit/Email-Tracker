package com.example.emailtracker;

import java.util.List;
import java.util.Map;

public class EmailRequest {
    private Recipients recipients;
    private EmailBody emailBody;

    public Recipients getRecipients() {
        return recipients;
    }

    public void setRecipients(Recipients recipients) {
        this.recipients = recipients;
    }

    public EmailBody getEmailBody() {
        return emailBody;
    }

    public void setEmailBody(EmailBody emailBody) {
        this.emailBody = emailBody;
    }
}

class Recipients {
    private List<Receiver> receivers;
    private String from;

    public List<Receiver> getReceivers() {
        return receivers;
    }

    public void setReceivers(List<Receiver> receivers) {
        this.receivers = receivers;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }
}

class Receiver {
    private String email;
    private String trackingId;
    private boolean wantToTrack;
    private String type;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTrackingId() {
        return trackingId;
    }

    public void setTrackingId(String trackingId) {
        this.trackingId = trackingId;
    }

    public boolean isWantToTrack() {
        return wantToTrack;
    }

    public void setWantToTrack(boolean wantToTrack) {
        this.wantToTrack = wantToTrack;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}

class EmailBody {
    private String htmlTemplate;
    private String subject;
    private Map<String, Map<String, String>> parameters;

    public String getHtmlTemplate() {
        return htmlTemplate;
    }

    public void setHtmlTemplate(String htmlTemplate) {
        this.htmlTemplate = htmlTemplate;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public Map<String, Map<String, String>> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Map<String, String>> parameters) {
        this.parameters = parameters;
    }
} 