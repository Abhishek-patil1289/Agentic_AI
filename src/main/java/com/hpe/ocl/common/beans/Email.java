package com.hpe.common.beans;

import java.util.Arrays;

/**
 * Email bean class used to construct email messages, including support for attachments.
 * This class contains the necessary fields and methods to set up an email for sending.
 */
public class Email {
    private String[] recipients;
    private String from;
    private String subject;
    private String attachmentName;
    private byte[] attachmentContent;
    private String attachmentType;

    public Email() {
        this.recipients = new String[0];
    }

    public String[] getRecipients() {
        return recipients;
    }

    public void setRecipients(String[] recipients) {
        this.recipients = recipients;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getAttachmentName() {
        return attachmentName;
    }

    public void setAttachmentName(String attachmentName) {
        this.attachmentName = attachmentName;
    }

    public byte[] getAttachmentContent() {
        return attachmentContent;
    }

    public void setAttachmentContent(byte[] attachmentContent) {
        this.attachmentContent = attachmentContent;
    }

    public String getAttachmentType() {
        return attachmentType;
    }

    public void setAttachmentType(String attachmentType) {
        this.attachmentType = attachmentType;
    }
}ent() {
        return attachmentContent;
    }

    public void setAttachmentContent(byte[] attachmentContent) {
        this.attachmentContent = attachmentContent;
    }

    public String getAttachmentType() {
        return attachmentType;
    }

    public void setAttachmentType(String attachmentType) {
        this.attachmentType = attachmentType;
    }

    @Override
    public String toString() {
        return "Email [recipients=" + Arrays.toString(recipients) + ", from=" + from + ", subject=" + subject + ", attachmentName=" + attachmentName + "]";
    }
private String[] recipients;
    private String from;
    private String subject;
    private String attachmentName;
    private byte[] attachmentContent;
    private String attachmentType;

    public Email() {
        this.recipients = new String[0];
    }

    public String[] getRecipients() {
        return recipients;
    }

    public void setRecipients(String[] recipients) {
        this.recipients = recipients;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getAttachmentName() {
        return attachmentName;
    }

    public void setAttachmentName(String attachmentName) {
        this.attachmentName = attachmentName;
    }

    public byte[] getAttachmentContent() {
        return attachmentContent;
    }

    public void setAttachmentContent(byte[] attachmentContent) {
        this.attachmentContent = attachmentContent;
    }

    public String getAttachmentType() {
        return attachmentType;
    }

    public void setAttachmentType(String attachmentType) {
        this.attachmentType = attachmentType;
    }

    @Override
    public String toString() {
        return "Email [recipients=" + Arrays.toString(recipients) + ", from=" + from + ", subject=" + subject + ", attachmentName=" + attachmentName + "]";
    }
}