package com.hpe.ocl.adminUI.user.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.hpe.common.beans.UserProfile;
import com.hpe.ocl.adminUI.user.beans.Currencies;
import com.hpe.ocl.adminUI.user.beans.Permissions;
import com.hpe.ocl.adminUI.user.beans.RolePermission;
import com.hpe.ocl.adminUI.user.beans.User;

public interface IUserDao {

	List<RolePermission> loadAllRolesAndPerm();

	String fetchUser(String emailId);
	
	void deleteFromLdapGrpUsr(String emailId);
	
	void deleteFromUsrPerm(String emailId);

	void insertIntoLdapGrpUsr(User user);
	
	void insertIntoUsrPerm(User user);

	void insertIntoUserProfile(User user);
	
	List<Permissions> fetchUserPermissions(String emailId);

	//Map<String, List<Object>> fetchUserRoles();

	List<String> fetchUserRoles(String emailId);

	List<Permissions> permissionForUser(String email);

	Map<String, String> fetchAllUserRole(String email, String roleId);

	UserProfile getCurrentUserProfile(String currentUser);

	List<Currencies> loadAllCurrencies();

	String fetchUserCurrency(String emailId);

	void updateUserProfileCurrency(User user);

	List<Map<String, Object>> fetchWWAdminUserRole();

	/**
	 * US-18451: Prepares HomeSearchDetails for the daily job.
	 * Sets document type to "UNORTH" and date range to last 24 hours.
	 * Uses a system user profile to fetch errored documents.
	 * @return HomeSearchDetails containing errored documents for last 24 hours.
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
		systemUser.setuserprofileid(0);
		systemUser.setusermailid("system@hpe.com");
		
		return searchErrored(details, systemUser);
	}
	
	/**
	 * US-18451: Adds an Excel attachment to the given email.
	 * @param email Email object to add attachment to.
	 * @param fileName Name of the attachment file.
	 * @param fileContent Byte array of file content.
	 * @param contentType MIME type of the file.
	 */
	private void addAttachment(Email email, String fileName, byte[] fileContent, String contentType) {
		email.setAttachmentName(fileName);
		email.setAttachmentContent(fileContent);
		email.setAttachmentType(contentType);
	}
	
	/**
	 * US-18451: Prepares the Email object for the daily errored document report.
	 * Fetches recipient list from DB, sets from address and subject.
	 * @return Email prepared for daily report.
	 */
	private Email prepareDailyEmailData() {
		Email email = new Email();
		String recipientsFromDb = homeSearchDao.getDailyMailRecipients();
		if (recipientsFromDb != null && !recipientsFromDb.trim().isEmpty()) {
			String[] recipients = 
				java.util.Arrays.stream(recipientsFromDb.split(","))
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
	 * US-18451: Prepares the email context for the daily job.
	 * Adds the document list to the email context if available.
	 * @return Context object containing variables for the email template.
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
	 * Prepares the data, generates the Excel file, attaches it to the email, and sends it.
	 * Logs the process and handles exceptions.
	 */
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
		LOG.debug("Daily errored document mail sent successfully");
	}

}