package com.hp.service.core.exception;

import java.io.Serializable;
import java.util.Objects;

/**
 * ResponseException is a class representing an exception message returned in responses.
 * It contains information about the domain, code, severity, and messages related to the exception.
 */
public class ResponseException implements Serializable {

    private static final long serialVersionUID = 1L;

    private String fieldId;
    private ApplicationDomainType domain;
    private int code;
    private ExceptionSeverity severity;
    private String message;
    private String messageDetails;

    public ResponseException() {
    }

    public ResponseException(String fieldId, ApplicationDomainType domain, int code, ExceptionSeverity severity, String message, String messageDetails) {
        this.fieldId = fieldId;
        this.domain = domain;
        this.code = code;
        this.severity = severity;
        this.message = message;
        this.messageDetails = messageDetails;
    }

    public ResponseException(String fieldId, ApplicationDomainType domain, String codeStr, ExceptionSeverity severity, String message, String messageDetails) {
        this.fieldId = fieldId;
        this.domain = domain;
        try {
            this.code = Integer.parseInt(codeStr);
        } catch (NumberFormatException e) {
            this.code = 0;
        }
        this.severity = severity;
        this.message = message;
        this.messageDetails = messageDetails;
    }

    public String getFieldId() {
        return fieldId;
    }

    public void setFieldId(String fieldId) {
        this.fieldId = fieldId;
    }

    public ApplicationDomainType getDomain() {
        return domain;
    }

    public void setDomain(ApplicationDomainType domain) {
        this.domain = domain;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public ExceptionSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(ExceptionSeverity severity) {
        this.severity = severity;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMessageDetails() {
        return messageDetails;
    }

    public void setMessageDetails(String messageDetails) {
        this.messageDetails = messageDetails;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ResponseException)) return false;
        ResponseException that = (ResponseException) o;
        return code == that.code &&
                Objects.equals(fieldId, that.fieldId) &&
                domain == that.domain &&
                severity == that.severity &&
                Objects.equals(message, that.message) &&
                Objects.equals(messageDetails, that.messageDetails);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fieldId, domain, code, severity, message, messageDetails);
    }

    @Override
    public String toString() {
        return "ResponseException{" +
                "fieldId='" + fieldId + '\'' +
                ", domain=" + domain +
                ", code=" + code +
                ", severity=" + severity +
                ", message='" + message + '\'' +
                ", messageDetails='" + messageDetails + '\'' +
                '}';
    }
}