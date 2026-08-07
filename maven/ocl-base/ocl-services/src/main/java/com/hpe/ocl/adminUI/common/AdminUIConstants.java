package com.hpe.ocl.adminUI.common;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.RowMapper;

import com.hpe.common.beans.KeyValueProp;
import com.hpe.ocl.adminUI.account.beans.AccountDropDown;
import com.hpe.ocl.adminUI.account.beans.TableColumn;


public final class AdminUIConstants {
	
	public static class StatusCode{
		public static final String SUCCESS = "Success";
	
		public static final String ERROR = "Error";
	
		public static final String INFO = "Info";
	
		public static final String WARNING = "Warning";
	}
	
	public static class ToolsConstants{
		public static final String RT_DROP_DOWN = "RTDropDown";
		public static final String RT_STORE_LIST = "RTStoreList";
		public static final String ORG_PARAM_ADD_HIST = "Add new organization parameters.";
		public static final String ORG_PARAM_UPD_HIST = "Update organization parameters.";
		public static final String ORG_PARAM_DEL_HIST = "Delete organization parameters.";
		public static final String ASSIGN_PORTAL_SYSTEM="assignPoSys";
		public static final String ASSIGN_RT_SYSTEM = "assignRtSys";
		
		
		public static final String ROUND_TRIP_DATA_COUNTRY="country";
		public static final String ROUND_TRIP_DATA_CURRENCY="currency";
		public static final String ROUND_TRIP_DATA_LANGUAGE="language";
		public static final String ROUND_TRIP_DATA_RTINSTANCE="rtInstance";
		public static final String ROUND_TRIP_DATA_RTSTORE="rtStore";
		
		//public static final String UPDATE_TEMPLATE = "UpdateTemplate";
		public static final String TEMPLATE_ROLL_BACK = "rollback";
		public static final String SEARCH = "MORESEARCH";
		public static final String BASIC_SEARCH = "SIMPLESEARCH";
		public static final String ACCOUNT_ID = "Reseller";
		public static final String ACCOUNT_NAME = "Tools";
		public static final String ACTIVATE_FLAG = "Y";
		public static final String DEACTIVATE_FLAG = "N";
		public static final String ACTIVATE_RPSTATUS = "";
		public static final String DEACTIVATE_RPSTATUS = "TSM";

			
		
	}
	

	public static class AccountConstants{
		public static final String ACC_SELECT_ACCOUNT_TYPE = "Account Type not selected";
		public static final String ACC_SELECT_REGION = "Account region not selected";
		public static final String ACC_SELECT_ORD_SOURCE ="Select OrderSource Type.";
		public static final String ACC_SELECT_ORD_DEST ="Select OrderSytsem Type.";
		public static final String MANAGE_ACC_EDITORS ="Editors";
		public static final String MANAGE_ACC_VIEWERS ="Viewers";
		public static final String MANAGE_ACC_RESUBM ="Resubmitters";
		public static final String ADV_ROUT_ADD ="add";
		public static final String ADV_ROUT_DELETE ="del";
		public static final String ADV_ROUT_SAVE ="save";
		public static final String BDE_DEFAULT_SETUP ="BDEDefaultSetup";
		public static final String ACCOUNT_ID="AID";
		public static final String CUSTOMER_ID="CUSTOMERID";
		public static final String TEXT="text";
		public static final String RADIOBUTTON="radioButton";
		public static final String CHECKBOX="checkbox";
		public static final String LINK="link";
		public static final String UDF_HEADER="H";
		public static final String UDF_LINE="L";
		public static final int ORDER_SOURCE_EPRIME= 12;
		public static final int NCRF_SOURCE= 2;
		public static final int CREATE_NCRF_SOURCE= 1;
		public static final String TENANT ="HPE";
		
		// US-17847_OCL-AMS option to search - start
		public static final String CREATE = "CREATE";
		public static final String MANAGE = "MANAGE";
		public static final String AMS = "AMS";
		public static final String US = "US";
		public static final String CA = "CA";
		public static final String LA = "LA";
		
		// US-17847_OCL-AMS option to search - End
		
		// US 18002- Payer Party ID field - start
		public static final String SOLD_TO_PARTY_ID ="SoldToPartyId";
		public static final String PAYER_PARTY_ID ="PayerPartyId";
		// US 18002- Payer Party ID field - end
		
		//Start: Added as part of Spira ticket IN:753733 - Lead time and Long Text Flag in punch out setup for SAPXCBL flow
		public static final String COL_LEADTIME ="leadtime";
		public static final String COL_LONGTEXTFLAG ="longtextFlag";
		//End: Added as part of Spira ticket IN:753733 - Lead time and Long Text Flag in punch out setup for SAPXCBL flow
		
	}
	
	/*public static class DocumentConstants{
		public static final String CURRENCY ="USD";	
		public static final int USRPRFID =50684;
	}*/
	
	public static class ErrorCodes {
		
		// HomeSearchDetails page - exportExcel & send email codes
		public static final long HOMESEARCH_EXPORT_XLS_ERR_CD = 13500;
		public static final long HOMESEARCH_EMAIL_ERR_CD = 13501;
		
		// SearchAttachment & DownloadAttachment error codes
		public static final long SEARCH_ATTACHMENT_ERR_CD = 13600;
		public static final long ATTACHMENT_DWNLD_ERR_CD = 13601;
	}
	
	public static class ErrorMessages{
		public static final String RTDATA_INSERT_FAIL = "RT Data insertion Failed ::";
		public static final String RTDATA_STORE_MISSING = "RT Data Store Not Selected";
		public static final String RTDATA_SYS_MISSING = "RT Data System Not Selected";
		public static final String RTDATA_REGION_MISSING = "RT Data Region Not Selected";
		public static final String ORGPARAM_UPDATE_FAILED = "Organization Param Couldnot be updated";
		public static final String ORGPARAM_INSERT_FAILED = "Organization Param Could not be added";
		public static final String ORGPARAM_DELETE_FAILED ="Organization Param Could not be deleted";
		public static final String MANAGE_STORE_ASSIGN_PORTAL_FAILED = "Assign Portal System Not Updated";
		public static final String MANAGE_STORE_ASSIGN_PORTAL_INSERT_FAILED = "Assign Portal System Successfully inserted failed";
		public static final String MANAGE_STORE_ASSIGN_RT_FAILED = "Assign RT System Not Updated";
		public static final String MANAGE_STORE_ASSIGN_RT_INSERT_FAILED = "Assign Portal System Successfully inserted failed";
		
		public static final String MANAGE_STORE_UPDATE_INTERFACE_FAILED = "Update Interface Failed";
		
		public static final String ROUND_TRIP_DATA_COUNTRY_UPDATE_FAILED = "Country update failed";
		public static final String ROUND_TRIP_DATA_COUNTRY_DELETE_FAILED = "Country delete failed";
		public static final String ROUND_TRIP_DATA_CURRENCY_UPDATE_FAILED = "Currency update failed";
		public static final String ROUND_TRIP_DATA_CURRENCY_DELETE_FAILED = "Currency delete failed";
		public static final String ROUND_TRIP_DATA_LANGUAGE_UPDATE_FAILED = "Language update failed";
		public static final String ROUND_TRIP_DATA_LANGUAGE_DELETE_FAILED = "Language delete failed";
		public static final String ROUND_TRIP_DATA_RTINSTANCE_UPDATE_FAILED = "RT Instance update failed";
		public static final String ROUND_TRIP_DATA_RTINSTANCE_DELETE_FAILED = "RT Instance delete failed";
		public static final String ROUND_TRIP_DATA_RTSTORE_UPDATE_FAILED = "RT Store update failed";
		public static final String ROUND_TRIP_DATA_RTSTORE_DELETE_FAILED = "RT Store delete failed";
		
		public static final String ROUTING_RULE_DETAILS__UPDATE_FAILED = "Routing rule record update failed";
		public static final String ROUTING_RULE_DETAILS_DELETE_FAILED = "Routing rule record delete failed";
		
		
		public static final String MANAGE_ACC_DELETE_FAILED = "Account Deletion failed";
		
		public static final String MANAGE_PO_EMAIL_TEMPLATE_UPDATE_FAILED = "Template Updated Failed";
		public static final String MANAGE_PO_EMAIL_TEMPLATE_ROLLBACK_FAILED = "RollBack Failed";
		
		
		public static final String MANAGE_ACCOUNT_ADV_RULE_DELETE_FAILED = "Rule deletion Failed";
		public static final String MANAGE_ACCOUNT_ADV_RULE_ADD_FAILED = "Rule insertion Failed";
		public static final String MANAGE_ACCOUNT_ADV_RULE_UPDATE_FAILED = "Rule updation Failed";
		
		public static final String MANAGE_ACCOUNT_ADV_RULE_VALUE_UPDATE_FAILED = " row Id(s) updation Failed";
		public static final String HOME_SEARCH_ADD_QUICK_SEARCH_FAILED = "Quick Search Failed";
		public static final String MANAGE_ACCOUNT_ACC_STATUS_FAILED = "Account Status updation failed";
		
		public static final String HOMESEARCH_EXPORT_XLS_FAILURE = "Internal Error while generating excel";
		public static final String HOMESEARCH_SEND_MAIL_FAILURE = "Internal error while template-fill/sending email";
		
		public static final String FETCH_ATTACHMENT_DETAIL_FAILED = "Error while fetching attachment details";
		public static final String ZIP_ENTRY_FAILURE = "Error occured while creating zip entries";
		public static final String FILE_DOWNLOAD_FAILURE = "Error occured while fetching single file for download";
		
		public static final String BDE_HISTORY_INSERT_FAILED = "Record insert failed";
		public static final String CONFIGURATION_MESSAGE = "Invalid Configuration Id";
		public static final String ORDER_CANCELLATION_FAILED = "Order Cancellation Failed";
		public static final String ORDERCANCELLATION_DUPLICATE_MESSAGE = "Duplicate Message Id";
		public static final String RESELLER_ACTIVATED_FAILED = "Reseller activated failed";
		public static final String RESELLER_DEACTIVATED_FAILED = "Reseller deactivated failed";
		
		public static final String DATA_ADMIN_DETAILS_UPDATE_FAILED = "Data admin record update failed";
		public static final String DATA_ADMIN_DETAILS_DELETE_FAILED = "Data admin record insert failed";

		public static final String MANAGEGROUP_GROUP_EXISTS = "Group exists!";
		public static final String MANAGEGROUP_GROUP_UPDATE_FAILED = "Group updated failed";		
		
		public static final String SETUP_NON_EXTRINSIC_DELETE_FAILED = "Record delete failed";
		
	}
	
	public static class WarningMessages{
		public static final String RTDATA_INSERT_WARN = "RT Data already exists.";
		public static final String ORGPARAM_INSERT_WARNING = "Organization parameter already exists";
		
		public static final String ROUND_TRIP_DATA_COUNTRY_INSERT_WARNING = "Country insert warning";
		public static final String ROUND_TRIP_DATA_CURRENCY_INSERT_WARNING = "Currency insert warning";
		public static final String ROUND_TRIP_DATA_LANGUAGE_INSERT_WARNING = "Language insert warning";
		public static final String ROUND_TRIP_DATA_RTINSTANCE_INSERT_WARNING = "RT Instance insert warning";
		public static final String ROUND_TRIP_DATA_RTSTORE_INSERT_WARNING = "RT Store insert warning";
		
		public static final String ROUTING_RULE_DETAILS__INSERT_WARNING = "Routing rule insert warning";
		public static final String ROUTING_RULE_DUPLICATE = "Routing rule already exists";

		public static final String DATA_ADMIN_DETAILS_INSERT_WARNING = "Data admin insert warning";
		public static final String DATA_ADMIN_DUPLICATE = "Data Admin record already exists";
		
		public static final String SETUP_NON_EXTRINSIC_INSERT_WARNING = "Setup non extrinsic record insert warning";
		public static final String SET_UP_NON_EXTRINSIC_DUPLICATE = "Setup non extrinsic record already exists";
	}
	
	public static class InfoMessages{
		public static final String NO_RECORDS = "No records available.";
		public static final String TEMPLATE_UPDATE = "Template already exist";
		public static final String RESELLER_SEARCH_ORDER = "No reseller orders available for given search criteria";
		public static final String MANAGEGROUP_GROUP_EXISTS = "Group already exist";
		public static final String MANAGEGROUP_GROUP_DOESNOT_EXISTS = "Group doesn't exist";
		public static final String MANAGEGROUP_GROUP_ACCNT_NO_DATA = "No Group and its linked accounts available";
		public static final String MANAGEGROUP_ACCNT_UPDATE_TO_GROUP_FAILED = "Assigning accounts to group failed, some accounts already assigned to other group";

	}
	public static class SuccessMessages{
			public static final String RTDATA_INSERT_SUCCESS = "RT Data inserted successfully.";
			public static final String RTDATA_SUCCESS = " action successfully completed.";
			public static final String ORGPARAM_UPDATE_SUCCESS = "Organization parameter updated successfully";
			public static final String ORGPARAM_INSERT_SUCCESS = "Organization parameter added successfully";
			public static final String ORGPARAM_DELETE_SUCCESS = "Organization parameter deleted successfully";
			public static final String TEMPLATE_UPDATE_SUCCESS = "Template update sucessfully"; 
			public static final String MANAGE_STORE_ASSIGN_PORTAL_SYSTEM = "Assign Portal System Successfully";
			public static final String MANAGE_STORE_ASSIGN_PORTAL_SYSTEM_INSERT = "Assign Portal System Successfully inserted";
			public static final String MANAGE_STORE_ASSIGN_RT_SYSTEM = "Assign RT System Successfully";
			public static final String MANAGE_STORE_ASSIGN_RT_SYSTEM_INSERT = "Assign RT System Successfully inserted";
			public static final String MANAGE_STORE_UPDATE_INTERFACE = "Update Interface Successfully";
			
			public static final String ROUND_TRIP_DATA_COUNTRY_DELETE_SUCCESS = "Country deleted successfully";
			public static final String ROUND_TRIP_DATA_COUNTRY_INSERT_SUCCESS = "Country added successfully";
			public static final String ROUND_TRIP_DATA_COUNTRY_UPDATE_SUCCESS = "Country updated successfully";
			public static final String ROUND_TRIP_DATA_CURRENCY_UPDATE_SUCCESS = "Currency updated sucessfully";
			public static final String ROUND_TRIP_DATA_CURRENCY_INSERT_SUCCESS = "Currency added sucessfully";
			public static final String ROUND_TRIP_DATA_CURRENCY_DELETE_SUCCESS = "Currency deleted sucessfully";
			public static final String ROUND_TRIP_DATA_LANGUAGE_UPDATE_SUCCESS = "Language upda