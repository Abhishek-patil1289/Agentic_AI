package com.hp.om.beans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.Length;

import com.hp.om.beans.constants.ExceptionSeverity;
import com.hp.om.beans.constants.ValidationResponseEnum;
import com.hp.om.beans.validataion.FVOValidationException;

/**
 * ApplyRuleResponse is a bean class to represent response from rule processing.
 * It holds validation messages, exceptions, and the Quote involved.
 * 
 * It implements Serializable for remote communication.
 * 
 * It uses bean validation annotations for field validations.
 */
public class ApplyRuleResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * List of validation exceptions that occurred during rule application.
     */
    private List<FVOValidationException> fvoValidationExceptionList = new ArrayList<>();

    /**
     * List of response exceptions from rule processing.
     */
    private List<ResponseException> responseExceptionList = new ArrayList<>();

    /**
     * The Quote object associated with this response.
     */
    private Quote quote;

    /**
     * The maximum severity level among all exceptions.
     * Should correspond to ExceptionSeverity constants.
     */
    private String maxSeverity = ExceptionSeverity.Info.toString();

    /**
     * The process code representing the rule process identifier.
     */
    @NotNull
    @Length(min = 1, max = 50)
    private String processCode;

    /**
     * The type of the response, e.g. ValidationMessages.
     */
    private ValidationResponseEnum responseType = ValidationResponseEnum.VALIDATIONMESSAGES;

    /**
     * Description of the response.
     */
    private String description;

    /**
     * Flag indicating if the Quote should be persisted.
     */
    private boolean persistQuote = false;

    public ApplyRuleResponse() {
    }

    public List<FVOValidationException> getFvoValidationExceptionList() {
        return fvoValidationExceptionList;
    }

    public void setFvoValidationExceptionList(List<FVOValidationException> fvoValidationExceptionList) {
        this.fvoValidationExceptionList = fvoValidationExceptionList;
    }

    public List<ResponseException> getResponseExceptionList() {
        return responseExceptionList;
    }

    public void setResponseExceptionList(List<ResponseException> responseExceptionList) {
        this.responseExceptionList = responseExceptionList;
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

    public boolean isPersistQuote() {
        return persistQuote;
    }

    public void setPersistQuote(boolean persistQuote) {
        this.persistQuote = persistQuote;
    }

    @Override
    public String toString() {
        return "ApplyRuleResponse [fvoValidationExceptionList=" + fvoValidationExceptionList + ", responseExceptionList="
                + responseExceptionList + ", quote=" + quote + ", maxSeverity=" + maxSeverity + ", processCode="
                + processCode + ", responseType=" + responseType + ", description=" + description + ", persistQuote="
                + persistQuote + "]";
    }
}