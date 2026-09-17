package com.hp.om.business.fallout.bean;

import java.io.Serializable;
import java.util.Date;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class Return implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "Return ID cannot be null")
    private Long returnId;

    @NotNull(message = "Return number cannot be null")
    @Size(min = 1, max = 30, message = "Return number must be between 1 and 30 characters")
    private String returnNumber;

    @NotNull(message = "Return date cannot be null")
    private Date returnDate;

    @Size(max = 100, message = "Return description cannot exceed 100 characters")
    private String description;

    @Size(max = 20, message = "Return status cannot exceed 20 characters")
    private String status;

    @Size(max = 50, message = "Created by cannot exceed 50 characters")
    private String createdBy;

    private Date createdDate;

    @Size(max = 50, message = "Last updated by cannot exceed 50 characters")
    private String lastUpdatedBy;

    private Date lastUpdatedDate;

    public Return() {
        super();
    }

    public Long getReturnId() {
        return returnId;
    }

    public void setReturnId(Long returnId) {
        this.returnId = returnId;
    }

    public String getReturnNumber() {
        return returnNumber;
    }

    public void setReturnNumber(String returnNumber) {
        this.returnNumber = returnNumber;
    }

    public Date getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(Date returnDate) {
        this.returnDate = returnDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public String getLastUpdatedBy() {
        return lastUpdatedBy;
    }

    public void setLastUpdatedBy(String lastUpdatedBy) {
        this.lastUpdatedBy = lastUpdatedBy;
    }

    public Date getLastUpdatedDate() {
        return lastUpdatedDate;
    }

    public void setLastUpdatedDate(Date lastUpdatedDate) {
        this.lastUpdatedDate = lastUpdatedDate;
    }

    @Override
    public String toString() {
        return "Return [returnId=" + returnId + ", returnNumber=" + returnNumber + ", returnDate=" + returnDate
                + ", description=" + description + ", status=" + status + ", createdBy=" + createdBy + ", createdDate="
                + createdDate + ", lastUpdatedBy=" + lastUpdatedBy + ", lastUpdatedDate=" + lastUpdatedDate + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((returnId == null) ? 0 : returnId.hashCode());
        result = prime * result + ((returnNumber == null) ? 0 : returnNumber.hashCode());
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
        if (returnNumber == null) {
            if (other.returnNumber != null)
                return false;
        } else if (!returnNumber.equals(other.returnNumber))
            return false;
        return true;
    }
}