package com.hp.service.core.exception;

import java.io.Serializable;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResponseException implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(ResponseException.class);

    private String code;
    private ApplicationDomainType domain;
    private int number;
    private ExceptionSeverity severity;
    private String message;
    private String messageDetail;

    public ResponseException() {
        // Default constructor
    }

    public ResponseException(String code, ApplicationDomainType domain, int number, ExceptionSeverity severity, String message, String messageDetail) {
        this.code = code;
        this.domain = domain;
        this.number = number;
        this.severity = severity;
        this.message = message;
        this.messageDetail = messageDetail;
    }

    public ResponseException(String code, ApplicationDomainType domain, String numberStr, ExceptionSeverity severity, String message, String messageDetail) {
        this.code = code;
        this.domain = domain;
        try {
            this.number = Integer.parseInt(numberStr);
        } catch (NumberFormatException e) {
            LOG.warn("Number format exception parsing numberStr: {}", numberStr, e);
            this.number = 0;
        }
        this.severity = severity;
        this.message = message;
        this.messageDetail = messageDetail;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public ApplicationDomainType getDomain() {
        return domain;
    }

    public void setDomain(ApplicationDomainType domain) {
        this.domain = domain;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
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

    public String getMessageDetail() {
        return messageDetail;
    }

    public void setMessageDetail(String messageDetail) {
        this.messageDetail = messageDetail;
    }

    @Override
    public String toString() {
        return "ResponseException{" +
                "code='" + code + '\'' +
                ", domain=" + domain +
                ", number=" + number +
                ", severity=" + severity +
                ", message='" + message + '\'' +
                ", messageDetail='" + messageDetail + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        ResponseException that = (ResponseException) obj;

        if (number != that.number) return false;
        if (code != null ? !code.equals(that.code) : that.code != null) return false;
        if (domain != that.domain) return false;
        if (severity != that.severity) return false;
        if (message != null ? !message.equals(that.message) : that.message != null) return false;
        return messageDetail != null ? messageDetail.equals(that.messageDetail) : that.messageDetail == null;
    }

    @Override
    public int hashCode() {
        int result = code != null ? code.hashCode() : 0;
        result = 31 * result + (domain != null ? domain.hashCode() : 0);
        result = 31 * result + number;
        result = 31 * result + (severity != null ? severity.hashCode() : 0);
        result = 31 * result + (message != null ? message.hashCode() : 0);
        result = 31 * result + (messageDetail != null ? messageDetail.hashCode() : 0);
        return result;
    }

    /**
     * Returns true if the severity is Error_Save_Allowed or Error_Save_Not_Allowed.
     * @return boolean indicating if this exception is an error.
     */
    public boolean isError() {
        return severity == ExceptionSeverity.Error_Save_Allowed || severity == ExceptionSeverity.Error_Save_Not_Allowed;
    }

    /**
     * Returns true if the severity is a warning.
     * @return boolean indicating if this exception is a warning.
     */
    public boolean isWarning() {
        return severity == ExceptionSeverity.WARN;
    }

    /**
     * Returns true if the severity is informational.
     * @return boolean indicating if this exception is informational.
     */
    public boolean isInfo() {
        return severity == ExceptionSeverity.INFO;
    }

    /**
     * Returns true if the exception has a code that equals JSR303.
     * @return boolean indicating if this is a JSR303 validation exception.
     */
    public boolean isJSR303() {
        return StringUtils.equalsIgnoreCase(this.code, "JSR303");
    }
}