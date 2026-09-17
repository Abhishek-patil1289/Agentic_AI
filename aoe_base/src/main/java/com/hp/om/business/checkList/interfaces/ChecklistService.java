package com.hp.om.business.checkList.interfaces;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.hp.common.validation.rule.FieldFieldRule;
import com.hp.common.validation.rule.FieldRule;
import com.hp.common.validation.rule.FieldRuleResponse;
import com.hp.common.validation.rule.FieldValidation;
import com.hp.common.validation.rule.PunchoutParam;
import com.hp.common.validation.rule.RulesFilter;
import com.hp.common.validation.rule.UiComponentMap;
import com.hp.om.beans.ApplyRuleResponse;
import com.hp.om.beans.CheckListItem;
import com.hp.om.beans.CountryList;
import com.hp.om.beans.CountryType;
import com.hp.om.beans.CustomerPoNrMapping;
import com.hp.om.beans.ErrorMessage;
import com.hp.om.beans.LocalizationFilter;
import com.hp.om.beans.ProfileHeaderFilter;
import com.hp.om.beans.QtEntryRuleResponse;
import com.hp.om.beans.Quote;
import com.hp.om.beans.QuoteKeyInterface;
import com.hp.om.beans.RegionCountry;
import com.hp.om.beans.RegionList;
import com.hp.om.beans.TypeList;
import com.hp.om.beans.validataion.FVOProcessValidationRequest;
import com.hp.om.beans.validataion.FVOProcessValidationRequestList;
import com.hp.om.integration.qidsdata.QidsFilter;
import com.hp.om.validataion.interfaces.IGenericDisplayResponse;
import com.hp.service.core.exception.ResponseExceptionList;

@Service
public interface ChecklistService {

	List<RegionList> regionList();

	List<CountryList> countryList(String regionCd);

	List<TypeList> typeList();
	
	List<CheckListItem> getAllCheckListItem(String type);
	
	List<CheckListItem> getValidCheckListItem(String regionCd, String countrytype, Integer checkListType);
			
	boolean saveCheckList(List<CheckListItem> validCheckListItems, RegionCountry rc);
	
    List<CheckListItem> getDataCheckList(String regionCd, String countryTyp, String processName) ;
	
	boolean deleteDataCheckItem(int regionCountryCkId );

	List<RegionCountry> addDataCheckItem(RegionCountry regionCountry);
	
	public boolean updateRegionCountry(RegionCountry regionCountry);
	
	CountryType getCountryTypByCode(String countryCode);
	
	String getValidateErrorByKey(String errorKey);
	
	List<ErrorMessage> getErrorMessages();

	public void applyRule(Quote quote, FieldRule fieldRule, ResponseExceptionList responseExceptionList);
	
	public List<FieldRule> getRules(RulesFilter rulesFilter);
	
	public List<FieldFieldRule> getFieldFieldRules(List<String> fldRlCdList,RulesFilter rulesFilter);
	
	public UiComponentMap convertToUiCompMap(List<FieldRuleResponse> fieldRuleResponseList);
	
	public UiComponentMap getEntryRuleQuote( Quote quote, RulesFilter rulesFilter) ;
	
	public void applyRules(Quote quote, RulesFilter rulesFilter, ApplyRuleResponse applyRuleRespnse) ;

	public FVOProcessValidationRequestList getAllValidationsList( QidsFilter qidsFilter, RulesFilter rulesFilter);

	public IGenericDisplayResponse getNonZeroMeterialFrmLineItm(Quote quote, FieldRule fieldRule);

	public IGenericDisplayResponse getValidationSummaryDisplay(FVOProcessValidationRequest fvoValidationRequest, Quote quote);
	
	public IGenericDisplayResponse getItemsGroupByMCCDiscounts(Quote quote, FieldRule fieldRule) ;
	
	public IGenericDisplayResponse getItemsGroupByMCCDiscounts05(Quote quote, FieldRule fieldRule) ;
	
	public IGenericDisplayResponse getItemsExceededThresholdDiscount (Quote quote, FieldRule fieldRule) ;
	
	public IGenericDisplayResponse getDealQuoteEndCustomerAddress(Quote quote, FieldRule fieldRule);
	
	public IGenericDisplayResponse getSupplierCodesFrmLineItm(Quote quote, FieldRule fieldRule);
		
	public IGenericDisplayResponse getMCCCodesFrmLineItm(Quote quote, FieldRule fieldRule);
	
	public IGenericDisplayResponse checkForTradeInProducts (Quote quote, FieldRule fieldRule);

	public IGenericDisplayResponse findObsoleteProducts (Quote quote, FieldRule fieldRule) ;
	
	public void setDefaults4Rules(Quote quote, RulesFilter rulesFilter) ;
	
	//public void getUserPermission4Rules(RulesFilter rulesFilter) ;
	
	public String determineProcessingModel (PunchoutParam punchoutParam) ;
	
//	public List<String> getTaskRoleListFromProcessingModel(String processingModel) ;
	
	public List<String> getTaskRoleListFromProcessingModel(Quote quote) ;
	
	public Map<String, FieldValidation> getFieldValidationMap(LocalizationFilter filterbase);
	
	public boolean isWFMCaseNumberExists(String wfmCaseNum);
	
	public IGenericDisplayResponse isWFMCaseNumberExists(Quote quote, FieldRule fieldRule) ;
	
	public IGenericDisplayResponse getFusionSupplierCodesFrmLineItm(Quote quote, FieldRule fieldRule) ;
	
	public void loadEntryRuleResponseInQuote( Quote quote) ;
	
	public void getEntryRuleResponseFromQuote(Quote quote) ;
	
	public List<QtEntryRuleResponse> getEntryRuleResponseForConversion(QuoteKeyInterface quote);
	
	public int persistEntryRuleResponseForConversion(QuoteKeyInterface quote, QtEntryRuleResponse qtEntryRuleResponse);
	
	public List<QtEntryRuleResponse> getQuoteEntryRuleResponse(int batchCount);

	public IGenericDisplayResponse copyEndCustomerDataForValueIndirect(Quote quote, FieldRule fieldRule);
	
	public Quote getCrsIdForCustomerAddress(Quote quote);

	public IGenericDisplayResponse getDemoBuyoutReporting(Quote quote,	FieldRule fieldRule);

	public IGenericDisplayResponse getDemoBuyoutMCodeCheck(Quote quote, FieldRule fieldRule);

	public IGenericDisplayResponse isOverrideCustomerToData(Quote quote, FieldRule fieldRule);
	
	public Map<String, FieldValidation> getFieldValidationMapByPrfl(LocalizationFilter filterbase, Quote quote) ;
	
	public Map<String, FieldValidation> getFieldValidationMapByPrflHeader(LocalizationFilter filterbase, ProfileHeaderFilter profileHeader) ;


	public FVOProcessValidationRequestList getAllValidationsList(Quote quote, RulesFilter rulesFilter);

	public IGenericDisplayResponse getValidationSummaryByQuote(FVOProcessValidationRequest request, Quote quote);
	
	public List<CustomerPoNrMapping> getCstmrPoNrMappingFromDb(Quote quote);

	public IGenericDisplayResponse setFlagPoNrAlreadyExist(Quote quote, FieldRule fieldRule);

	public IGenericDisplayResponse checkForMultipleDeals(Quote quote, FieldRule fieldRule);

	public IGenericDisplayResponse setQuotePropertiesBasedonFinModel(Quote quote, FieldRule fieldRule);
	
	public IGenericDisplayResponse checkForInternalDemoInvalidSupplyingDiv (Quote quote, FieldRule fieldRule) ;
	
	public IGenericDisplayResponse checkForInternalDemoProductLine (Quote quote, FieldRule fieldRule) ;
	
	public IGenericDisplayResponse setQuotePropertiesBasedonInternalDemo (Quote quote, FieldRule fieldRule) ;
	
	public IGenericDisplayResponse setInternalDemoValues4ApplyCBN(Quote quote, FieldRule fieldRule);
	
	public IGenericDisplayResponse setContractStartAndEndDate(Quote quote, FieldRule fieldRule);
	
	public IGenericDisplayResponse findObsoleteProductsFromCorona(Quote quote, FieldRule fieldRule) ;

	public IGenericDisplayResponse setPostCBNSetters(Quote quote, FieldRule fieldRule);
	
	public IGenericDisplayResponse determinePOCategory(Quote quote, FieldRule fieldRule);
	
	public IGenericDisplayResponse setContextualQuoteDefaults(Quote quote, FieldRule fieldRule);

	IGenericDisplayResponse checkForEDIPoHeaderData(Quote quote, FieldRule fieldRule);

	IGenericDisplayResponse checkForCustomerCountryCode(Quote quote, FieldRule fieldRule);
	
	//public IGenericDisplayResponse setRequestedDeliveryDateForS4BisnessModel(Quote quote, FieldRule fieldRule);
	
	public IGenericDisplayResponse checkForPOCategoryMismatch (Quote quote, FieldRule fieldRule) ;

	public IGenericDisplayResponse checkLineItemSolution(Quote quote, FieldRule fieldRule);
	
	public IGenericDisplayResponse getPaIdList(Quote quote, FieldRule fieldRule);

	public IGenericDisplayResponse copySoldToToCSP(Quote quote, FieldRule fieldRule);

	//INC6484467 >> chinaGTM shipping condition validation check(US-18081)
	public IGenericDisplayResponse gtmValidationcheck(Quote quote, FieldRule fieldRule);
	
	public String getShipToCountry(Quote quote);

	//Us-18174 PO category
	public void validateEDIandEOPOrders(Quote quote , ApplyRuleResponse applyRuleRespnse);



	//US-18175 - code changes to  STOP EC & RESELLER validations
	public Set<String> getInclusivePartyIds();

	//US-18346 - code changes to validate deal version
	public void processAndValidateDealVersionNr(Quote quote, ApplyRuleResponse applyRuleResponse);
}