package com.hpe.ocl.adminUI.homeSearch.service;

import com.hpe.common.beans.Email;
import com.hpe.common.exception.SystemApplicationException;
import com.hpe.ocl.adminUI.homeSearch.dao.IHomeSearchDao;
import com.hpe.ocl.adminUI.homeSearch.model.HomeSearchDetails;
import com.hpe.ocl.adminUI.homeSearch.model.UserProfile;
import com.hpe.ocl.adminUI.homeSearch.util.CommonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;

import java.io.ByteArrayInputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Properties;

@Service
public class HomeSearchServiceImpl implements IHomeSearchService {

    private static final Logger LOG = LoggerFactory.getLogger(HomeSearchServiceImpl.class);

    @Autowired
    @Qualifier("emailProperties")
    Properties emailProps;

    @Autowired 
    private SendEmail sendEmail;

    @Autowired
    public IHomeSearchDao homeSearchDao;

    HomeSearchDetails homeSearchDetails;

    @Override
    public LoadHomeSearch loadHomeSearchDropDownList() {
        UserProfile userInfo = CommonUtil.getCurrentUserProfile();
        LOG.debug("loadHomeSearchDropDownList(): User email id:: " + userInfo.getusermailid());

        LoadHomeSearch homeSearchDetails = new LoadHomeSearch();
        List<HomeSearchDetails> list = homeSearchDao.getHomeSearchDetails();
        homeSearchDetails.setHomeSearchDetails(list);
        return homeSearchDetails;
    }

    /**
     * US-18451 Daily Report: Prepares HomeSearchDetails for the daily job.
     * Sets the document type to "UNORTH" and the date range to the last 24 hours.
     * Uses a system user profile to fetch errored documents.
     *
     * @return HomeSearchDetails containing errored documents for the previous day.
     */
    private HomeSearchDetails prepareHomeSearchDetailsForDailyJob() {
        HomeSearchDetails details = new HomeSearchDetails();
        details.setDocumentType("UNORTH");

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        Date fromDate = cal.getTime();
        Date toDate = new Date();

        details.setFromDate(fromDate);
        details.setToDate(toDate);
        details.setUserProfile(new UserProfile(0, "system@hpe.com")); // System user

        return details;
    }

    /**
     * US-18451: Sends an email with the daily errored document report.
     */
    @Transactional(readOnly = true)
    public void sendDailyDocumentListMail() {
        LOG.debug("Starting DAILY errored document mail job");
        this.homeSearchDetails = prepareHomeSearchDetailsForDailyJob();
        ByteArrayInputStream excelStream = exportExcelDocument();
        byte[] excelBytes;

        try {
            excelBytes = excelStream.readAllBytes();
        } catch (Exception e) {
            throw new SystemApplicationException(
                    AdminUIConstants.ErrorCodes.HOMESEARCH_EMAIL_ERR_CD,
                    "Failed to generate Excel for daily mail",
                    e
            );
        }
        Email email = prepareDailyEmailData();
        addAttachment(
                email,
                "Daily_Errored_Documents.xlsx",
                excelBytes,
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        );
        sendEmail.sendMailWithAttachment(email);
        LOG.debug("DAILY errored document mail sent successfully");
    }

    /**
     * US-18451: Prepares the Email object for the daily errored document report.
     * This method fetches the recipient list from the database, splits the comma-separated
     * string into an array, and sets it as the recipients of the email. It also sets the
     * sender's address and the subject for the daily report.
     */
    private Email prepareDailyEmailData() {
        Email email = new Email();
        String recipientsFromDb = homeSearchDao.getDailyMailRecipients();
        email.setRecipients(
                Arrays.stream(recipientsFromDb.split(","))
                        .map(String::trim)
                        .toArray(String[]::new)
        );

        email.setFrom(emailProps.getProperty("fromAddress"));
        email.setSubject("Daily Errored Document Report");

        return email;
    }

    /**
     * US-18451: Prepares the email context for the daily job.
     * Adds the document list to the email context if available.
     *
     * @return Context object containing variables for the email template.
     */
    private Context prepareMailBodyForDailyJob() {
        Context ctx = new Context();

        if (homeSearchDetails != null) {
            List<HomeSearchDetails> documentList = checkDocumentListType();
            ctx.setVariable("documentList", documentList);
        }

        return ctx;
    }

    /**
     * Adds an attachment to the email object.
     *
     * @param email the email object to attach to
     * @param fileName the name of the file to attach
     * @param fileContent the content of the file
     * @param contentType the MIME type of the file
     */
    private void addAttachment(Email email, String fileName, byte[] fileContent, String contentType) {
        email.setAttachmentName(fileName);
        email.setAttachmentContent(fileContent);
        email.setAttachmentType(contentType);
    }

    // Other existing methods...
}
