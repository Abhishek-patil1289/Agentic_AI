package com.hpe.ocl.adminUI.homeSearch.service;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hpe.common.beans.Email;
import com.hpe.common.beans.UserProfile;
import com.hpe.common.exception.SystemApplicationException;
import com.hpe.ocl.adminUI.common.AdminUIConstants;
import com.hpe.ocl.adminUI.homeSearch.beans.HomeSearchDetails;
import com.hpe.ocl.adminUI.homeSearch.dao.IHomeSearchDao;

import org.thymeleaf.context.Context;

@Service
public class HomeSearchServiceImpl implements IHomeSearchService {

    private static final Logger LOG = LoggerFactory.getLogger(HomeSearchServiceImpl.class);

    @Autowired
    private IHomeSearchDao homeSearchDao;

    @Autowired
    private SendEmail sendEmail;

    @Autowired
    private java.util.Properties emailProps;

    private HomeSearchDetails homeSearchDetails;

    // Existing methods...

    /**
     * US-18451: Prepares the HomeSearchDetails for daily job (last 24h, UNORTH doc type, system user).
     */
    private HomeSearchDetails prepareHomeSearchDetailsForDailyJob() {
        HomeSearchDetails details = new HomeSearchDetails();
        details.setDocumentType("UNORTH");

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        details.setFromDate(cal.getTime());
        details.setToDate(Calendar.getInstance().getTime());

        UserProfile systemUser = new UserProfile();
        systemUser.setUserprofileid(0);
        systemUser.setUsermailid("system@hpe.com");
        details.setUserProfile(systemUser);

        return details;
    }

    /**
     * US-18451: Adds an attachment to the email.
     */
    private void addAttachment(Email email, String fileName, byte[] fileContent, String contentType) {
        email.setAttachmentName(fileName);
        email.setAttachmentContent(fileContent);
        email.setAttachmentType(contentType);
    }

    /**
     * US-18451: Prepares the Email object for the daily errored document report.
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
     * US-18451: Prepare the mail body context for the daily job email.
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
        addAttachment(email, "Daily_Errored_Documents.xlsx", excelBytes, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

        sendEmail.sendMailWithAttachment(email);

        LOG.debug("DAILY errored document mail sent successfully");
    }

    /**
     * US-18451: Override checkDocumentListType to return empty list instead of null.
     */
    private List<?> checkDocumentListType() {
        List<?> documents = getDocumentListFromSomewhere(); // placeholder for actual retrieval logic
        if (documents == null) {
            return Collections.emptyList();
        }
        return documents;
    }

    // Placeholder method to represent exportExcelDocument
    private ByteArrayInputStream exportExcelDocument() {
        // Existing implementation assumed
        return null;
    }

    // Other existing methods...
}