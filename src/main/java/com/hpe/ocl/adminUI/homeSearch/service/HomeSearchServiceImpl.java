package com.hpe.ocl.adminUI.homeSearch.service;

import com.hpe.common.beans.Email;
import com.hpe.common.beans.UserProfile;
import com.hpe.common.exceptions.SystemApplicationException;
import com.hpe.ocl.adminUI.homeSearch.beans.HomeSearchDetails;
import com.hpe.ocl.adminUI.homeSearch.dao.IHomeSearchDao;
import org.slf4j.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Properties;

@Service
public class HomeSearchServiceImpl implements IHomeSearchService {

    private static final Logger LOG = LoggerFactory.getLogger(HomeSearchServiceImpl.class);

    private static final int HOMESEARCH_EMAIL_ERR_CD = 18000; // US-18451

    @Autowired
    @Qualifier("emailProperties")
    private Properties emailProps;

    @Autowired
    private SendEmail sendEmail;

    @Autowired
    private IHomeSearchDao homeSearchDao;

    private HomeSearchDetails homeSearchDetails;

    // Existing methods assumed (searchErrored, exportExcelDocument, checkDocumentListType, etc.)

    /**
     * US-18451: Prepare daily search criteria for errored documents report.
     * Sets documentType to "UNORTH" and date range to last 24 hours.
     * Uses system user profile.
     * Calls existing searchErrored method and returns the result.
     *
     * @return HomeSearchDetails with daily errored documents.
     */
    // US-18451
    private HomeSearchDetails prepareHomeSearchDetailsForDailyJob() {
        LOG.debug("US-18451: Preparing daily search criteria for errored documents");
        HomeSearchDetails details = new HomeSearchDetails();
        details.setDocumentType("UNORTH");

        Calendar cal = Calendar.getInstance();
        Date toDate = cal.getTime();
        cal.add(Calendar.DAY_OF_MONTH, -1);
        Date fromDate = cal.getTime();

        details.setFromDate(fromDate);
        details.setToDate(toDate);

        UserProfile systemUser = new UserProfile();
        systemUser.setUserprofileid(0);
        systemUser.setUsermailid("system@hpe.com");

        // Call existing private method searchErrored, assumed to exist
        HomeSearchDetails result = searchErrored(details, systemUser);
        return result;
    }

    /**
     * US-18451: Adds attachment info to Email object.
     *
     * @param email       Email object to add attachment to.
     * @param fileName    Name of the attachment file.
     * @param fileContent Byte array of the file content.
     * @param contentType MIME content type of the attachment.
     */
    // US-18451
    private void addAttachment(Email email, String fileName, byte[] fileContent, String contentType) {
        email.setAttachmentName(fileName);
        email.setAttachmentContent(fileContent);
        email.setAttachmentType(contentType);
    }

    /**
     * US-18451: Prepare Email object for the daily errored document report.
     * Fetches recipients list from DB, sets from address and subject.
     *
     * @return Email configured with recipients, from, and subject.
     */
    // US-18451
    private Email prepareDailyEmailData() {
        Email email = new Email();
        String recipientsFromDb = homeSearchDao.getDailyMailRecipients();
        if (recipientsFromDb != null && !recipientsFromDb.trim().isEmpty()) {
            String[] recipients = Arrays.stream(recipientsFromDb.split(","))
                                        .map(String::trim)
                                        .toArray(String[]::new);
            email.setRecipients(recipients);
        } else {
            email.setRecipients(new String[0]);
        }
        email.setFrom(emailProps.getProperty("fromAddress"));
        email.setSubject("Daily Errored Document Report");
        return email;
    }

    /**
     * US-18451: Prepare Thymeleaf Context for daily mail body.
     * Sets "documentList" variable if homeSearchDetails is not null.
     *
     * @return Context with variables for email template
     */
    // US-18451
    private Context prepareMailBodyForDailyJob() {
        Context ctx = new Context();
        if (homeSearchDetails != null) {
            List<?> docList = checkDocumentListType();
            ctx.setVariable("documentList", docList);
        }
        return ctx;
    }

    /**
     * US-18451: Public method to send daily errored document list mail.
     * Prepares search details, generates Excel report, prepares email with attachment and sends it.
     * Throws SystemApplicationException on failure.
     */
    // US-18451
    @Scheduled(cron = "${errored.documents.daily.mail.cron}")
    public void sendDailyDocumentListMail() {
        MDC.put("correlationId", "US18451-DailyReport");
        LOG.info("US-18451: Starting daily errored document report email job");
        try {
            this.homeSearchDetails = prepareHomeSearchDetailsForDailyJob();

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try {
                exportExcelDocument(homeSearchDetails, outputStream);
            } catch (IOException e) {
                LOG.error("US-18451: Error exporting Excel document", e);
                throw new SystemApplicationException(HOMESEARCH_EMAIL_ERR_CD, "Failed to export Excel for daily errored document report", e);
            }

            Email email = prepareDailyEmailData();
            addAttachment(email, "Daily_Errored_Documents.xlsx", outputStream.toByteArray(), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

            sendEmail.sendMailWithAttachment(email);
            LOG.info("US-18451: Daily errored document report email sent successfully");
        } catch (Exception e) {
            LOG.error("US-18451: Error sending daily errored document report email", e);
            throw new SystemApplicationException(HOMESEARCH_EMAIL_ERR_CD, "Failed to send daily errored document report email", e);
        } finally {
            MDC.remove("correlationId");
        }
    }

    /**
     * US-18451: Override checkDocumentListType to return empty list if null.
     *
     * @return List of documents or empty list if none.
     */
    // US-18451
    @Override
    public List<?> checkDocumentListType() {
        List<?> docList = super.checkDocumentListType();
        if (docList == null) {
            return Collections.emptyList();
        }
        return docList;
    }

    // Existing methods and fields...

    // Placeholder for existing private searchErrored method
    private HomeSearchDetails searchErrored(HomeSearchDetails details, UserProfile user) {
        // Existing implementation assumed
        // For compilation, returning the details
        return details;
    }

    // Placeholder for existing exportExcelDocument method
    private void exportExcelDocument(HomeSearchDetails details, ByteArrayOutputStream outputStream) throws IOException {
        // Existing implementation assumed
    }

}