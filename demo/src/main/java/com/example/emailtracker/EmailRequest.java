package com.example.emailtracker;

import java.util.List;

public class EmailRequest {
    private String from;
    private String subject;
    private String htmlTemplate;
    private List<Recipient> recipients;

    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getHtmlTemplate() { return htmlTemplate; }
    public void setHtmlTemplate(String htmlTemplate) { this.htmlTemplate = htmlTemplate; }
    public List<Recipient> getRecipients() { return recipients; }
    public void setRecipients(List<Recipient> recipients) { this.recipients = recipients; }
} 