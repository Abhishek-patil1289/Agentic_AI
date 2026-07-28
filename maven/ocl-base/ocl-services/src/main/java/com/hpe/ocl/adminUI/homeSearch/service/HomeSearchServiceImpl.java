package com.hpe.ocl.adminUI.homeSearch.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Properties;
import java.util.*;

import org.apache.commons.lang.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hpe.common.beans.Email;
import com.hpe.common.beans.KeyValueProp;
import com.hpe.common.beans.UserProfile;
import com.hpe.common.email.SendEmail;
import com.hpe.common.exception.SystemApplicationException;
import com.hpe.common.log.AppLogger;
import com.hpe.common.log.AppLoggerFactory;
import com.hpe.common.log.AppLoggingDomain;
import com.hpe.common.util.CommonUtil;
import com.hpe.ocl.adminUI.account.beans.AccountDropDown;
import com.hpe.ocl.adminUI.account.beans.RegionAndSource;
import com.hpe.ocl.adminUI.account.dao.IAccountDao;
import com.hpe.ocl.adminUI.account.service.IAccountService;
import com.hpe.ocl.adminUI.account.service.IManageAccountService;
import com.hpe.ocl.adminUI.common.AdminUIConstants;
import com.hpe.ocl.adminUI.common.BDEUtils;
import com.hpe.ocl.adminUI.common.StatusContainer;
import com.hpe.ocl.adminUI.homeSearch.beans.HomeSearchDetails;
import com.hpe.ocl.adminUI.homeSearch.beans.LoadHomeSearch;
import com.hpe.ocl.adminUI.homeSearch.beans.QuickSearchDetails;
import com.hpe.ocl.adminUI.homeSearch.dao.IHomeSearchDao;
import org.springframework.beans.factory.annotation.Value;
import java.util.Date;

@Service
public class HomeSearchServiceImpl implements IHomeSearchService {

	private static final AppLogger LOG = AppLoggerFactory.getLogger(HomeSearchServiceImpl.class, AppLoggingDomain.OCLUI);
	
	@Autowired
	@Qualifier("emailProperties")
	Properties emailProps;
	
	@Autowired 
	private SendEmail sendEmail;
	
	@Autowired
	public IHomeSearchDao homeSearchDao;
	
	@Autowired
	private IAccountService accountService;
	
	@Autowired
	private IAccountDao accountDao;
	
	@Autowired
	private IManageAccountService manageAccService;


	HomeSearchDetails homeSearchDetails;

	@Override
	public LoadHomeSearch loadHomeSearchDropDownList() {
		UserProfile userInfo = CommonUtil.getCurrentUserProfile();
		LOG.debug("loadHomeSearchDropDownList(): User email id:: "+userInfo.getusermailid());

		LoadHomeSearch homeSearchDetails = new LoadHomeSearch();
		List<KeyValueProp> docTypeList = homeSearchDao.getDocTypeList();
		homeSearchDetails.setDocTypeList(docTypeList);
		// US-17847_OCL-AMS option to search
		List<RegionAndSource> regionSource = null;
		
		// US-17847_OCL-AMS option to search - Cache constant changed to to include AMS without affecting
		// other pages
		if(AdminUIConstants.acctDDFORHOMESEARCH != null) {
			
			regionSource = AdminUIConstants.acctDDFORHOMESEARCH.getRegionSource();
		}else
		{
			//accountService.createAccountDD().getRegionSource();
			// get region source with AMS from DB and load to cache
			AdminUIConstants.acctDDFORHOMESEARCH = new AccountDropDown();
			manageAccService.loadRegionDDWIthAMS(AdminUIConstants.acctDDFORHOMESEARCH ,"ACCOUNT.FETCH_ORDER_DEST_PER_SOURCE");
			regionSource = AdminUIConstants.acctDDFORHOMESEARCH.getRegionSource();
			
		}
		 //List<KeyValueProp> region = manageRTDataDao.getRegionList();
		 //homeSearchDetails.setRegionList(region);
		// Set region Source
		homeSearchDetails.setRegionSource(regionSource);
		 // US-17847_OCL-AMS option to search - end
		
		 List<KeyValueProp> reportStatus = homeSearchDao.getReportStatus();
		 homeSearchDetails.setReportStatusList(reportStatus);
		 
		 List<KeyValueProp> docAction = homeSearchDao.getDocAction();
		 homeSearchDetails.setDocActionList(docAction);
		 
		 MapSqlParameterSource params = new MapSqlParameterSource();
		 params.addValue("userid",userInfo.getuserprofileid());
		 params.addValue("pageName", "DocumentSearch");
		 List<KeyValueProp> quickSearch = homeSearchDao.getQuickSearch(params,"HOME_SEARCH.QUICK_SEARCH");
		 homeSearchDetails.setQuicksearchList(quickSearch);
		 
		 List<KeyValueProp> orderSource = homeSearchDao.getOrderSource();
		 homeSearchDetails.setOrderSourceList(orderSource);
		 
		 List<KeyValueProp> orderDestination = homeSearchDao.getOrderDestination();
		homeSearchDetails.setOrderDestinationList(orderDestination);
				 
		 List<KeyValueProp> accountStatus = homeSearchDao.getAccountStatus();
		 homeSearchDetails.setAccountStatusList(accountStatus);
		 
		 List<KeyValueProp> documentStatus = homeSearchDao.getDocStatus();
		 homeSearchDetails.setDocumentStatusList(documentStatus);
		 
		return homeSearchDetails; 
	}

	@Override
	public HomeSearchDetails searchDocument(HomeSearchDetails homeSearch, String action) {
		UserProfile userInfo = CommonUtil.getCurrentUserProfile();
		LOG.debug("searchDocument(): User profile data from session: "+userInfo.getusermailid()+" "+userInfo.getCurrency());
		
		if (StringUtils.isNotEmpty(action))
		{
			if (action != null && action.equalsIgnoreCase(AdminUIConstants.ToolsConstants.SEARCH))
			{
				
					 if (BDEUtils.isEqualsIgnoreCase(homeSearch.getDocumentType() ,"UNORTH"))
						{
						 	homeSearchDetails=searchErrored(homeSearch, userInfo);
						}
						else if(BDEUtils.isEqualsIgnoreCase(homeSearch.getDocumentType() ,"RTRIP"))
						{
							homeSearchDetails=searchRoundTrip(homeSearch, userInfo);
						}
						else 
						{
							homeSearchDetails=searchPoDocument(homeSearch, userInfo);
						}
				}
			else if (action != null && action.equalsIgnoreCase(AdminUIConstants.ToolsConstants.BASIC_SEARCH))
			{
				 homeSearchDetails=basicSearch(homeSearch, userInfo);
			}
			
		}
		
		return homeSearch;
	}

	private HomeSearchDetails basicSearch(HomeSearchDetails homeSearch, UserProfile userInfo) {
	// TODO Auto-generated method stub
		LOG.debug("Inside basicSearch() service..");
		try {
		HashMap countMap = homeSearchDao.getBasicSearchCount(userInfo,homeSearch);
		LOG.debug("Taking Record count  for Basic Search..");
		if (countMap!=null && countMap.size()>0)
		{
			int recordCount = ((Integer)countMap.get("documentCount")).intValue();
			homeSearch.setTotalRecords(recordCount);
			LOG.debug("Searching Result for Basic Search ..");
		if (homeSearch.getTotalRecords()>0)
		{
			java.text.SimpleDateFormat dd = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			String dateStr;
			dateStr = "";
			try{
				if (dd != null && homeSearch.getFromDate() != null)
					dateStr = dd.format(homeSearch.getFromDate());
			} catch (Exception e){
				LOG.error("Exception in home Search Basic Document - From Date Conversion Error");
				throw new SystemApplicationException(13101,e.getMessage(),e);
			} 
			try{
				if (dd != null && homeSearch.getToDate() != null)
					dateStr = dd.format(homeSearch.getToDate());
			} catch (Exception e){
				LOG.error("Exception in home Search Basic  Document - To Date Conversion Error");
				throw new SystemApplicationException(13101,e.getMessage(),e);

			}
			MapSqlParameterSource params = new MapSqlParameterSource();
			params.addValue("userid", userInfo.getuserprofileid());// TO DO add userInfo.getuserprofileid() in place of hard code value
			params.addValue("HPPO", BDEUtils.handleNullAndQuote(homeSearch.getS4OrderNumber()));
			params.addValue("B2BiTrackingNum", BDEUtils.handleNullAndQuote(homeSearch.getB2BiTracking()));
			params.addValue("customer_ponbr", BDEUtils.handleNullAndQuote(homeSearch.getPoNumber()));
			params.addValue("AcctName", BDEUtils.handleNullAndQuote(homeSearch.getAccountName()));
			params.addValue("AcctID", BDEUtils.handleNullAndQuote(homeSearch.getAccountID()));
			params.addValue("Status", BDEUtils.handleNullAndQuote(homeSearch.getDocStatus()));
			params.addValue("fromDate", homeSearch.getFromDate());
			
			params.addValue("toDate", homeSearch.getToDate());
			params.addValue("startIndex", homeSearch.getStartGroupNumber());
			params.addValue("endIndex", recordCount);
			params.addValue("tenantListAsString", "'HPE','HPQ'");
			
			List<HomeSearchDetails> basicSearch = homeSearchDao.getBasicSearch(params, "HOME_SEARCH.BASIC_SEARCH");
			homeSearch.setPoDocument(basicSearch);
			homeSearch.setDocumentType("PO"); //INC4690273 simple search Export to excel issue
		}
		}LOG.debug(" Result for Basic Search ..");
		}catch(Exception e)
		{
			LOG.error("Exception in Basic Search- Home document search page");
			throw new SystemApplicationException(13101,e.getMessage(),e);
		}
	return homeSearch;
}

		// TODO Auto-generated method stub
	private HomeSearchDetails searchPoDocument(HomeSearchDetails homeDetails,UserProfile userInfo) {
		LOG.debug(" Inside of searchPoDocument()  ..");
		try {
				HashMap documentListCountAndTotal = homeSearchDao.getPoDocuments(userInfo,homeDetails);
				LOG.debug(" Record count for Document Type : [PO]  ..");
		if(documentListCountAndTotal!=null && documentListCountAndTotal.size()>0)
		{
			int count=((Integer)documentListCountAndTotal.get("documentCount")).intValue();
			homeDetails.setTotalRecords(count);
			if(documentListCountAndTotal.get("documentTotal")!=null){
				homeDetails.setTotalSumDisplay("Total (USD) "+BDEUtils.formatNumber(Double.parseDouble((String)documentListCountAndTotal.get("documentTotal")), "#.##"));
			}else{
				homeDetails.setTotalSumDisplay("Total (USD) 0");
			}
			LOG.debug(" Searching Result for Document Type : [PO]  ..");
			if(homeDetails.getTotalRecords()>0)
				{
				java.text.SimpleDateFormat dd = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				String dateStr;
				dateStr = "";
				try{
					if (dd != null && homeDetails.getFromDate() != null)
						dateStr = dd.format(homeDetails.getFromDate());
				} catch (Exception e){
					LOG.error("Exception in home Search PO  Document - From Date Conversion Error");
					throw new SystemApplicationException(13201,e.getMessage(),e);
				} 
				try{
					if (dd != null && homeDetails.getToDate() != null)
						dateStr = dd.format(homeDetails.getToDate());
				} catch (Exception e){
					LOG.error("Exception in home Search PO  Document - To Date Conversion Error");
					throw new SystemApplicationException(13201,e.getMessage(),e);

				}
				MapSqlParameterSource params = new MapSqlParameterSource();
				params.addValue("userid", userInfo.getuserprofileid());// TO DO add userInfo.getuserprofileid() in place of hard code value
				params.addValue("View",homeDetails.getAccountStatus());
				params.addValue("FromDate", homeDetails.getFromDate());
				params.addValue("ToDate", homeDetails.getToDate());
				params.addValue("Status", BDEUtils.handleNullAndQuote(homeDetails.getDocStatus()));
				params.addValue("Action", BDEUtils.handleNullAndQuote(homeDetails.getDocAction()));
				params.addValue("HPPO", BDEUtils.handleNullAndQuote(homeDetails.getS4OrderNumber()));
				params.addValue("Region", BDEUtils.handleNullAndQuote(homeDetails.getRegion()));
				params.addValue("NcrfCBN", BDEUtils.handleNullAndQuote(homeDetails.getNcrfID()));
				params.addValue("B2BiTrackingNum", BDEUtils.handleNullAndQuote(homeDetails.getB2BiTracking()));
				params.addValue("SenderId", BDEUtils.handleNullAndQuote(homeDetails.getSenderID()));
				params.addValue("AcctName", BDEUtils.handleNullAndQuote(homeDetails.getAccountName()));
				params.addValue("AcctID", BDEUtils.handleNullAndQuote(homeDetails.getAccountID()));
				params.addValue("customer_ponbr", BDEUtils.handleNullAndQuote(homeDetails.getPoNumber()));
				params.addValue("OrderSourceId", BDEUtils.handleNullAndQuote(homeDetails.getOrderSource()));
				params.addValue("OrderSystemId", BDEUtils.handleNullAndQuote(homeDetails.getOrderDestination()));
				params.addValue("ReportStatus", BDEUtils.handleNullAndQuote(homeDetails.getReportStatus()));
				params.addValue("sort", homeDetails.getSort());
				params.addValue("sortInd", homeDetails.getSortInd());
				params.addValue("startIndex", homeDetails.getStartGroupNumber());
				params.addValue("endIndex", count);
				params.addValue("tenant", "");
				params.addValue("verifiedTenant", "");
				params.addValue("userTenantID", "HPE");
				params.addValue("soldToPartyId", homeDetails.getSoldToPartyId());
				//US-18376 : catalogFlag value from homeDetails in the search
				params.addValue("CATALOG_FLAG", null);
				//params.addValue("catalogFlag", homeDetails.getCatalogFlag());

					List<HomeSearchDetails> homePoDocument = homeSearchDao.getPoDocumentList(params, "HOME_SEARCH.SEARCH_PO_DOCUMENT");
				homeDetails.setPoDocument(homePoDocument);

				}
		}
		LOG.debug(" Result for Document Type : [PO]  ..");
		}catch (Exception e)
		{
			LOG.error("Exception in home Search PO Document - Home document search page");
			throw new SystemApplicationException(13201,e.getMessage(),e);

		}
		
		return homeDetails;
	}

	private HomeSearchDetails  searchRoundTrip(HomeSearchDetails homeDetails,UserProfile userInfo) {
		// TODO Auto-generated method stub
		//Date toDate = homeDetails.getToDate();
		//Date time1 = new Date(System.currentTimeMillis());
		LOG.debug(" Inside the searchRoundTrip()  ..");
		try {
		HashMap documentListCountAndTotal= homeSearchDao.getRoundTripCount(homeDetails,userInfo);
		LOG.debug(" Record Count for Document Type : [RTRIP]  ..");
		if(documentListCountAndTotal!=null && documentListCountAndTotal.size()>0)
		{
			int count=((Integer)documentListCountAndTotal.get("documentCount")).intValue();
			homeDetails.setTotalRecords(count);
			LOG.debug("Searching Result for Document Type : [RTRIP]  ..");
		if(homeDetails.getTotalRecords()>0)
		{
			java.text.SimpleDateFormat dd = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			String dateStr;
			dateStr = "";
			try{
				if (dd != null && homeDetails.getFromDate() != null)
					dateStr = dd.format(homeDetails.getFromDate());
			} catch (Exception e){
				LOG.error("Exception in home Search Round Trip  Document - From Date Conversion Error");
				throw new SystemApplicationException(13301,e.getMessage(),e);
			} 
			try{
				if (dd != null && homeDetails.getToDate() != null)
					dateStr = dd.format(homeDetails.getToDate());
			} catch (Exception e){
				LOG.error("Exception in home Search PO  Document - To Date Conversion Error");
				throw new SystemApplicationException(13301,e.getMessage(),e);

			}
			MapSqlParameterSource params = new MapSqlParameterSource();
			params.addValue("userid", userInfo.getuserprofileid()); // TO DO add userInfo.getuserprofileid() in place of hard code value
			params.addValue("View",homeDetails.getAccountStatus());
			params.addValue("FromDate", homeDetails.getFromDate());
			params.addValue("ToDate", homeDetails.getToDate());
			params.addValue("SenderId", BDEUtils.handleNullAndQuote(homeDetails.getSenderID()));
			params.addValue("AcctName", BDEUtils.handleNullAndQuote(homeDetails.getAccountName()));
			params.addValue("AcctID", BDEUtils.handleNullAndQuote(homeDetails.getAccountID()));
			params.addValue("cart", homeDetails.getCartReturn());
			params.addValue("sort", homeDetails.getSort());
			params.addValue("sortInd", homeDetails.getSortInd());
			params.addValue("startIndex", homeDetails.getStartGroupNumber());
			params.addValue("endIndex", count);
			params.addValue("tenant", "");
			params.addValue("userTenantID", "HPE");


			List<HomeSearchDetails> homeRoundTripDocument = homeSearchDao.getRoundTripDocumentList(params, "HOME_SEARCH.SEARCH_ROUND_TRIP_DOCUMENT");
			homeDetails.setRoundtrip(homeRoundTripDocument);
		}
		}LOG.debug(" Result for Document Type : [RTRIP]  ..");
		}catch (Exception e)
		{
			LOG.error("Exception in home Search Round Trip  Document - Home document search page");
			throw new SystemApplicationException(13301,e.getMessage(),e);

		}
		return homeDetails;
	}

	private HomeSearchDetails searchErrored(HomeSearchDetails homeDetails,UserProfile userInfo) {
		// TODO Auto-generated method stub
		LOG.debug(" Inside the searchErrored()  ..");
		try {
		HashMap documentListCountAndTotal= homeSearchDao.getErroedDocumentList(homeDetails,userInfo);
		LOG.debug(" Record Count for Document Type : [UNORTH]  ..");
		if(documentListCountAndTotal!=null && documentListCountAndTotal.size()>0)
		{
			int count=((Integer)documentListCountAndTotal.get("documentCount")).intValue();
			homeDetails.setTotalRecords(count);
			LOG.debug(" Searching Result for Document Type : [UNORTH]  ..");
			if(homeDetails.getTotalRecords()>0)
			{
				java.text.SimpleDateFormat dd = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				String dateStr;
				dateStr = "";
				try{
					if (dd != null && homeDetails.getFromDate() != null)
						dateStr = dd.format(homeDetails.getFromDate());
				} catch (Exception e){
					LOG.error("Exception in home Search Errored  Document - From Date Conversion Error");
					throw new SystemApplicationException(13401,e.getMessage(),e);
				} 
				try{
					if (dd != null && homeDetails.getToDate() != null)
						dateStr = dd.format(homeDetails.getToDate());
				} catch (Exception e){
					LOG.error("Exception in home Search Errored  Document - To Date Conversion Error");
					throw new SystemApplicationException(13401,e.getMessage(),e);

				}
				MapSqlParameterSource params = new MapSqlParameterSource();
				params.addValue("userid", userInfo.getuserprofileid()); // TO DO add userInfo.getuserprofileid() in place of hard code value
				
				params.addValue("FromDate", homeDetails.getFromDate());
				params.addValue("ToDate", homeDetails.getToDate());
				params.addValue("poNumber", BDEUtils.handleNullAndQuote(homeDetails.getPoNumber()));
				params.addValue("senderid", BDEUtils.handleNullAndQuote(homeDetails.getSenderID()));
				
				params.addValue("accountid", BDEUtils.handleNullAndQuote(homeDetails.getAccountID()));
				
				params.addValue("sort", homeDetails.getSort());
				params.addValue("sortInd", homeDetails.getSortInd());
				params.addValue("startIndex", homeDetails.getStartGroupNumber());
				params.addValue("endIndex", count );
				
				
				List<HomeSearchDetails> homeErroredDocument = homeSearchDao.getErroredDocumentList(params, "HOME_SEARCH.SEARCH_ERRORED_DOCUMENT");
				homeDetails.setErroredDocument(homeErroredDocument);
			}
			}LOG.debug(" Result for Document Type : [UNORTH]  ..");	
		}catch (Exception e)
		{
			LOG.error("Exception in home Search Errored  Document - Home document search page");
			throw new SystemApplicationException(13401,e.getMessage(),e);
		}
		return homeDetails;
	}
	
	@Override
	public ByteArrayInputStream exportExcelDocument() {
    
	    	// US-18376: Added "Modified" column to Excel export headers
	    	final String[] PO_DOC_HEADERs = { "Date Created", "Account Name", "OCL Account ID",
	   			 	"Document", "Message Id", "Document Status", "Sold To Party Id", "Original Sold To Party Id", "Total Price", "Total (USD)", "Modified" };

	    	final String[] RTRIP_DOC_HEADERs = { "Date", "Account Name", " OCL Account ID", "Roundtrip Store",
	    			"Punchout Request", "Cart Returned" };
	   	
	    	final String[] ERR_DOC_HEADERs = { "Date", "Document Received", "Sender/Organisation ID", "Document Type", "PO Number", 
	    			"Error Message", "Detailed Error Message", "OCL Account ID" };

			final String SHEET = "DocumentList";

			Workbook workbook = new XSSFWorkbook(); 
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			Sheet sheet = workbook.createSheet(SHEET);
			sheet.setDisplayGridlines(false);

			CellStyle style = workbook.createCellStyle();//Create style
			Font font = workbook.createFont();//Create font
			font.setBold(true);//Make font bold

			style.setFont(font);//set it to bold

			// US-18376: Create center-aligned style for Modified column
			CellStyle centerAlignedStyle = workbook.createCellStyle();
			centerAlignedStyle.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER);


			try {
			
				if(homeSearchDetails != null) {
					String docType = homeSearchDetails.getDocumentType();
					if(docType!=null && docType.equalsIgnoreCase("PO")) {
						LOG.debug("Preparing Excel data for Document type : [ PO ]");
						Row poHeaderAttribute = sheet.createRow(0);//Create first row
						poHeaderAttribute.createCell(0).setCellValue("Total Documents returned: "+homeSearchDetails.getTotalRecords());
						poHeaderAttribute.getCell(0).setCellStyle(style);//Set the style(bold)
						poHeaderAttribute = sheet.createRow(1);//Create second row
						poHeaderAttribute.createCell(0).setCellValue("Total (USD): "+homeSearchDetails.getTotalSumDisplay());
						poHeaderAttribute.getCell(0).setCellStyle(style);//Set the style(bold)
	    
						// Grid Headers
						Row poHeaderRow = sheet.createRow(3);// Skip one row and create 3rd row
						
						for (int col = 0; col < PO_DOC_HEADERs.length; col++) {
							Cell cell = poHeaderRow.createCell(col);
							cell.setCellValue(PO_DOC_HEADERs[col]);
							poHeaderRow.getCell(col).setCellStyle(style);//Set the style(bold)
						}
	    
						int rowIdx = 4;
	    
	    				for (HomeSearchDetails homeSearchDoc : checkDocumentListType()) {
	    			
	    					Row poDataRow = sheet.createRow(rowIdx++);
	    					poDataRow.setHeightInPoints(29.0f);
	    	
	    					poDataRow.createCell(0).setCellValue(BDEUtils.handleNullAndQuote(homeSearchDoc.getCreateDate()));
	    					poDataRow.createCell(1).setCellValue(BDEUtils.handleNullAndQuote(homeSearchDoc.getAccountName()));
	    					poDataRow.createCell(2).setCellValue(BDEUtils.handleNullAndQuote(homeSearchDoc.getNcrfID()));
	    					poDataRow.createCell(3).setCellValue(BDEUtils.handleNullAndQuote(homeSearchDoc.getDocReceived()));
	    					poDataRow.createCell(4).setCellValue(BDEUtils.handleNullAndQuote(homeSearchDoc.getB2BiTracking()));
	    					poDataRow.createCell(5).setCellValue(BDEUtils.handleNullAndQuote(homeSearchDoc.getDocStatus()));
	    					poDataRow.createCell(6).setCellValue(BDEUtils.handleNullAndQuote(homeSearchDoc.getSoldToPartyId()));
	    					poDataRow.createCell(7).setCellValue(BDEUtils.handleNullAndQuote(homeSearchDoc.getOriginalSoldToPartyId()));
	    					poDataRow.createCell(8).setCellValue(BDEUtils.handleNullAndQuote(homeSearchDoc.getTotalPricestr()));
	    					// US-18042 -OCL_SearchResultToNumber - start
	    					// Added to make String field to Number so that user can perform calculations on this field
	    					if(!BDEUtils.isEmptyOrNull(homeSearchDoc.getTotalUsd())) {
	    						poDataRow.createCell(9).setCellValue(new BigDecimal(homeSearchDoc.getTotalUsd()).doubleValue());
	    					} 
	    					// US-18042 -OCL_SearchResultToNumber - end

	    					// US-18376: Add catalogFlag (Modified) field to Excel export with center alignment
	    					Cell modifiedCell = poDataRow.createCell(10);
	    					modifiedCell.setCellValue(BDEUtils.handleNullAndQuote(homeSearchDoc.getCatalogFlag()));
	    					modifiedCell.setCellStyle(centerAlignedStyle);

	    				}
	    				sheet.autoSizeColumn(0);
	    				sheet.autoSizeColumn(1);
	    				sheet.autoSizeColumn(2);
	    				sheet.autoSizeColumn(3);
	    				sheet.autoSizeColumn(4);
	    				sheet.autoSizeColumn(5);
	    				sheet.autoSizeColumn(6);
	    				sheet.autoSizeColumn(7);
	    				sheet.autoSizeColumn(8);
	    				sheet.autoSizeColumn(9);
	    				// US-18376: Set fixed width for "Modified" column for better spacing
	    				sheet.setColumnWidth(10, 3000); // Approximately 15 characters width for proper spacing
	    				LOG.debug("Excel data READY for Document type : [ PO ]");
					} else if (docType !=null && docType.equalsIgnoreCase("RTRIP")) {
					
						LOG.debug("Preparing Excel data for Document type : [ RTRIP ]");
						
						Row rtRipHeaderRow = sheet.createRow(0);//Create first row of RTRIP doc
					
						for (int col = 0; col < RTRIP_DOC_HEADERs.length; col++) {
							Cell cell = rtRipHeaderRow.createCell(col);
							cell.setCellValue(RTRIP_DOC_HEADERs[col]);
							rtRipHeaderRow.getCell(col).setCellStyle(style);//Set the style(bold)
						}
						int rowIdx = 1;
						for (HomeSearchDetails homeSearchDoc : checkDocumentListType()) {
						
							Row rtRiptDataRow = sheet.createRow(rowIdx++); // Create rows for RTRIP data
							rtRiptDataRow.setHeightInPoints(29.0f);
							
							rtRiptDataRow.createCell(0).setCellValue(BDEUtils.handleNullAndQuote(homeSearchDoc.getCreateDate()));
							rtRiptDataRow.createCell(1).setCellValue(BDEUtils.handleNullAndQuote(homeSearchDoc.getAccountName()));
							rtRiptDataRow.createCell(2).setCellValue(BDEUtils.handleNullAndQuote(homeSearchDoc.getAccountID()));
							rtRiptDataRow.createCell(3).setCellValue(BDEUtils.handleNullAndQuote(homeSearchDoc.getRoudtripStore()));
							rtRiptDataRow.createCell(4).setCellValue(BDEUtils.handleNullAndQuote(homeSearchDoc.getPunchoutRequest()));
							rtRiptDataRow.createCell(5).setCellValue(BDEUtils.handleNullAndQuote(homeSearchDoc.getCartReturn()));
						
						}
						sheet.autoSizeColumn(0);
	    				sheet.autoSizeColumn(1);
	    				sheet.autoSizeColumn(2);
	    				sheet.autoSizeColumn(3);
	    				sheet.autoSizeColumn(4);
	    				sheet.autoSizeColumn(5);
	    				LOG.debug("Excel data READY for Document type : [ RTRIP ]");
					} else {
						LOG.debug("Preparing Excel data for Document type : [ UNORTH ]");
						Row errHeaderRow = sheet.createRow(0);//Create first row of UNORTH doc
					
						for (int col = 0; col < ERR_DOC_HEADERs.length; col++) {
							Cell cell = errHeaderRow.createCell(col);
	    					cell.setCellValue(ERR_DOC_HEADERs[col]);
	    					errHeaderRow.getCell(col).setCellStyle(style);//Set the style(bold)
						}
						int rowIdx = 1;
						for (HomeSearchDetails homeSearchDoc : checkDocumentListType()) {
						
							Row errDataRow = sheet.createRow(rowIdx++); // Create rows for RTRIP data
							errDataRow.setHeightInPoints(29.0f);
							
							errDataRow.createCell(0).setCellValue(BDEUtils.handleNullAndQuote(homeSearchDoc.getCreateDate()));
							errDataRow.createCell(1).setCellValue(BDEUtils.handleNullAndQuote(homeSearchDoc.getDocReceived()));
							errDataRow.createCell(2).setCellValue(BDEUtils.handleNullAndQuote(homeSearchDoc.getBuyerSenderId()));
							errDataRow.createCell(3).setCellValue(BDEUtils.handleNullAndQuote(homeSearchDoc.getDocumentType()));
							errDataRow.createCell(4).setCellValue(BDEUtils.handleNullAndQuote(homeSearchDoc.getPoNumber()));
							errDataRow.createCell(5).setCellValue(BDEUtils.handleNullAndQuote(homeSearchDoc.getErrorMessage()));
							errDataRow.createCell(6).setCellValue(BDEUtils.handleNullAndQuote(homeSearchDoc.getDetailedErrorMessage()));
							errDataRow.createCell(7).setCellValue(BDEUtils.handleNullAndQuote(homeSearchDoc.getCustomerID()));
						
						}
					}
					sheet.autoSizeColumn(0);
    				sheet.autoSizeColumn(1);
    				sheet.autoSizeColumn(2);
    				sheet.autoSizeColumn(3);
    				sheet.autoSizeColumn(4);
    				sheet.autoSizeColumn(5);
    				sheet.autoSizeColumn(6);
    				sheet.autoSizeColumn(7);
					LOG.debug("Excel data READY for Document type : [ UNORTH ]");
				}
	    		workbook.write(out);
	    		workbook.close();
	    		LOG.debug("Excel file write completed");
	    } catch(Exception e) {
	    	LOG.debug("EXCEPTION in exportExcel function of Home Search page", e);
	    	throw new SystemApplicationException(AdminUIConstants.ErrorCodes.HOMESEARCH_EXPORT_XLS_ERR_CD, 
	    				AdminUIConstants.ErrorMessages.HOMESEARCH_EXPORT_XLS_FAILURE, e);
	    }
	    
	    return new ByteArrayInputStream(out.toByteArray());
	}
	
	@Override
	public String sendDocumentListMail() {
		
		LOG.debug("Inside sendDocumentListMail() service..");
		UserProfile userInfo = CommonUtil.getCurrentUserProfile();
		LOG.debug("Obtained user profile data from session: ",userInfo);
		Context ctx = prepareMailBody(userInfo);
		Email email =  prepareEmailData(userInfo);
		LOG.debug("Email data prepared, calling config method to mail fill template: ", email);
		try {
			sendEmail.emailConfiguration(ctx, getEmailDocumentTemplate(), email);
		} catch (Exception e) {
			LOG.error("Exception while trying to send email - Home document search page");
			throw new SystemApplicationException(AdminUIConstants.ErrorCodes.HOMESEARCH_EMAIL_ERR_CD,
						AdminUIConstants.ErrorMessages.HOMESEARCH_SEND_MAIL_FAILURE, e);
		}
		return AdminUIConstants.SuccessMessages.HOMESEARCH_SEND_EMAIL_SUCCESS;
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
	 * US-18451: Adds an Excel attachment to the given Email object.
	 * Sets the attachment name, content, and MIME type.
	 *
	 * @param email       The Email object to which the attachment is added.
	 * @param fileName    The name of the attachment file.
	 * @param fileContent The content of the attachment as a byte array.
	 * @param contentType The MIME type of the attachment.
	 */
	private void addAttachment(
			Email email,
			String fileName,
			byte[] fileContent,
			String contentType) {

		email.setAttachmentName(fileName);
		email.setAttachmentContent(fileContent);
		email.setAttachmentType(contentType);
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
		LOG.debug("DAILY errored document mail sent successfully");
	}


	private Context prepareMailBody(UserProfile userInfo) {
		
		final Context ctx = new Context(); 
		List<HomeSearchDetails> homeSearchDocsList;
		
		try {
			
			if (homeSearchDetails != null) {
				homeSearchDocsList=checkDocumentListType();
				ctx.setVariable("documentList", homeSearchDocsList);
			}
			
		} catch (Exception e) {
			LOG.error("Exception in home Search prepareMailBody - Home document search page");
			throw new SystemApplicationException(13501,e.getMessage(),e);
		} return ctx;
	}
	
	private Email prepareEmailData(UserProfile userInfo) {
		
		Email email = new Email();
		email.setRecipients(new String[] {userInfo.getusermailid()});
		email.setFrom(emailProps.getProperty("fromAddress"));
		email.setSubject(getEmailSubject(homeSearchDetails));
		
		return email;
	}
	
	private List<HomeSearchDetails> checkDocumentListType(){
		
		List<HomeSearchDetails> homeSearchDocsList = null;
		
		if(homeSearchDetails.getPoDocument() != null && homeSearchDetails.getPoDocument().size() > 0) {
			 homeSearchDocsList = homeSearchDetails.getPoDocument();
		} else if (homeSearchDetails.getRoundtrip() != null && homeSearchDetails.getRoundtrip().size() > 0) {
			 homeSearchDocsList = homeSearchDetails.getRoundtrip();
		
			 
				 for(HomeSearchDetails homeSearchDocs : homeSearchDocsList) {
					 if(!homeSearchDocs.getIsDescriptionLoaded()) {
						 homeSearchDocs.setPunchoutRequest(BDEUtils.isEqualsIgnoreCase(homeSearchDocs.getPunchoutRequest(), "1") ? "Document not exists":"Document exists");
						 homeSearchDocs.setCartReturn(BDEUtils.isEqualsIgnoreCase(homeSearchDocs.getCartReturn(), "1") ? "No Shopping Cart Returned":"Shopping Cart Returned");
					 }homeSearchDocs.setIsDescriptionLoaded(true);
				 }
			 
			 
		} else {
			 homeSearchDocsList = homeSearchDetails.getErroredDocument();
		}
		// US-18451- Return empty list if no documents found
		if (homeSearchDocsList == null) {
			return Collections.emptyList();
		}
		return homeSearchDocsList;
	}
	
	
	private String getEmailSubject(HomeSearchDetails homeSearchDetails){
		
		String emailSubject;
		
		if(homeSearchDetails.getPoDocument() != null && homeSearchDetails.getPoDocument().size() > 0) {
			emailSubject = emailProps.getProperty("poDocumentListSubject");
		} else if (homeSearchDetails.getRoundtrip() != null && homeSearchDetails.getRoundtrip().size() > 0) {
			emailSubject = emailProps.getProperty("rtrpDocmentListSubject");
		} else {
			emailSubject = emailProps.getProperty("errordDocumentListSubject");
		} return emailSubject;
	}
	
	private String getEmailDocumentTemplate(){
		
		String docType;
		
		if(homeSearchDetails.getPoDocument() != null && homeSearchDetails.getPoDocument().size() > 0) {
			docType = "PODocumentEmail";
		} else if (homeSearchDetails.getRoundtrip() != null && homeSearchDetails.getRoundtrip().size() > 0) {
			docType = "RTRIPDocumentEmail";
		} else {
			docType = "UNORTHDocumentEmail";
		} return docType;
	}
	

	@Override
	public QuickSearchDetails addquickSearch(QuickSearchDetails homeSearch) {
		// TODO Auto-generated method stub
		LOG.debug("Inside the addquickSearch()");
		UserProfile userInfo = CommonUtil.getCurrentUserProfile();
		//TODO: comment it later - HUSSAIN
		LOG.debug("Obtained user profile data from session: ",userInfo);
		//userInfo.setuserprofileid(37412);

		if(BDEUtils.isEqualsIgnoreCase(homeSearch.getAction() ,"MORESEARCH"))
		{
			if (BDEUtils.isEqualsIgnoreCase(homeSearch.getDocumentType(), "UNORTH")) {
				addquickSearch(homeSearch, userInfo);
			} else if (BDEUtils.isEqualsIgnoreCase(homeSearch.getDocumentType(), "RTRIP")) {
				addquickSearch(homeSearch, userInfo);
			} else {
				addquickSearch(homeSearch, userInfo);
			}
		} else if (BDEUtils.isEqualsIgnoreCase(homeSearch.getAction(), "SIMPLESEARCH")) {
			addquickSearch(homeSearch, userInfo);
		}
		return homeSearch;
	}

	private QuickSearchDetails addquickSearch(QuickSearchDetails homeSearch,UserProfile userInfo) {
		// TODO Auto-generated method stub
		LOG.debug("Inside the addquickSearch() ...");
		StatusContainer status = new StatusContainer();
		LoadHomeSearch search = new LoadHomeSearch();
		//System.out.print(homeSearch.getDocumentType()+ homeSearch.getAccountStatus());
				homeSearch.setAccountID(homeSearch.getAccountID());
				homeSearch.setDocumentType(homeSearch.getDocumentType());
		try {		
		String E = getStringJSONValue(homeSearch);
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("userId",userInfo.getuserprofileid());
		params.addValue("searchname", homeSearch.getSearchName());
		params.addValue("pageName", "DocumentSearch");
		homeSearchDao.delQuickSearch(params);
		params.addValue("flag", BDEUtils.handleNullAndQuote("0"));
		params.addValue("strxml", E);
		int count = homeSearchDao.addQuickSearch(params);
		if (count>0)
		{
			homeSearch.addStatusMessages(
					AdminUIConstants.StatusCode.INFO,
					"Quick Search "+ homeSearch.getSearchName() +" is added sucessfully.");
		}
		else
		{
			
			homeSearch.addStatusMessages(AdminUIConstants.StatusCode.INFO,
					AdminUIConstants.ErrorMessages.HOME_SEARCH_ADD_QUICK_SEARCH_FAILED);
		}
		String count1 = homeSearchDao.getQuickSearchId(params);
		homeSearch.setQuickSearchId(count1);
		quickSearchreBind(search);
		
		}
	catch (Exception e)
	{
		LOG.error("Exception in home Search Quick Search - Home document search page");
		throw new SystemApplicationException(13601,e.getMessage(),e);
	}
		return homeSearch;
	}

	
	private LoadHomeSearch quickSearchreBind(LoadHomeSearch loaddata) {
		// TODO Auto-generated method stub
		//HomeSearchDetails h = new HomeSearchDetails();
		UserProfile userInfo = CommonUtil.getCurrentUserProfile();
		//TODO: comment it later - HUSSAIN
		//userInfo.setuserprofileid(37412);
		 MapSqlParameterSource params = new MapSqlParameterSource();
		 params.addValue("userid",userInfo.getuserprofileid());
		 params.addValue("pageName", "DocumentSearch");
		 List<KeyValueProp> quickSearch = homeSearchDao.getQuickSearch(params,"HOME_SEARCH.QUICK_SEARCH");
		 ListIterator<KeyValueProp> it = quickSearch.listIterator();
		 while(it.hasNext())
		 {
			 KeyValueProp quicksearchTmplt = ( KeyValueProp) it.next();
			 it.set(quicksearchTmplt);

		 }
		 if (quickSearch != null)
			{
				loaddata.setQuicksearchList(quickSearch);
			}
		return loaddata ;
	}

	public static < E > String getStringJSONValue(E e) {
		String jsonMapper = null;
		if (e != null) {
		ObjectMapper mapper = new ObjectMapper();
		try {
		jsonMapper = mapper.writeValueAsString(e);
		} catch (JsonProcessingException e1) {
			LOG.error("Exception in home Search getStringJSONValue - Home document search page");
			throw new SystemApplicationException(13701,e1.getMessage(),e1);
		
		}
		}
		return jsonMapper;
		}

	@Override
	public QuickSearchDetails loadQuickSearch(QuickSearchDetails homeSearch) {
		// TODO Auto-generated method stub
		String json = null;
		if(!BDEUtils.isEmptyOrNull(homeSearch.getQuickSearchId())) {
		
			MapSqlParameterSource params = new MapSqlParameterSource();
			params.addValue("quickSearchId", homeSearch.getQuickSearchId());
			 json = homeSearchDao.getQuickSearchDetails(params);
			
			//HomeSearchDetails homeSearch1 = convertJsonToObject(json);
			
			
		}
		
		return convertJsonToObject(json) ;
	}
	
	private QuickSearchDetails convertJsonToObject(String json) {
		QuickSearchDetails beanObject = null;
		try {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true); //ADDED
		beanObject = mapper.readValue(json, QuickSearchDetails.class);
		} catch (Exception e) {
		//Logger.error(MataProcessorUtil.class,"Exception caught while in convertJsonToObject() ::: ",e);
			LOG.error("Exception in home Search convertJsonToObject - Home document search page");
			throw new SystemApplicationException(13801,e.getMessage(),e);
		}
		return beanObject;
		}

private static final AppLogger LOG = AppLoggerFactory.getLogger(HomeSearchServiceImpl.class, AppLoggingDomain.OCLUI);

    private static final int HOMESEARCH_EMAIL_ERR_CD = 18451;

    @Autowired
    private IHomeSearchDao homeSearchDao;

    @Autowired
    private SendEmail sendEmail;

    @Autowired
    private Properties emailProps;

    private HomeSearchDetails homeSearchDetails;

    /**
     * US-18451: Prepare HomeSearchDetails for daily job.
     * Sets documentType to "UNORTH" and date range to last 24 hours.
     * Constructs system user profile and calls existing searchErrored method.
     * @return HomeSearchDetails result of errored documents search.
     */
    private HomeSearchDetails prepareHomeSearchDetailsForDailyJob() {
        HomeSearchDetails details = new HomeSearchDetails();
        details.setDocumentType("UNORTH");

        Calendar cal = Calendar.getInstance();
        Date toDate = cal.getTime();
        cal.add(Calendar.DAY_OF_YEAR, -1);
        Date fromDate = cal.getTime();

        details.setFromDate(fromDate);
        details.setToDate(toDate);

        UserProfile systemUser = new UserProfile();
        systemUser.setUserprofileid(0);
        systemUser.setUsermailid("system@hpe.com");

        return searchErrored(details, systemUser);
    }

    /**
     * US-18451: Add attachment details to Email.
     * @param email Email object to attach to.
     * @param fileName Name of the attachment file.
     * @param fileContent Content bytes of the attachment.
     * @param contentType MIME type of the attachment.
     */
    private void addAttachment(Email email, String fileName, byte[] fileContent, String contentType) {
        email.setAttachmentName(fileName);
        email.setAttachmentContent(fileContent);
        email.setAttachmentType(contentType);
    }

    /**
     * US-18451: Prepare Email object for daily errored document report.
     * Fetches recipients list from DB, sets from address and subject.
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
     * US-18451: Public method to send daily errored document list mail.
     * Prepares search details, generates Excel report, prepares email with attachment and sends it.
     * Throws SystemApplicationException on failure.
     */
    public void sendDailyDocumentListMail() {
        LOG.info("US-18451: Starting daily errored document report email job");
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

        try {
            sendEmail.sendMailWithAttachment(email);
            LOG.info("US-18451: Daily errored document report email sent successfully");
        } catch (Exception e) {
            LOG.error("US-18451: Error sending daily errored document report email", e);
            throw new SystemApplicationException(HOMESEARCH_EMAIL_ERR_CD, "Failed to send daily errored document report email", e);
        }
    }

    /**
     * US-18451: Override checkDocumentListType to return empty list if null.
     * @return List of documents or empty list if none.
     */
    @Override
    public List<?> checkDocumentListType() {
        List<?> docList = super.checkDocumentListType();
        if (docList == null) {
            return Collections.emptyList();
        }
        return docList;
    }

    // Existing methods and dependencies below...
    private HomeSearchDetails searchErrored(HomeSearchDetails details, UserProfile user) {
        // existing implementation
        return null;
    }

    private void exportExcelDocument(HomeSearchDetails details, ByteArrayOutputStream outputStream) throws IOException {
        // existing implementation
    }
}

