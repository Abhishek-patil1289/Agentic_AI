package com.hp.om.business.fallout.bean;

import java.io.Serializable;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class Return implements Serializable {

    private static final long serialVersionUID = -1234567890123456789L;

    private static final Logger LOG = LoggerFactory.getLogger(Return.class);

    @NotNull(message = "returnId cannot be null")
    private String returnId;

    @Valid
    private List<@NotNull(message = "returnItem cannot be null") ReturnItem> returnItems;

    private String status;

    private String reasonCode;

    private String comments;

    public Return() {
        LOG.debug("Return object instantiated");
    }

    public Return(String returnId, List<ReturnItem> returnItems, String status, String reasonCode, String comments) {
        this.returnId = returnId;
        this.returnItems = returnItems;
        this.status = status;
        this.reasonCode = reasonCode;
        this.comments = comments;
        LOG.debug("Return object created with id: {}", returnId);
    }

    public String getReturnId() {
        LOG.debug("Getting returnId: {}", returnId);
        return returnId;
    }

    public void setReturnId(String returnId) {
        LOG.debug("Setting returnId: {}", returnId);
        this.returnId = returnId;
    }

    public List<ReturnItem> getReturnItems() {
        LOG.debug("Getting returnItems list, size: {}", returnItems != null ? returnItems.size() : 0);
        return returnItems;
    }

    public void setReturnItems(List<ReturnItem> returnItems) {
        LOG.debug("Setting returnItems list, size: {}", returnItems != null ? returnItems.size() : 0);
        this.returnItems = returnItems;
    }

    public String getStatus() {
        LOG.debug("Getting status: {}", status);
        return status;
    }

    public void setStatus(String status) {
        LOG.debug("Setting status: {}", status);
        this.status = status;
    }

    public String getReasonCode() {
        LOG.debug("Getting reasonCode: {}", reasonCode);
        return reasonCode;
    }

    public void setReasonCode(String reasonCode) {
        LOG.debug("Setting reasonCode: {}", reasonCode);
        this.reasonCode = reasonCode;
    }

    public String getComments() {
        LOG.debug("Getting comments: {}", comments);
        return comments;
    }

    public void setComments(String comments) {
        LOG.debug("Setting comments: {}", comments);
        this.comments = comments;
    }

    @Override
    public String toString() {
        return "Return [returnId=" + returnId + ", returnItems=" + returnItems + ", status=" + status + ", reasonCode=" + reasonCode + ", comments=" + comments + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((returnId == null) ? 0 : returnId.hashCode());
        result = prime * result + ((returnItems == null) ? 0 : returnItems.hashCode());
        result = prime * result + ((status == null) ? 0 : status.hashCode());
        result = prime * result + ((reasonCode == null) ? 0 : reasonCode.hashCode());
        result = prime * result + ((comments == null) ? 0 : comments.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Return other = (Return) obj;
        if (returnId == null) {
            if (other.returnId != null)
                return false;
        } else if (!returnId.equals(other.returnId))
            return false;
        if (returnItems == null) {
            if (other.returnItems != null)
                return false;
        } else if (!returnItems.equals(other.returnItems))
            return false;
        if (status == null) {
            if (other.status != null)
                return false;
        } else if (!status.equals(other.status))
            return false;
        if (reasonCode == null) {
            if (other.reasonCode != null)
                return false;
        } else if (!reasonCode.equals(other.reasonCode))
            return false;
        if (comments == null) {
            if (other.comments != null)
                return false;
        } else if (!comments.equals(other.comments))
            return false;
        return true;
    }

}