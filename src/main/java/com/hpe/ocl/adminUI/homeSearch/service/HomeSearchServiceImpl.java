package com.hpe.ocl.adminUI.homeSearch.service;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hpe.common.beans.Email;
import com.hpe.common.beans.UserProfile;
import com.hpe.common.exception.SystemApplicationException;
import com.hpe.common.log.AppLogger;
import com.hpe.common.log.AppLoggerFactory;
import com.hpe.common.log.AppLoggingDomain;
import com.hpe.ocl.adminUI.common.AdminUIConstants;
import com.hpe.ocl.adminUI.homeSearch.beans.HomeSearchDetails;
import com.hpe.ocl.adminUI.homeSearch.dao.IHomeSearchDao;
import com.hpe.ocl.adminUI.homeSearch.service.IHomeSearchService;
import com.hpe.ocl.adminUI.mail.SendEmail;
import org.thymeleaf.context.Context;

@Service
public class HomeSearchServiceImpl extends HomeSearchBaseService implements IHomeSearchService {

    private static final AppLogger LOG = AppLoggerFactory.getLogger(HomeSearchServiceImpl.class, AppLoggingDomain.OCLUI);

    @Autowired
    private IHomeSearchDao homeSearchDao;

    @Autowired
    private SendEmail sendEmail;

    @Autowired
    private Properties emailProps;

    private HomeSearchDetails homeSearchDetails;

    /**
     * US-18451: Prepare HomeSearchDetails for the daily errored document report job.
     * Sets documentType to "UNORTH", date range last 24 hours, and system user profile.
     * Calls existing searchErrored method.
     *
     * @return HomeSearchDetails list filtered for last 24 hours errored UNORTH documents.
     */
    private HomeSearchDetails prepareHomeSearchDetailsForDailyJob() {
        HomeSearchDetails details = new HomeSearchDetails();
        details.setDocumentType("UNORTH");

        Calendar toCal = Calendar.getInstance();
        Date toDate = toCal.getTime();
        toCal.add(Calendar.DAY_OF_MONTH, -1);
        Date fromDate = toCal.getTime();

        details.setFromDate(fromDate);
        details.setToDate(toDate);

        UserProfile systemUser = new UserProfile();
        systemUser.setUserprofileid(0);
        systemUser.setUsermailid("system@hpe.com");

        return searchErrored(details, systemUser);
    }

    /**
     * US-18451: Add attachment details to Email bean.
     * Sets file name, content bytes, and MIME content type.
     *
     * @param email Email object to add attachment to
     * @param fileName Attachment file name
     * @param fileContent Attachment content bytes
     * @param contentType MIME content type
     */
    private void addAttachment(Email email, String fileName, byte[] fileContent, String contentType) {
        email.setAttachmentName(fileName);
        email.setAttachmentContent(fileContent);
        email.setAttachmentType(contentType);
    }

    /**
     * US-18451: Prepare Email object for daily errored document report.
     * Fetches recipients list from DB, sets from address and subject.
     *
     * @return Email configured with recipients, from, and subject.
     */
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
    private Context prepareMailBodyForDailyJob() {
        Context ctx = new Context();
        if (homeSearchDetails != null) {
            List<?> docList = checkDocumentListType();
            ctx.setVariable("documentList", docList);
        }
        return ctx;
    }

    /**
     * US-18451: Scheduled method to send daily errored document Excel report email.
     * Generates Excel, prepares Email, adds attachment, and sends email.
     * Logs start and completion.
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
        addAttachment(email, "Daily_Errored_Documents.xlsx", excelBytes, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

        sendEmail.sendMailWithAttachment(email);

        LOG.debug("Daily errored document mail job completed successfully");
    }

    /**
     * US-18451: Override checkDocumentListType to return empty list if null.
     *
     * @return List of documents or empty list.
     */
    @Override
    public List<?> checkDocumentListType() {
        List<?> list = super.checkDocumentListType();
        return list == null ? Collections.emptyList() : list;
    }

}