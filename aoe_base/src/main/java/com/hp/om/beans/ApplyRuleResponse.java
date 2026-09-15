package com.hp.om.beans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.hp.om.constants.ApplicationDomainType;
import com.hp.om.constants.ExceptionSeverity;

public class ApplyRuleResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private String processCode;
    private ValidationResponseEnum responseType;
    private String description;
    private Quote quote;
    private String maxSeverity;
    private List<ResponseException> responseExceptionList = new ArrayList<>();
    private boolean persistQuote = false;

    public ApplyRuleResponse() {
    }

    public ApplyRuleResponse(String processCode, ValidationResponseEnum responseType, String description) {
        this.processCode = processCode;
        this.responseType = responseType;
        this.description = description;
    }

    public String getProcessCode() {
        return processCode;
    }

    public void setProcessCode(String processCode) {
        this.processCode = processCode;
    }

    public ValidationResponseEnum getResponseType() {
        return responseType;
    }

    public void setResponseType(ValidationResponseEnum responseType) {
        this.responseType = responseType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Quote getQuote() {
        return quote;
    }

    public void setQuote(Quote quote) {
        this.quote = quote;
    }

    public String getMaxSeverity() {
        return maxSeverity;
    }

    public void setMaxSeverity(String maxSeverity) {
        this.maxSeverity = maxSeverity;
    }

    public List<ResponseException> getResponseExceptionList() {
        return responseExceptionList;
    }

    public void setResponseExceptionList(List<ResponseException> responseExceptionList) {
        this.responseExceptionList = responseExceptionList;
    }

    public boolean isPersistQuote() {
        return persistQuote;
    }

    public void setPersistQuote(boolean persistQuote) {
        this.persistQuote = persistQuote;
    }

    /**
     * Adds a ResponseException to the responseExceptionList and updates maxSeverity if necessary.
     *
     * @param responseException the ResponseException to add
     */
    public void addResponseException(ResponseException responseException) {
        if (responseException != null) {
            this.responseExceptionList.add(responseException);
            updateMaxSeverity(responseException.getSeverity());
        }
    }

    /**
     * Updates the maxSeverity field to the highest severity level.
     *
     * @param severity the severity to compare and update
     */
    private void updateMaxSeverity(String severity) {
        if (severity == null) {
            return;
        }
        if (this.maxSeverity == null) {
            this.maxSeverity = severity;
            return;
        }
        // Assuming severity levels based on ExceptionSeverity enum, ordered by importance
        ExceptionSeverity currentMax = ExceptionSeverity.fromString(this.maxSeverity);
        ExceptionSeverity newSeverity = ExceptionSeverity.fromString(severity);
        if (newSeverity != null && currentMax != null && newSeverity.isMoreSevereThan(currentMax)) {
            this.maxSeverity = severity;
        }
    }

    @Override
    public String toString() {
        return "ApplyRuleResponse [processCode=" + processCode + ", responseType=" + responseType + ", description=" + description
                + ", quote=" + (quote != null ? quote.getAssetQuoteNrAndVrsn() : "null") + ", maxSeverity=" + maxSeverity
                + ", responseExceptionList=" + responseExceptionList + ", persistQuote=" + persistQuote + "]";
    }
}