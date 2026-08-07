package com.hpe.ocl.adminUI.homeSearch.service;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;

import com.hpe.common.beans.Email;
import com.hpe.common.exception.SystemApplicationException;
import com.hpe.common.log.AppLogger;
import com.hpe.common.log.AppLoggerFactory;
import com.hpe.common.log.AppLoggingDomain;
import com.hpe.ocl.adminUI.common.AdminUIConstants;
import com.hpe.ocl.adminUI.homeSearch.beans.HomeSearchDetails;
import com.hpe.ocl.adminUI.homeSearch.dao.IHomeSearchDao;
import com.hpe.ocl.adminUI.user.beans.UserProfile;
import com.hpe.ocl.email.SendEmail;

@Service
public class HomeSearchServiceImpl implements IHomeSearchService {

    private static final AppLogger LOG = AppLoggerFactory.getLogger(HomeSearchServiceImpl.class, AppLoggingDomain.OCLUI);

    @Autowired
    private IHomeSearchDao homeSearchDao;

    @Autowired
    private SendEmail sendEmail;

    @Autowired
    private java.util.Properties emailProps;

    private HomeSearchDetails homeSearchDetails;

    // --- Existing methods ... (unchanged) ---

    /**
     * US-18451: Prepares HomeSearchDetails for the daily job.
     * Sets documentType to "UNORTH" and date range to last 24 hours.
     * Uses a system user profile with id 0 and email system@hpe.com.
     * Calls existing searchErrored method and returns result.
     */
    private HomeSearchDetails prepareHomeSearchDetailsForDailyJob() {
        HomeSearchDetails details = new HomeSearchDetails();
        details.setDocumentType("UNORTH");

        Date now = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(now);
        cal.add(Calendar.DATE, -1);
        Date yesterdaySameTime = cal.getTime();

        details.setFromDate(yesterdaySameTime);
        details.setToDate(now);

        UserProfile systemUser = new UserProfile();
        systemUser.setUserprofileid(0);
        systemUser.setUsermailid("system@hpe.com");

        return searchErrored(details, systemUser);
    }

    /**
     * US-18451: Adds an attachment to the Email object.
     * Sets attachment name, content, and content type.
     * @param email Email object
     * @param fileName Attachment file name
     * @param fileContent Attachment content bytes
     * @param contentType MIME type
     */
    private void addAttachment(Email email, String fileName, byte[] fileContent, String contentType) {
        email.setAttachmentName(fileName);
        email.setAttachmentContent(fileContent);
        email.setAttachmentType(contentType);
    }

    /**
     * US-18451: Prepares the Email object for the daily errored document report.
     * Fetches recipients from DB, splits and trims to String[].
     * Sets from address and subject.
     * @return Email object
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
     * @return Context object for email template
     */
    private Context prepareMailBodyForDailyJob() {
        Context ctx = new Context();

        if (homeSearchDetails != null) {
            List<HomeSearchDetails> list = checkDocumentListType();
            ctx.setVariable("documentList", list);
        }

        return ctx;
    }

    /**
     * US-18451: CRON job to send the daily errored document mail with Excel attachment.
     * Prepares data, generates Excel, attaches it, and sends email.
     */
    @Transactional
    public void sendDailyDocumentListMail() {
        MDC.put("correlationId", "US-18451-DailyReport");
        LOG.info("Starting DAILY errored document mail job");
        try {
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

            Context ctx = prepareMailBodyForDailyJob();
            sendEmail.emailConfiguration(ctx, getEmailDocumentTemplate(), email);
            sendEmail.sendMailWithAttachment(email);

            LOG.info("Daily errored document mail job completed successfully");
        } catch (Exception e) {
            LOG.error("Error during sending daily errored document mail", e);
            throw e instanceof SystemApplicationException ? (SystemApplicationException) e :
                    new SystemApplicationException(AdminUIConstants.ErrorCodes.HOMESEARCH_EMAIL_ERR_CD,
                            "Error sending daily errored document mail", e);
        } finally {
            MDC.remove("correlationId");
        }
    }

    /**
     * US-18451: Updated to return empty list instead of null for no documents found.
     * @return List<HomeSearchDetails> or empty list
     */
    @Override
    public List<HomeSearchDetails> checkDocumentListType() {
        List<HomeSearchDetails> list = super.checkDocumentListType();
        if (list == null) {
            return Collections.emptyList();
        }
        return list;
    }

    // --- Assume existing methods searchErrored, exportExcelDocument, getEmailDocumentTemplate, checkDocumentListType ---

}