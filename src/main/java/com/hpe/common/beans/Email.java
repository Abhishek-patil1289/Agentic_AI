package com.hpe.common.beans;

import java.util.Arrays;

public class Email{
	private String[] recipients = new String[0];
	private String from;
	private String cc;
	private String bcc;
	private String subject;

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

	public String getCc() {
		return cc;
	}

	public void setCc(String cc) {
		this.cc = cc;
	}

	public String getBcc() {
		return bcc;
	}

	public void setBcc(String bcc) {
		this.bcc = bcc;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	// US-18451: Attachment fields for email
	private String attachmentName;
	private byte[] attachmentContent;
	private String attachmentType;
	
	@Override
	public String toString() {
		return "Email [recipients=" + Arrays.toString(recipients) + ", from=" + from + ", cc=" + cc + ", bcc=" + bcc
				+ ", subject=" + subject + "]";
	}

	// US-18451: Getters and setters for email attachment fields.
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
}