package com.hpe.ocl.adminUI.homeSearch.beans;

import java.util.Date;
import java.util.List;

import com.hpe.common.beans.KeyValueProp;
import com.hpe.ocl.adminUI.common.StatusContainer;

public class HomeSearchDetails extends StatusContainer {

    private String documentType;
    private String accountStatus;
    private String accountName;
    private String senderID;
    private String b2BiTracking;
    private String docAction;
    private String region;
    private String orderSource;
    private Date fromDate;
    private Date toDate;
    private String supplierOrderNumber;
    private String poNumber;
    private String accountID;
    private String ncrfID;
    private String docStatus;
    private String reportStatus;
    private String orderDestination;
    private String pageAction;
    private String tenant = "HPE";
    private String verifiedTenant = "HPE";
    private String tenantList = "HPE";
    private int totalRecords = 0;
    private String view;
    private String totalSumDisplay;
    private int startGroupNumber;
    private int endGroupNumber;
    private String createDate;
    private double totalPrice;
    private double total;
    private String sort = "";
    private int sortInd = 1;
    private String toCurrency;
    private String fromCurrency;
    private String docReceived;
    private String docSent;
    private int logSequenceId;
    private String cart;
    private String roudtripStore;
    private String rtID;
    private String punchoutRequest;
    private String cartReturn;
    private List<HomeSearchDetails> roundtrip;
    private List<HomeSearchDetails> poDocument;
    private Date dataFormat;
    private List<HomeSearchDetails> erroredDocument;
    private String buyerSenderId;
    private String errorId;
    private String errorMessage;
    private String detailedErrorMessage;
    private String customerID;
    private String attachment;
    private String searchName;
    private String quickSearchId;
    private String quickSearchDetails;
    private String s4OrderNumber;
    private List<KeyValueProp> orderDestinationList;
    private List<KeyValueProp> accountStatusList;
    private List<KeyValueProp> documentStatusList;

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public String getAccountStatus() {
        return accountStatus;
    }

    public void setAccountStatus(String accountStatus) {
        this.accountStatus = accountStatus;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getSenderID() {
        return senderID;
    }

    public void setSenderID(String senderID) {
        this.senderID = senderID;
    }

    public String getB2BiTracking() {
        return b2BiTracking;
    }

    public void setB2BiTracking(String b2BiTracking) {
        this.b2BiTracking = b2BiTracking;
    }

    public String getDocAction() {
        return docAction;
    }

    public void setDocAction(String docAction) {
        this.docAction = docAction;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getOrderSource() {
        return orderSource;
    }

    public void setOrderSource(String orderSource) {
        this.orderSource = orderSource;
    }

    public Date getFromDate() {
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public String getSupplierOrderNumber() {
        return supplierOrderNumber;
    }

    public void setSupplierOrderNumber(String supplierOrderNumber) {
        this.supplierOrderNumber = supplierOrderNumber;
    }

    public String getPoNumber() {
        return poNumber;
    }

    public void setPoNumber(String poNumber) {
        this.poNumber = poNumber;
    }

    public String getAccountID() {
        return accountID;
    }

    public void setAccountID(String accountID) {
        this.accountID = accountID;
    }

    public String getNcrfID() {
        return ncrfID;
    }

    public void setNcrfID(String ncrfID) {
        this.ncrfID = ncrfID;
    }

    public String getDocStatus() {
        return docStatus;
    }

    public void setDocStatus(String docStatus) {
        this.docStatus = docStatus;
    }

    public String getReportStatus() {
        return reportStatus;
    }

    public void setReportStatus(String reportStatus) {
        this.reportStatus = reportStatus;
    }

    public String getOrderDestination() {
        return orderDestination;
    }

    public void setOrderDestination(String orderDestination) {
        this.orderDestination = orderDestination;
    }

    public String getPageAction() {
        return pageAction;
    }

    public void setPageAction(String pageAction) {
        this.pageAction = pageAction;
    }

    public String getTenant() {
        return tenant;
    }

    public void setTenant(String tenant) {
        this.tenant = tenant;
    }

    public String getVerifiedTenant() {
        return verifiedTenant;
    }

    public void setVerifiedTenant(String verifiedTenant) {
        this.verifiedTenant = verifiedTenant;
    }

    public String getTenantList() {
        return tenantList;
    }

    public void setTenantList(String tenantList) {
        this.tenantList = tenantList;
    }

    public int getTotalRecords() {
        return totalRecords;
    }

    public void setTotalRecords(int totalRecords) {
        this.totalRecords = totalRecords;
    }

    public String getView() {
        return view;
    }

    public void setView(String view) {
        this.view = view;
    }

    public String getTotalSumDisplay() {
        return totalSumDisplay;
    }

    public void setTotalSumDisplay(String totalSumDisplay) {
        this.totalSumDisplay = totalSumDisplay;
    }

    public int getStartGroupNumber() {
        return startGroupNumber;
    }

    public void setStartGroupNumber(int startGroupNumber) {
        this.startGroupNumber = startGroupNumber;
    }

    public int getEndGroupNumber() {
        return endGroupNumber;
    }

    public void setEndGroupNumber(int endGroupNumber) {
        this.endGroupNumber = endGroupNumber;
    }

    public String getCreateDate() {
        return createDate;
    }

    public void setCreateDate(String createDate) {
        this.createDate = createDate;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }

    public int getSortInd() {
        return sortInd;
    }

    public void setSortInd(int sortInd) {
        this.sortInd = sortInd;
    }

    public String getToCurrency() {
        return toCurrency;
    }

    public void setToCurrency(String toCurrency) {
        this.toCurrency = toCurrency;
    }

    public String getFromCurrency() {
        return fromCurrency;
    }

    public void setFromCurrency(String fromCurrency) {
        this.fromCurrency = fromCurrency;
    }

    public String getDocReceived() {
        return docReceived;
    }

    public void setDocReceived(String docReceived) {
        this.docReceived = docReceived;
    }

    public String getDocSent() {
        return docSent;
    }

    public void setDocSent(String docSent) {
        this.docSent = docSent;
    }

    public int getLogSequenceId() {
        return logSequenceId;
    }

    public void setLogSequenceId(int logSequenceId) {
        this.logSequenceId = logSequenceId;
    }

    public String getCart() {
        return cart;
    }

    public void setCart(String cart) {
        this.cart = cart;
    }

    public String getRoudtripStore() {
        return roudtripStore;
    }

    public void setRoudtripStore(String roudtripStore) {
        this.roudtripStore = roudtripStore;
    }

    public String getRtID() {
        return rtID;
    }

    public void setRtID(String rtID) {
        this.rtID = rtID;
    }

    public String getPunchoutRequest() {
        return punchoutRequest;
    }

    public void setPunchoutRequest(String punchoutRequest) {
        this.punchoutRequest = punchoutRequest;
    }

    public String getCartReturn() {
        return cartReturn;
    }

    public void setCartReturn(String cartReturn) {
        this.cartReturn = cartReturn;
    }

    public List<HomeSearchDetails> getRoundtrip() {
        return roundtrip;
    }

    public void setRoundtrip(List<HomeSearchDetails> roundtrip) {
        this.roundtrip = roundtrip;
    }

    public List<HomeSearchDetails> getPoDocument() {
        return poDocument;
    }

    public void setPoDocument(List<HomeSearchDetails> poDocument) {
        this.poDocument = poDocument;
    }

    public Date getDataFormat() {
        return dataFormat;
    }

    public void setDataFormat(Date dataFormat) {
        this.dataFormat = dataFormat;
    }

    public List<HomeSearchDetails> getErroredDocument() {
        return erroredDocument;
    }

    public void setErroredDocument(List<HomeSearchDetails> erroredDocument) {
        this.erroredDocument = erroredDocument;
    }

    public String getBuyerSenderId() {
        return buyerSenderId;
    }

    public void setBuyerSenderId(String buyerSenderId) {
        this.buyerSenderId = buyerSenderId;
    }

    public String getErrorId() {
        return errorId;
    }

    public void setErrorId(String errorId) {
        this.errorId = errorId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getDetailedErrorMessage() {
        return detailedErrorMessage;
    }

    public void setDetailedErrorMessage(String detailedErrorMessage) {
        this.detailedErrorMessage = detailedErrorMessage;
    }

    public String getCustomerID() {
        return customerID;
    }

    public void setCustomerID(String customerID) {
        this.customerID = customerID;
    }

    public String getAttachment() {
        return attachment;
    }

    public void setAttachment(String attachment) {
        this.attachment = attachment;
    }

    public String getSearchName() {
        return searchName;
    }

    public void setSearchName(String searchName) {
        this.searchName = searchName;
    }

    public String getQuickSearchId() {
        return quickSearchId;
    }

    public void setQuickSearchId(String quickSearchId) {
        this.quickSearchId = quickSearchId;
    }

    public String getQuickSearchDetails() {
        return quickSearchDetails;
    }

    public void setQuickSearchDetails(String quickSearchDetails) {
        this.quickSearchDetails = quickSearchDetails;
    }

    public String getS4OrderNumber() {
        return s4OrderNumber;
    }

    public void setS4OrderNumber(String s4OrderNumber) {
        this.s4OrderNumber = s4OrderNumber;
    }

    public List<KeyValueProp> getOrderDestinationList() {
        return orderDestinationList;
    }

    public void setOrderDestinationList(List<KeyValueProp> orderDestinationList) {
        this.orderDestinationList = orderDestinationList;
    }

    public List<KeyValueProp> getAccountStatusList() {
        return accountStatusList;
    }

    public void setAccountStatusList(List<KeyValueProp> accountStatusList) {
        this.accountStatusList = accountStatusList;
    }

    public List<KeyValueProp> getDocumentStatusList() {
        return documentStatusList;
    }

    public void setDocumentStatusList(List<KeyValueProp> documentStatusList) {
        this.documentStatusList = documentStatusList;
    }

}