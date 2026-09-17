package com.hp.om.business.checkList;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.validation.Valid;

import com.hp.om.integration.S4.S4DealNumberValidationService;
import com.hp.om.integration.detailchange.DetailChangeDAOImpl;
import com.hp.om.integration.detailchange.interfaces.DetailChangeDAO;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import com.google.common.base.Predicate;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.hp.bcs.common.Constants.NGQConstants;
import com.hp.bcs.utils.CacheConfig;
import com.hp.common.CommonUtil;
import com.hp.common.Constants;
import com.hp.common.Constants.FrictionLessValueOrder;
import com.hp.common.FieldValidationUtil;
import com.hp.common.RuleUtil;
import com.hp.common.beans.QuoteCharBitConstants;
import com.hp.common.validation.rule.FieldFieldRule;
import com.hp.common.validation.rule.FieldRule;
import com.hp.common.validation.rule.FieldRuleAction;
import com.hp.common.validation.rule.FieldRuleMap;
import com.hp.common.validation.rule.FieldRuleResponse;
import com.hp.common.validation.rule.FieldValidation;
import com.hp.common.validation.rule.FieldValidationDetail;
import com.hp.common.validation.rule.ModeMap;
import com.hp.common.validation.rule.ProductRulesValidator;
import com.hp.common.validation.rule.PunchoutParam;
import com.hp.common.validation.rule.RegexRulesValidator;
import com.hp.common.validation.rule.RulesFilter;
import com.hp.common.validation.rule.RulesValidator;
import com.hp.common.validation.rule.SpELEntryRequiredRuleValidator;
import com.hp.common.validation.rule.SpELRulesValidator;
import com.hp.common.validation.rule.UiComponentMap;
import com.hp.common.validation.rule.interfaces.IRulesCacheHandler;
import com.hp.common.validation.rule.interfaces.IRulesValidator;
import com.hp.dao.core.DAOFactory;
import com.hp.om.beans.ApplTypeType;
import com.hp.om.beans.ApplyRuleResponse;
import com.hp.om.beans.AssetType;
import com.hp.om.beans.CheckListItem;
import com.hp.om.beans.CountryList;
import com.hp.om.beans.CountryType;
import com.hp.om.beans.CustomerPoNrMapping;
import com.hp.om.beans.CustomerType;
import com.hp.om.beans.EntryRuleType;
import com.hp.om.beans.ErrorMessage;
import com.hp.om.beans.FinancialModel;
import com.hp.om.beans.FusionSupplierModel;
import com.hp.om.beans.InclusivePartyIdModel;
import com.hp.om.beans.InternalDemoModel;
import com.hp.om.beans.LocalDBQuoteFilter;
import com.hp.om.beans.LocalizationFilter;
import com.hp.om.beans.POStatusCodeType;
import com.hp.om.beans.ProcessProductCheck;
import com.hp.om.beans.ProductProcessActions;
import com.hp.om.beans.ProfileHeaderFilter;
import com.hp.om.beans.QtCmt;
import com.hp.om.beans.QtCmtType;
import com.hp.om.beans.QtEntryRuleResponse;
import com.hp.om.beans.QtFlag;
import com.hp.om.beans.QtFlagType;
import com.hp.om.beans.QtPrchOrdReqt;
import com.hp.om.beans.Quote;
import com.hp.om.beans.QuoteCustomer;
import com.hp.om.beans.QuoteCustomerAddress;
import com.hp.om.beans.QuoteItem;
import com.hp.om.beans.QuoteItemCarePack;
import com.hp.om.beans.QuoteItemMcc;
import com.hp.om.beans.QuoteKeyInterface;
import com.hp.om.beans.RegionCountry;
import com.hp.om.beans.RegionList;
import com.hp.om.beans.TypeList;
import com.hp.om.beans.ValidationResponseEnum;
import com.hp.om.beans.WrappedQuote;
import com.hp.om.beans.validataion.DiscountCheckResponse;
import com.hp.om.beans.validataion.DiscountCheckResponse.DiscountItemModel;
import com.hp.om.beans.validataion.DiscountCheckResponse.DiscountItemModel.MCCObject;
import com.hp.om.beans.validataion.FVOProcessResponse;
import com.hp.om.beans.validataion.FVOProcessValidationRequest;
import com.hp.om.beans.validataion.FVOProcessValidationRequestList;
import com.hp.om.beans.validataion.FVOReportResponse;
import com.hp.om.beans.validataion.ObsoleteLineItemsResponse;
import com.hp.om.beans.validataion.ProcessCheckResponse;
import com.hp.om.beans.validataion.TableProductResponse;
import com.hp.om.beans.validataion.TableProductResponse.ProductResponse;
import com.hp.om.business.checkList.interfaces.ChecklistService;
import com.hp.om.business.checkList.interfaces.IExecutionProcessor;
import com.hp.om.business.customer.search.mdcp.dto.CustSearchCreateV3Request;
import com.hp.om.business.customer.search.mdcp.interfaces.IMDMService;
import com.hp.om.business.detailchange.interfaces.DetailChangeService;
import com.hp.om.business.dropdownlist.interfaces.DropDownListService;
import com.hp.om.business.frictionless.interfaces.FrictionlessOrderService;
import com.hp.om.business.myworkspace.interfaces.MyWorkspaceService;
import com.hp.om.business.ordermgmt.FlomServices;
import com.hp.om.business.ordermgmt.QuoteCacheHandler;
import com.hp.om.business.ordermgmt.interfaces.IOrderMgmtServices;
import com.hp.om.business.prevalidation.process.interfaces.ProcessCheckService;
import com.hp.om.business.quote.interfaces.QidsQuoteService;
import com.hp.om.business.quote.interfaces.QuoteService;
import com.hp.om.integration.checkList.interfaces.CheckListRepository;
import com.hp.om.integration.myworkspace.MyWorkspaceFilter;
import com.hp.om.integration.qidsdata.QidsFilter;
import com.hp.om.service.application.ApplicationService;
import com.hp.om.service.customer.search.create.cxf.generated.CustomerSearchCreateV3Response;
import com.hp.om.usermgmt.services.beans.Permission;
import com.hp.om.validataion.interfaces.IGenericDisplayResponse;
import com.hp.omext.converttoorder.business.interfaces.IConvertToOrderGateway;
import com.hp.omext.converttoorder.business.model.ConvertToOrderRequestInternal;
import com.hp.omext.converttoorder.business.model.ConvertToOrderResponseInternal;
import com.hp.omext.converttoorder.business.strategy.DetermineCTOStrategyService;
import com.hp.omext.converttoorder.business.trustedpricing.interfaces.ITrustedPricingService;
import com.hp.omext.converttoorder.business.util.ConvertToOrderConstants;
import com.hp.omext.converttoorder.services.soap.generated.OrderRequestHeader;
import com.hp.omext.pricing.business.ezprs.interfaces.IEZPRSForObsoleteProducts;
import com.hp.omext.pricing.business.interfaces.QuoteItemPricingable;
import com.hp.omext.pricing.business.interfaces.QuotePricingable;
import com.hp.service.core.ApplicationDomainType;
import com.hp.service.core.ElkOrderHistoryActionType;
import com.hp.service.core.ElkOrderHistoryBasicType;
import com.hp.service.core.LoggingDomainType;
import com.hp.service.core.Q2CLogger;
import com.hp.service.core.Q2CLoggerFactory;
import com.hp.service.core.exception.BusinessApplicationException;
import com.hp.service.core.exception.ExceptionSeverity;
import com.hp.service.core.exception.ResponseException;
import com.hp.service.core.exception.ResponseExceptionList;
import com.hp.service.core.exception.SystemApplicationException;

@Component
public class ChecklistServiceImpl implements ChecklistService {

	@Autowired
	public CheckListRepository checkListRepository;

	@Autowired
	public RulesValidator rulesValidator ;

	@Autowired
	public RegexRulesValidator regexRulesValidator ;

	@Autowired
	public SpELRulesValidator spELRulesValidator ;

	@Autowired
	public SpELEntryRequiredRuleValidator spELEntryRequiredRuleValidator ; 

	@Autowired
	public ProductRulesValidator productRulesValidator ;

	@Autowired
	private ProcessCheckService processCheckService;	

	@Autowired
	private ApplicationService appService;

	@Autowired
	private ValidationRuleProcessor validationRuleProcessor ;

	@Autowired
	private ProcessRuleProcessor processRuleProcessor ;

	@Autowired
	private IEZPRSForObsoleteProducts ezprsService;
	
	@Autowired
	private IRulesCacheHandler rulesCacheHandler;
	
	@Autowired
	DropDownListService dropDownListService ;
	
	@Autowired
	public QuoteCacheHandler quoteCacheHandler ;	
	
	@Autowired
	private DetermineCTOStrategyService determineCTOStrategyService ;
	
	@Autowired
	private MyWorkspaceService myWorkSpaceService ;

	@Autowired 
	private IMDMService mdmServices;
	
	@Autowired
	ITrustedPricingService trustedPricingService;
	
	@Autowired
	private FrictionlessOrderService frictionlessOrderService ;
	
	@Autowired
	private QidsQuoteService qidsQuoteService;
	
	@Autowired
	private DetailChangeService detailChangeService;
	
	@Autowired
	private QuoteService quoteService;	
	
	@Autowired
    private FlomServices flomServices;
	@Autowired
	S4DealNumberValidationService s4DealNumberValidationServiceImpl;

	@Autowired
	public DetailChangeDAO detailChangeDAO;

	@Autowired
	private IConvertToOrderGateway convertToOrderGateway;

	private static final Q2CLogger LOG = Q2CLoggerFactory.getLogger(ChecklistServiceImpl.class,LoggingDomainType.PREVLD);
	private static final Q2CLogger LOG_ELK = Q2CLoggerFactory.getLogger(ChecklistServiceImpl.class, LoggingDomainType.ELK);
	private String enableDQMDealVersionNr; //US-18346_V3_DBFlag

	@PostConstruct
	public void init() {
		enableDQMDealVersionNr = CacheConfig.getValuee("ENABLE_DQM_DEALVERSION_VALIDATION");
	}
	
	/*
	 * Backward compatiblity support
	 */



	@Override
	public List<RegionList> regionList() {
		return checkListRepository.regionList();
	}


	@Override
	public List<CountryList> countryList(String regionCd) {
		return checkListRepository.countryList(regionCd);
	}


	@Override
	public List<TypeList> typeList() {
		return checkListRepository.typeList();
	}

	@Override
	public List<CheckListItem> getAllCheckListItem(String type) {
		return checkListRepository.getAllCheckListItem(type);
	}

	@Override
	public List<CheckListItem> getValidCheckListItem(String regionCd,
			String countrytype, Integer checkListType) {
		return checkListRepository.getValidCheckListItem(regionCd, countrytype, checkListType);
	}


	@Override
	public boolean saveCheckList(List<CheckListItem> validCheckListItems,
			RegionCountry rc) {
		return checkListRepository.saveCheckList(validCheckListItems,rc);
	}

	@Override
	public List<CheckListItem> getDataCheckList(String regionCd, String countryTyp, String processName) {
		List<CheckListItem> regularExpList = checkListRepository.getDataCheckList(regionCd, countryTyp, processName);

		return regularExpList;
	}


	@Override
	public boolean deleteDataCheckItem(int regionCountryCkId) {
		return checkListRepository.deleteDataCheckItem(regionCountryCkId);
	}


	@Override
	public List<RegionCountry> addDataCheckItem(RegionCountry regionCountry) {
		return checkListRepository.addDataCheckItem(regionCountry);
	}


	@Override
	public boolean updateRegionCountry(RegionCountry regionCountry) {
		return checkListRepository.updateRegionCountry(regionCountry);
	}


	@Override
	public CountryType getCountryTypByCode(String countryCode) {
		return checkListRepository.getCountryTypByCode(countryCode);
	}


	@Override
	public String getValidateErrorByKey(String errorKey) {
		return checkListRepository.getValidateErrorByKey(errorKey);
	}


	@Override
	public List<ErrorMessage> getErrorMessages() {
		return checkListRepository.getErrorMessages();
	}

	@Override
	public void applyRule(Quote quote, FieldRule fieldRule, ResponseExceptionList responseExceptionList) {
		getRulesValidatorHandler(fieldRule).applyRule (quote, fieldRule, responseExceptionList  );

	}

	private IRulesValidator getRulesValidatorHandler (FieldRule fieldRule) {
		IRulesValidator rulesValidatorObj = null ;
		if (fieldRule.getProductFieldRuleList()  != null && !fieldRule.getProductFieldRuleList().isEmpty() ) {
			rulesValidatorObj = productRulesValidator ;	
		}
		if (StringUtils.isNotEmpty (fieldRule.getRegx()) ) {
			rulesValidatorObj = regexRulesValidator ;
		} else if (StringUtils.equalsIgnoreCase(fieldRule.getEtryRlCd() , EntryRuleType.Entry_Required.toString() )
				|| StringUtils.equalsIgnoreCase(fieldRule.getEtryRlCd() , EntryRuleType.NOENTRY_BUT_REQUIRED.toString() )
				|| StringUtils.equalsIgnoreCase(fieldRule.getEtryRlCd() , EntryRuleType.NOEDIT_BUT_REQUIRED.toString() )) {
			rulesValidatorObj = spELEntryRequiredRuleValidator ;
		} else if (StringUtils.isNotEmpty (fieldRule.getSpelExpr()) ) {
			rulesValidatorObj = spELRulesValidator ;
		} else {
			rulesValidatorObj = rulesValidator ;
		}
		return rulesValidatorObj ;
	}

	@Override
	public String determineProcessingModel (PunchoutParam punchoutParam) {
		String processingModel = "" ;
		if (StringUtils.isEmpty ( punchoutParam.getRtmCd() ) ) {
			return processingModel ;
		}
		processingModel = punchoutParam.getProcessingModel() ;
		if (StringUtils.isEmpty(processingModel )) {
			// Need to check whether for the given 
			// input there will be multiple values returned
			List<String> processingModelList = checkListRepository.getProcessingModelList(punchoutParam) ;
			if (processingModelList != null && !processingModelList.isEmpty() )
				processingModel = processingModelList.get(0) ;

		}
		if (!StringUtils.isEmpty(processingModel)) {
			Quote quote = new Quote();
			QtPrchOrdReqt qtPrchOrdReqt = new QtPrchOrdReqt();
			quote.setQtPrchOrdReqt(qtPrchOrdReqt);
			quote.setRegionCd(punchoutParam.getSubRegionCd());
			quote.setCountryCd(punchoutParam.getCountryCd());
			qtPrchOrdReqt.setProcessingModelCd(processingModel);
			List<String> taskRoleList =  getTaskRoleListFromProcessingModel ( quote ) ;
			if (taskRoleList == null || taskRoleList.isEmpty()) {
				processingModel = null ;
			}
		}
		return processingModel ;
		
	}
	
	@Override
	public List<String> getTaskRoleListFromProcessingModel(Quote quote) {
		String processingModel = null;
		if(null != quote && null != quote.getQtPrchOrdReqt()) {
			processingModel = quote.getQtPrchOrdReqt().getProcessingModelCd();
		} else {
			return new ArrayList<String>();
		}
		
		// Till the development is completed
		if (StringUtils.equalsIgnoreCase(CacheConfig.getValuee("fvo.override2febesplit"), "Y")
			&&	StringUtils.isEmpty(processingModel) ) {
			processingModel = appService.getOrderMgmtApplnType(quote).getRegionBasedDisplayValues().getPrcssMdl() ;
		}
		List<String>permissionList = getAllPermissionList(CommonUtil.getCurrentUserName()) ; 
		if(permissionList == null || permissionList.isEmpty()){
			return new ArrayList<String>();
		}
		return checkListRepository.getTaskRoleListFromProcessingModel(processingModel, permissionList)  ;
	}
	
	@Override
	public List<FieldRule> getRules(RulesFilter rulesFilter) {
		
		LOG.debug("getRules:{}",rulesFilter.hashCode());
		
		List<FieldRule> fieldRuleList = rulesCacheHandler.cacheRules(rulesFilter);
		List<FieldRule> filteredRuleList = new ArrayList<FieldRule>() ; 
		boolean proceed = false ;
		for (FieldRule fRule : fieldRuleList) {
			proceed = false ;
			
			if ( StringUtils.isEmpty(fRule.getMndtryBitChar()) 
					|| CommonUtil.ANDANY(rulesFilter.getBitWiseQtChar(), NumberUtils.toLong(fRule.getMndtryBitChar() ) ) ) {
				if ( StringUtils.isEmpty(fRule.getNgtnBitChar()) 
						|| !CommonUtil.ANDANY(rulesFilter.getBitWiseQtChar(), NumberUtils.toLong(fRule.getNgtnBitChar() ) ) ){
					proceed = true ;
				}
			}
			
			if (proceed) {
				if ("ALL".equals(fRule.getBitOpTyp())) {
					if (CommonUtil.AND(rulesFilter.getBitWiseQtChar(), NumberUtils.toLong(fRule.getBitWiseQtChar() ) )
							|| StringUtils.isEmpty(fRule.getBitWiseQtChar()) ) {
							filteredRuleList.add(fRule) ;
					}
				} else if ("ANY".equals(fRule.getBitOpTyp())) {
					if (CommonUtil.ANDANY(rulesFilter.getBitWiseQtChar(), NumberUtils.toLong(fRule.getBitWiseQtChar() ) )
							|| StringUtils.isEmpty(fRule.getBitWiseQtChar()) ) {
							filteredRuleList.add(fRule) ;
					}				
				}
			}
		}
		
		return filteredRuleList;

		/*List<FieldRule> fieldRules = checkListRepository.getRules(rulesFilter);
		if (fieldRules != null && !fieldRules.isEmpty()) {
			List<String> fldRlCdList = new ArrayList<String>();

			for(FieldRule fieldRule : fieldRules){
				fldRlCdList.add(fieldRule.getFldRlCd());
			}

			List<FieldFieldRule> fieldFieldRules = getFieldFieldRules(fldRlCdList);
			List <FieldRuleAction> fieldRuleActionsList = checkListRepository.getFieldRuleActions(fldRlCdList) ;
			for(FieldRule fieldRule : fieldRules){
				LOG.debug("fieldRule = "+fieldRule+" fieldFieldRules "+fieldFieldRules.size()) ;
				for(FieldFieldRule fieldFieldRule : fieldFieldRules){
					if(StringUtils.equalsIgnoreCase(fieldRule.getFldRlCd(),fieldFieldRule.getFldRlCd())){
						fieldRule.getFieldFieldRuleList().add(fieldFieldRule) ;
					}
				}

				for (FieldRuleAction fieldRuleAction : fieldRuleActionsList) {
					if(StringUtils.equalsIgnoreCase(fieldRule.getFldRlCd(), fieldRuleAction.getFldRlCd())){
						fieldRule.getFieldRuleActionsList().add(fieldRuleAction) ;
					}
				}
			}	

		}
		return fieldRules;*/
	}


	@Override
	public List<FieldFieldRule> getFieldFieldRules(List<String> fldRlCdList,RulesFilter rulesFilter) {
		return checkListRepository.getFieldFieldRules( fldRlCdList,rulesFilter);
	}

	@Override
	public IGenericDisplayResponse getValidationSummaryDisplay ( FVOProcessValidationRequest request, Quote quote ) {
		List<Quote> quoteList = null ;
		if ( !request.getQidsFilter().isCcoaFlow() ) {
			quoteList = quoteCacheHandler.getQuotationDetails(request.getQidsFilter()) ;
		} else {
			LocalDBQuoteFilter ldbFilter = new LocalDBQuoteFilter() ;
			ldbFilter.setSlsQtnId(request.getQidsFilter().getSlsQtnId());
			ldbFilter.setAssetQuoteNrAndVrsn(request.getQidsFilter().getAssetQuoteNrAndVrsn());
			ldbFilter.setSlsQtnRvsnSqnNr(request.getQidsFilter().getSlsQtnRvsnSqnNr());
			quoteList = quoteCacheHandler.getQuoteFromAOE (ldbFilter) ;	
		}
		quote = quoteList.get(0) ;
		return getValidationSummaryByQuote (request, quote) ;
	}
	
	@Override
	public IGenericDisplayResponse getValidationSummaryByQuote ( FVOProcessValidationRequest request, Quote quote ) {
		IExecutionProcessor executionProcessor = null ;

		if (request.getFieldRuleList() != null && !request.getFieldRuleList().isEmpty()) {
			FieldRule fieldRule = request.getFieldRuleList().get(0) ;
			if (ValidationResponseEnum.VALIDATIONMESSAGES.equals( fieldRule.getResponseType() ) 
				|| ValidationResponseEnum.VALIDATIONTHRUMETHODS.equals( fieldRule.getResponseType() ) 
				|| ValidationResponseEnum.RULEANDSUMMARYMSGS.equals( fieldRule.getResponseType()  ) ) {
				executionProcessor = validationRuleProcessor ;
			} else {
				executionProcessor = processRuleProcessor ;
			}
			return executionProcessor.execute(quote, request) ;
		}

		return new FVOProcessResponse() ;		
	}


	@Override
	public void applyRules(Quote quote, RulesFilter rulesFilter, ApplyRuleResponse applyRuleRespnse) {
		Stopwatch stopwatchAR = Stopwatch.createStarted();
		Stopwatch stopwatch = Stopwatch.createStarted();
		populateRulesFilter (quote, rulesFilter) ;
		if (rulesFilter.getTaskRoleList().isEmpty()) {
			ResponseException re = new ResponseException("", ApplicationDomainType.OMUI, 1001, ExceptionSeverity.WARN, "LCL_NO_PERMISSION_KEY", "");
			applyRuleRespnse.getResponseExceptionList().add(re) ;
			applyRuleRespnse.setQuote(quote) ;
			applyRuleRespnse.setMaxSeverity((ExceptionSeverity.WARN.toString()) ) ;
			return  ;
		}
		stopwatch.stop();
		LOG.debug("AOE_PROFILING_APPLYRULE groupCode "+ rulesFilter.getPrcssGrpCd() +" time taken for populating rulesFilter " + stopwatch.elapsed(TimeUnit.MILLISECONDS) + " ms") ;
		
		stopwatch.reset() ;
		stopwatch.start();
		List <FieldRule> fieldRuleList = getRules(rulesFilter) ;
		
		if (StringUtils.equalsIgnoreCase(rulesFilter.getPrcssGrpCd(), FrictionLessValueOrder.QUOTE_SPILT)) {

			Collections.sort(fieldRuleList, new Comparator<FieldRule>() {
				@Override
				public int compare(FieldRule o1, FieldRule o2) {
					return new Integer(o1.getDisplaySequence()).compareTo(o2.getDisplaySequence());
				}

			});
		}
	
		stopwatch.stop();
		LOG.debug("AOE_PROFILING_APPLYRULE groupCode "+ rulesFilter.getPrcssGrpCd() +" time taken by getRules() " + stopwatch.elapsed(TimeUnit.MILLISECONDS) + " ms") ;

		stopwatch.reset() ;
		stopwatch.start();
		if (fieldRuleList == null || fieldRuleList.isEmpty()) {
			ResponseException re = new ResponseException("", ApplicationDomainType.OMUI, 1001, ExceptionSeverity.WARN, "LCL_NO_RULES_KEY", "");
			applyRuleRespnse.getResponseExceptionList().add(re) ;
			applyRuleRespnse.setQuote(quote) ;
			applyRuleRespnse.setMaxSeverity((ExceptionSeverity.WARN.toString()) ) ;
			return  ;
		}

		boolean processImplicitSet = true ;
		boolean noAssociatedRuleAction = true ;
		if (rulesFilter != null) {
			for (ExceptionSeverity svty : ExceptionSeverity.values()) {
				if (NumberUtils.toInt(svty.getCode(), 0) == 0) {
					continue ;
				}

				if (svty.equals(ExceptionSeverity.INFO) && 
						!processImplicitSet) {
					continue ;
				}

				for (FieldRule fieldRule : fieldRuleList) {
					/*
					if (StringUtils.isNotEmpty(fieldRule.getEtryRlCd()) &&  !(StringUtils.equalsIgnoreCase(fieldRule.getEtryRlCd() , EntryRuleType.Entry_Required.toString() )
							|| StringUtils.equalsIgnoreCase(fieldRule.getEtryRlCd() , EntryRuleType.NOENTRY_BUT_REQUIRED.toString() )
							|| StringUtils.equalsIgnoreCase(fieldRule.getEtryRlCd() , EntryRuleType.NOEDIT_BUT_REQUIRED.toString() ) ) ) {
						continue ;
					}
					*/
					
					if (fieldRule.getSvrtyCd().equals(svty)) {

						if ( /*!fieldRule.getFieldRuleActionsList().isEmpty() */ fieldRule.isProdAcnEnabled() ) {
							noAssociatedRuleAction = true ;
							for (FieldRuleAction frAction : fieldRule.getFieldRuleActionsList()) {
								if (rulesFilter.getProductActionList().contains( frAction.getActionId() )	) {
									noAssociatedRuleAction = false ;
									break ;
								}
							}
							if (noAssociatedRuleAction ) {
								continue ;
							}
						}

						if (!getRulesValidatorHandler(fieldRule).overrideSpelExprPostMandatoryCheck(quote, fieldRule, applyRuleRespnse.getResponseExceptionList()) ) {
							continue ;
						}


						if ( StringUtils.isEmpty(fieldRule.getRegx() )
								&& StringUtils.isEmpty (fieldRule.getSpelExpr() ) ) {
							continue ;
						}
						applyRule (quote, fieldRule, applyRuleRespnse.getResponseExceptionList() ) ;
					}
				}

				if (svty.equals(ExceptionSeverity.Error_Save_Not_Allowed) && 
						!applyRuleRespnse.getResponseExceptionList().isEmpty()	) {
					processImplicitSet = false  ;
				}

			}
		}

		ExceptionSeverity maxSeverity = ExceptionSeverity.INFO;
		applyRuleRespnse.getResponseExceptionList().setMaxSeverity(maxSeverity) ;
/*
		if (!applyRuleRespnse.getResponseExceptionList().isEmpty()
				&& !maxSeverity.equals(ExceptionSeverity.Error_Save_Not_Allowed)	) {
			rulesFilter.setRuleTimingEnum(RuleTimingEnum.POST_VALIDATION_RULE) ;
			List <FieldRule> postVaidationfieldRuleList = getRules(rulesFilter) ;
			if (postVaidationfieldRuleList != null 
					&& !postVaidationfieldRuleList.isEmpty()) {
				for (FieldRule fieldRule : postVaidationfieldRuleList) {
					applyRule (quote, fieldRule, applyRuleRespnse.getResponseExceptionList() ) ;
				}
			}
		}*/
		//US-18174 po category determination for EDI and EOP
		validateEDIandEOPOrders(quote,applyRuleRespnse);
		//US-18358 - code changes for USAGE as INB
		setPriceListForUsageChangeINB(quote,applyRuleRespnse);
		checkFanApproval(quote,applyRuleRespnse);

		//US-18520 -PreBuild changes
//		if(quote.getFunctionalModelCd()!=null) {
//			if (quote.getFunctionalModelCd().equals("1SLT") || quote.getFunctionalModelCd().equals("HFSBB")) {
//				deliveryDateCheck(quote, applyRuleRespnse);
//			}
//		}
	

		//US-18346-DQM Validation for DealVersionNumber
		if (StringUtils.equalsIgnoreCase(enableDQMDealVersionNr, "TRUE")) {
			LOG.info("QIDS DealVersion set to Quote DealVersionNr : " + quote.getDealVersionNr());
			if (StringUtils.startsWith(quote.getAssetQuoteNr(), "D") && StringUtils.isNotBlank(quote.getDealVersionNr())) {
				processAndValidateDealVersionNr(quote, applyRuleRespnse);
			}
		}
		ResponseExceptionList msgLessInfoFilteredResponseExceptionList = new ResponseExceptionList() ;

		removeDuplicateErrors(applyRuleRespnse);
		
		for (ResponseException bae : applyRuleRespnse.getResponseExceptionList()) {
			maxSeverity = ExceptionSeverity.max(maxSeverity, Enum.valueOf(ExceptionSeverity.class,  bae.getSeverity() ) ) ;
			if ( ( StringUtils.isNotEmpty( bae.getMessageDetails() ) || !ExceptionSeverity.INFO.toString().equals(bae.getSeverity()   ) )  ) {
				msgLessInfoFilteredResponseExceptionList.add(bae) ;
				continue ;
			}
		}		

		
		applyRuleRespnse.setResponseExceptionList(msgLessInfoFilteredResponseExceptionList/*applyRuleRespnse.getResponseExceptionList()*/) ;
		applyRuleRespnse.getResponseExceptionList().setMaxSeverity(maxSeverity) ;

		applyRuleRespnse.setMaxSeverity( maxSeverity.toString() ) ;
		applyRuleRespnse.setQuote(quote) ;
		stopwatch.stop();
		LOG.debug("AOE_PROFILING_APPLYRULE groupCode "+ rulesFilter.getPrcssGrpCd() +" time taken for processing ALL Rule " + stopwatch.elapsed(TimeUnit.MILLISECONDS) + " ms") ;
		
		QtFlag qtFlag = quote.getFlags().get(QtFlagType.NETPRICEDIFFFLAG);
			
		if(!quote.getQtPrchOrdReqt().isSerpFlag()) {
		if (rulesFilter.getPrcssGrpCd().equalsIgnoreCase("FVO_QD_SUBMIT") 
				&& CommonUtil.AND(quote.getBitWiseQtChar(), 256) 
				&&  (ExceptionSeverity.INFO.equals(applyRuleRespnse.getResponseExceptionList().getMaxSeverity()) || null == applyRuleRespnse.getResponseExceptionList().getMaxSeverity())) {
			if(StringUtils.isEmpty(quote.getVsoeApproverId())) {
				trustedPricingService.generateListPriceDiff(quote, applyRuleRespnse);
			}else if(qtFlag != null  && StringUtils.equalsIgnoreCase(qtFlag.getFlgVl(),"true")) {
				trustedPricingService.generateNetPriceDiff(quote, applyRuleRespnse);
			}
		}	
	}
		String logMessage = "PERFMONIT | CONTEXT=ApplyRules_"+rulesFilter.getPrcssGrpCd()+ " | "+ CommonUtil.getELKString(quote, stopwatchAR);
		LOG_ELK.debug(logMessage);
		return ;
	}

	private void removeDuplicateErrors(ApplyRuleResponse response) {

		ResponseExceptionList uniquesList = new  ResponseExceptionList();

		for (ResponseException bae : response.getResponseExceptionList()) {

		
			if(Iterables.any(Lists.newArrayList(uniquesList), getPredicate(bae.getFieldId())))
			continue;
			
			Iterable<ResponseException> responseList = getRedundantMessages(response.getResponseExceptionList(),bae.getFieldId());

			if(Iterables.size(responseList) > 1)
			{
				
				Iterable<ResponseException> finalList =  determineHighestMessageSeverity(responseList);		
				uniquesList.add(Iterables.get(finalList, 0));

			}
			else
				uniquesList.add(bae);
		}

		response.setResponseExceptionList(uniquesList);
	}

	
	private Iterable<ResponseException> determineHighestMessageSeverity(Iterable<ResponseException> responseList) {
		
		Iterable<ResponseException> errorSaveList = null;
	
		
		errorSaveList =  Iterables.filter(responseList,new Predicate<ResponseException>() {
				@Override
				public boolean apply(final ResponseException input) {
					return input.getSeverity().equals("Error_Save_Not_Allowed");
				}
			});
		
		if(Iterables.size(errorSaveList) == 1)
			return errorSaveList;
		
		errorSaveList =  Iterables.filter(responseList,new Predicate<ResponseException>() {
			@Override
			public boolean apply(final ResponseException input) {
				return !input.getCode().equals("JSR303");
			}
		});
		
		
		
		
		
		
		return errorSaveList;
	}


	private Iterable<ResponseException> getRedundantMessages(List<ResponseException> theCollection,String fieldId) {
		
		Iterable<ResponseException> responseExceptionList = Lists.newArrayList(theCollection);
		
		Iterable<ResponseException> resultList= Iterables.filter(responseExceptionList, getPredicate(fieldId));

		return resultList;
	}
	
	private Predicate<ResponseException> getPredicate(final String field)
	{
		
		return new Predicate<ResponseException>() {
			@Override
			public boolean apply(final ResponseException input) {
				// CR 165683
				return !CommonUtil.isEmptyString(field) && input.getFieldId().equals(field);
			}
		};
		
	}
	
	@Override
	public UiComponentMap convertToUiCompMap(List<FieldRuleResponse> fieldRuleResponseList){
		UiComponentMap uiCompMap = new UiComponentMap();
		String compMd = "B";
		String compNm = "noComp";
		
		uiCompMap.put(compNm, new ModeMap());
		uiCompMap.get(compNm).put(compMd, (new FieldRuleMap()));
		
		for(FieldRuleResponse rule : fieldRuleResponseList){
			FieldRuleMap fieldRulemap = uiCompMap.get(compNm).get(compMd);
			fieldRulemap.put(rule.getFldNm(), rule);
		}
		
		return uiCompMap;
	}
	
	@Override
	public UiComponentMap getEntryRuleQuote( Quote quote, RulesFilter rulesFilter) {
		Stopwatch stopwatch = Stopwatch.createStarted();
		UiComponentMap uiCompMap = new UiComponentMap();
		boolean isConverted = false ;
		
		populateRulesFilter (quote, rulesFilter) ;
		if (rulesFilter.getTaskRoleList().isEmpty()) return uiCompMap;
		if (quote != null && ( StringUtils.equalsIgnoreCase(quote.getSlsQtnVrsnSttsCd(), "Converted") 
				|| POStatusCodeType.ORCR.getCode().equals(quote.getQtPrchOrdReqt().getPoStatusCD())
				|| POStatusCodeType.Converted.getCode().equals(quote.getQtPrchOrdReqt().getPoStatusCD())
				|| POStatusCodeType.Cancel.getCode().equals(quote.getQtPrchOrdReqt().getPoStatusCD() ) 
				|| POStatusCodeType.CIP.getCode().equals(quote.getQtPrchOrdReqt().getPoStatusCD()) 
				|| POStatusCodeType.ATP_Hold.getCode().equals(quote.getQtPrchOrdReqt().getPoStatusCD()))
				|| rulesFilter.isReadOnlyUsr())
			isConverted = true ;
		rulesFilter.setDisplayDependent(true) ;
		
		List<FieldRule> fieldRuleList = getRules(rulesFilter);
		
		if (fieldRuleList == null || fieldRuleList.isEmpty()) return uiCompMap;
		
		List<FieldRule> ruleAppliedFieldRuleList = new ArrayList<FieldRule>() ;
		boolean entryCandidate = false;
		ResponseExceptionList responseExceptionList = new ResponseExceptionList() ;

	    flomServices.setContextualQuoteDefaults(quote);   
		for(FieldRule fieldRule : fieldRuleList) {
			entryCandidate = false;
			if( StringUtils.isEmpty(fieldRule.getEtryRlCd() ) ) continue;

			responseExceptionList.clear();

			if (/*!fieldRule.getFieldRuleActionsList().isEmpty() */ fieldRule.isProdAcnEnabled()  ) {
				boolean haltExecution = true;
				if (StringUtils.isNotBlank(fieldRule.getPrcssAcnBit())) {
					if (CommonUtil.ANDANY(quote.getCstmAtr().getProdAcnBitWiseChar(), NumberUtils.toLong(  fieldRule.getPrcssAcnBit(), 0 ) ) ) {
						haltExecution = false;
						//break;						
					}
				}
				/*
				for (FieldRuleAction frAction : fieldRule.getFieldRuleActionsList()) {
					if (rulesFilter.getProductActionList().contains( frAction.getActionId() )	) {
						haltExecution = false;
						break;
					}
				}
				*/
				if (haltExecution) continue;
			}

			if (StringUtils.isEmpty(fieldRule.getRegx() ) && StringUtils.isEmpty (fieldRule.getSpelExpr() ) ) {
				entryCandidate = true;
			} else {
				if (quote == null || StringUtils.isNotEmpty(fieldRule.getRegx() ) ) continue;
				
				//LOG.debug(fieldRule.getFldRlCd() +" " + fieldRule.getPrflId() +" Begins");
				applyRule (quote, fieldRule, responseExceptionList) ;
				//LOG.debug(fieldRule.getFldRlCd() +" " + fieldRule.getPrflId() +" ENDS");
			}
			if (entryCandidate || responseExceptionList.isEmpty()) ruleAppliedFieldRuleList.add(fieldRule);
		}

		EntryRuleType maxEntryRule = null;
		EntryRuleType existingEntryRule = null;
		boolean refreshEnabled = false;
		int entryRuleGroupPriority = -1 ;
		int index = 0;
		for(FieldRule fieldRule : ruleAppliedFieldRuleList){
			if( StringUtils.isEmpty( fieldRule.getEtryRlCd() ) ) continue;
			
			String compMd = fieldRule.getUiCompMd();
			String compNm = fieldRule.getUiCompNm();
			if(compMd == null || compMd.isEmpty()) compMd = "B";
			if(compNm == null || compNm.isEmpty()) compNm = "noComp";
			
			if(uiCompMap.get(compNm) == null) 				uiCompMap.put(compNm, new ModeMap());
			if(uiCompMap.get(compNm).get(compMd) == null) 	uiCompMap.get(compNm).put(compMd, (new FieldRuleMap()));
			FieldRuleMap fieldRuleMap = uiCompMap.get(compNm).get(compMd);
			
			if ( fieldRule.getFieldFieldRuleList().isEmpty() ) {
				index++;
				FieldRuleResponse fieldRuleResponse = new FieldRuleResponse();
				
				fieldRuleResponse.setEtryRlCd(isConverted ? EntryRuleType.Entry_Not_Allowed.getCode() :  fieldRule.getEtryRlCd());
				fieldRuleResponse.setPrcssCd(fieldRule.getPrcssCd()) ;
				fieldRuleMap.put("NoFldName"+index, fieldRuleResponse);
				continue;
			}
			
			for(FieldFieldRule fieldFieldRule : fieldRule.getFieldFieldRuleList() ) {
				String parsedFieldName = getRulesValidatorHandler(fieldRule).getParsedFieldNamesFromRule(fieldFieldRule.getFldNm());
				if( fieldRuleMap.get(parsedFieldName) == null ){
					FieldRuleResponse fieldRuleResponse = new FieldRuleResponse();
					fieldRuleResponse.setFldNm(parsedFieldName);
					fieldRuleResponse.setPrcssCd(fieldRule.getPrcssCd());
					
					fieldRuleResponse.setUiCompMd(compMd);
					fieldRuleResponse.setUiCompNm(compNm);
					fieldRuleResponse.setOrdrAccptncPrcss((fieldFieldRule.isOrdrAccptncPrcss()));
					entryRuleGroupPriority = fieldRule.getEntryRuleGroupPriority();
					fieldRuleResponse.setEntryRuleGroupPriority(entryRuleGroupPriority);
					
					fieldRuleMap.put(parsedFieldName, fieldRuleResponse);
					
					existingEntryRule = EntryRuleType.valueOf(fieldRule.getEtryRlCd()) ;
					refreshEnabled = fieldFieldRule.isRefreshEnabled();
				}else{
					existingEntryRule = EntryRuleType.valueOf( fieldRuleMap.get(parsedFieldName).getEtryRlCd() );
					refreshEnabled = fieldRuleMap.get(parsedFieldName).isRefreshEnabled();
					entryRuleGroupPriority = fieldRuleMap.get(parsedFieldName).getEntryRuleGroupPriority() ;
				}
				if (parsedFieldName.contains("customers.ENDCUSTOMER.company")) {
					//LOG.debug(" parsedFieldName "+parsedFieldName);
				}
				maxEntryRule = EntryRuleType.max( existingEntryRule, EntryRuleType.valueOf(fieldRule.getEtryRlCd()));
				if (entryRuleGroupPriority > fieldRule.getEntryRuleGroupPriority()) {
					maxEntryRule = existingEntryRule ;
				} else if (entryRuleGroupPriority < fieldRule.getEntryRuleGroupPriority()) {
					maxEntryRule = EntryRuleType.valueOf(fieldRule.getEtryRlCd()) ;
				} 
				fieldRuleMap.get(parsedFieldName).setOrgEtryRlCd(maxEntryRule.getCode());
				fieldRuleMap.get(parsedFieldName).setEtryRlCd(isConverted ? EntryRuleType.Entry_Not_Allowed.getCode() : maxEntryRule.getCode());
				if ( EntryRuleType.FORCE_HIDE.getCode().equals(maxEntryRule.getCode() ) && isConverted) {
					fieldRuleMap.get(parsedFieldName).setEtryRlCd( EntryRuleType.FORCE_HIDE.getCode() ) ;
				}

				fieldRuleMap.get(parsedFieldName).setLclFldLblKey( fieldFieldRule.getLclFldLblKey() );
				
				if ( maxEntryRule.equals( existingEntryRule ) ) fieldRuleMap.get(parsedFieldName).setRefreshEnabled( refreshEnabled );
				else fieldRuleMap.get(parsedFieldName).setRefreshEnabled( fieldFieldRule.isRefreshEnabled() );
				String miscAttributes = fieldFieldRule.getMiscAttributes();  
				if (StringUtils.isNotEmpty(miscAttributes)) {
					List<String> jsonElements = CommonUtil.smartSplit(miscAttributes, ",") ;
					
					/*
					JsonArrayBuilder jsonUserArray = Json.createArrayBuilder();
					for ( String anElement : jsonElements ) {
						JsonObject job = Json.createObjectBuilder().add(StringUtils.substringBefore(anElement, ":"), StringUtils.substringAfter(anElement, ":")).build() ;
						jsonUserArray.add(job);
					}
					JsonObject jsonFinalOutput = Json.createObjectBuilder().add("attributes", jsonUserArray).build();
					*/
					
					JsonObjectBuilder jsonObject = Json.createObjectBuilder() ;
					for ( String anElement : jsonElements ) {
						jsonObject.add(StringUtils.substringBefore(anElement, ":"), StringUtils.substringAfter(anElement, ":"));
					}
					fieldRuleMap.get(parsedFieldName).setMiscAttributes( jsonObject.build().toString());
				}						
				
			}
		}
		for ( String key1 : uiCompMap.keySet() ) {
			ModeMap modeMap = uiCompMap.get(key1) ;
			for (String mKey : modeMap.keySet()) {
				FieldRuleMap returnFrMap = new FieldRuleMap() ;
				FieldRuleMap fRuleMap = modeMap.get(mKey) ;
				for (String key : fRuleMap.keySet() ) {
					if (!fRuleMap.get(key).getEtryRlCd().equals(EntryRuleType.FORCE_HIDE.toString())  ) {
						returnFrMap.put(key, fRuleMap.get(key)) ;
					} else {
						//LOG.debug(" key "+key);
					}
				}	
				modeMap.put(mKey, returnFrMap) ;
			}
		}

		if (rulesFilter.isFormatUIResponse()) setUIFieldValidationForResponse (fieldRuleList, uiCompMap);
		
		String logMessage = "PERFMONIT | CONTEXT=GetEntryRule_"+rulesFilter.getPrcssGrpCd() + " | "+ CommonUtil.getELKString(quote, stopwatch);
		LOG_ELK.debug(logMessage);
		return uiCompMap;
	}
	
	private void setUIFieldValidationForResponse (List<FieldRule> fieldRuleList, UiComponentMap uiCompMap) {
		for(FieldRule fieldRule : fieldRuleList) {
			if( !StringUtils.isEmpty(fieldRule.getRegx()) ) {
				for(FieldFieldRule fieldFieldRule : fieldRule.getFieldFieldRuleList() ) {
					String compMd = fieldRule.getUiCompMd();
					String compNm = fieldRule.getUiCompNm();
					if(compMd == null || compMd.isEmpty()) compMd = "B";
					if(compNm == null || compNm.isEmpty()) compNm = "noComp";
					
					if(uiCompMap == null || uiCompMap.isEmpty() || uiCompMap.get(compNm) == null || uiCompMap.get(compNm).get(compMd) == null){
						    continue;
					}
					
					FieldRuleResponse fieldRuleResponse = uiCompMap.get(compNm).get(compMd).get(fieldFieldRule.getFldNm());
					if (fieldRuleResponse != null) {
						FieldValidation fldValidation = new FieldValidation();

						List<FieldValidationDetail> validations = new ArrayList<FieldValidationDetail>();
						FieldValidationDetail fldValidationDetail = new FieldValidationDetail();
                        if (fieldRuleResponse.getFldValidation() != null) {
                            validations = fieldRuleResponse.getFldValidation().getValidations();
                        }


						Map<String,Object> properties = new HashMap<String,Object>();
						properties.put("REGX", fieldRule.getRegx());
						properties.put("RTM_CD", fieldRule.getRtmEnum().name());
						properties.put("MSG_ID", fieldRule.getMsgId());
						properties.put("ETRY_RL_CD", fieldRuleResponse.getEtryRlCd());

						if (!StringUtils.isEmpty(fieldRule.getSvrtyCd().name())  ) {
							properties.put("SVRTY_CD", fieldRule.getSvrtyCd().name());

						} else {
							properties.put("SVRTY_CD", ExceptionSeverity.INFO);
						}

						fldValidationDetail.setProperties(properties);
						fldValidationDetail.setValidation("DB");
						fldValidationDetail.setValidationType("Pattern");
						validations.add(fldValidationDetail);
						fldValidation.setFldNm(fieldRuleResponse.getFldNm());
						fldValidation.setValidations(validations);
						fieldRuleResponse.setFldValidation(fldValidation);
					}
				}
			}
		}
	}

	private void populateRulesFilter (Quote quote,
			RulesFilter rulesFilter) {

		Stopwatch stopwatch = Stopwatch.createStarted();
		setDefaults4Rules (quote, rulesFilter) ;
		
		stopwatch.stop();
		//LOG.debug("AOE_PROFILING_H2_APPLYRULE groupCode "+ rulesFilter.getPrcssGrpCd() +" time taken for setDefaults4Rules " + stopwatch.elapsed(TimeUnit.MILLISECONDS) + " ms") ;
		
		stopwatch.start();
		populateProductActions4Rules(quote, rulesFilter) ;
		stopwatch.stop();
		//LOG.debug("AOE_PROFILING_H2_APPLYRULE groupCode "+ rulesFilter.getPrcssGrpCd() +" time taken for populateProductActions4Rules " + stopwatch.elapsed(TimeUnit.MILLISECONDS) + " ms") ;

		//getUserPermission4Rules(rulesFilter) ; 
		if (!rulesFilter.isElectronicOrder()) {
			stopwatch.start();
			/*
			 *  Uncomment this for dynamic control of FE and BE flow
			 */
			//Set<String> taskRoleList = new HashSet<String> ( getTaskRoleListFromProcessingModel(quote.getQtPrchOrdReqt().getProcessingModelCd()) );
			/*
			 *  Comment these for dynamic control of FE and BE flow
			 */
			Set<String> taskRoleList = new HashSet<String>() ;
			taskRoleList.add("ORCR") ;
			taskRoleList.add("ORCV") ;
			taskRoleList.add("CNCL") ;
			
			stopwatch.stop();
			//LOG.debug("AOE_PROFILING_H2_APPLYRULE groupCode "+ rulesFilter.getPrcssGrpCd() +" time taken for getTaskRoleListFromProcessingModel " + stopwatch.elapsed(TimeUnit.MILLISECONDS) + " ms") ;
			rulesFilter.setTaskRoleList(taskRoleList) ;
			/*String origAsset = AssetType.WATSON.getCode() ;
			
			if (appService.getQuoteType().getQuoteTypeType().equals(QuoteTypeType.SOFTWARE) ) {
				origAsset = "BMI - SW" ;
			}*/
			
			LocalizationFilter filterbase = null;
			if (null != quote) {
				filterbase = new LocalizationFilter();
				filterbase.setRegionCD(quote.getRegionCd());
				filterbase.setOrigAsset("Watson");
				filterbase.setOmTypCd("FLOM");
				if (getDUSCheck(quote, rulesFilter) && "NA".equals(quote.getRegionCd())
						&& "US".equals(quote.getCountryCd())) {
					filterbase.setOrigAsset("NGQ");
				} else if (!getDUSCheck(quote, rulesFilter) && "NA".equals(quote.getRegionCd())) {
					filterbase.setCountryCD(quote.getCountryCd());
				}
			} else {
				 filterbase = appService.getLocalizationFilterByCookies() ;
			}			
			rulesFilter.setOrigAsset(filterbase.getOrigAsset());
			//if(quote!=null && quote.getFlags().get(QtFlagType.SERPFLAG)!=null && "true".equalsIgnoreCase(quote.getFlags().get(QtFlagType.SERPFLAG).getFlgVl())) {
			if (quote != null && quote.getQtPrchOrdReqt() != null && quote.getQtPrchOrdReqt().isSerpFlag()) {
				rulesFilter.setOmTypCd("SERP");	
			} else {
				rulesFilter.setOmTypCd(appService.getApplType().getAppTypeType().toString());
			}
			//rulesFilter.setOmTypCd(appService.getApplType().getAppTypeType().toString());
			
			rulesFilter.setBitWiseQtChar(quote.getBitWiseQtChar());
	
			/*
			ProcessingStrategyModel inputFilterModel = new ProcessingStrategyModel() ;
			inputFilterModel.setCountryCd(rulesFilter.getCountryCd());
			inputFilterModel.setRegionCd(rulesFilter.getPrflRgn());
			inputFilterModel.setOmTypCd(appService.getApplType().getAppTypeType().toString());
			inputFilterModel.setSrcSystem(CanadaOrderCreationConstants.ORIGINATING_APPLICATION);
			ProcessingStrategyModel prcssngMdl = determineCTOStrategyService.getProcessingStrategyModel(inputFilterModel);
			rulesFilter.setProcessingStrategy(prcssngMdl == null ? null : prcssngMdl.getProcessingStrategy());
			*/		
		}

		return  ;
	}

	@Override
	public void setDefaults4Rules(Quote quote, RulesFilter rulesFilter) {

		if (quote != null) {
			rulesFilter.setRtmCd(quote.getRtm());
			rulesFilter.setPrflRgn(quote.getRegionCd());
			
			if("FVO_CRT_ORD".equals(rulesFilter.getPrcssGrpCd())) {
				rulesFilter.setPrflRgn("EU");
			}
			
			if (quote != null && quote.getQtPrchOrdReqt() != null && quote.getQtPrchOrdReqt().isSerpFlag()) {
				rulesFilter.setOmTypCd("SERP");	
			} else {
				rulesFilter.setOmTypCd("FLOM");
			}
			
			if(!getDUSCheck(quote, rulesFilter)
					&& ("NA".equals(quote.getRegionCd()) || null != quote.getCountryCd())) {				
				rulesFilter.setCountryCd(quote.getCountryCd());				
			} else if (getDUSCheck(quote, rulesFilter)
					&& null != quote.getRegionCd() && null != quote.getCountryCd()) {
				rulesFilter.setPrflRgn(quote.getRegionCd());
				rulesFilter.setCountryCd(quote.getCountryCd());
				rulesFilter.setOrigAsset("NGQ");				
			} /*else {
				Cookie cookieObj = CommonUtil.getCookieByName(CookieOptions.USER_COOKIE_OBJ); 
				if( cookieObj != null && StringUtils.isNotEmpty(cookieObj.getValue())){            
					UserProfileCookieModel cookieModel = appService.deSerializeUserProfileCookieModel(cookieObj.getValue());  
					rulesFilter.setPrflRgn(cookieModel.getRegion());
					rulesFilter.setCountryCd(cookieModel.getCountryCD());
				}
				LocalizationFilter filterbase = appService.getLocalizationFilterByCookies() ;
				if (StringUtils.isEmpty(rulesFilter.getPrflRgn() ) )
					//rulesFilter.setPrflRgn(FrictionLessValueOrder.EMEA_REGION_CD );
					rulesFilter.setPrflRgn(filterbase.getRegionCD());
					rulesFilter.setCountryCd(filterbase.getCountryCD());
			}*/
		} else {
			rulesFilter.setRtmCd(FrictionLessValueOrder.DEFAULT_RTM );			
		}		
	
	/*	if ( CommonUtil.isEmptyString(rulesFilter.getPrcssCd() )
				&& CommonUtil.isEmptyString(rulesFilter.getPrcssGrpCd() )	) {
			// throw BAE
		} */
	}

	private boolean getDUSCheck(Quote quote, RulesFilter rulesFilter) {
		if("DUS" .equals(quote.getQtPrchOrdReqt().getFlowType())) {
			return true;
		}
		return false;
	}


	private void populateProductActions4Rules(Quote quote, RulesFilter rulesFilter) {

		if ( quote != null ) {
			Set<String> productActionIdsList =  new HashSet<String>() ;
			List<ProcessProductCheck>  productActionsList =  processCheckService.getProductActions(quote) ;
			for (ProcessProductCheck eachRecord : productActionsList) {
				productActionIdsList.add(eachRecord.getPrcssId()+"") ;
			}	
			rulesFilter.setProductActionList(productActionIdsList) ;
		}
	}

	/*
	@Override
	public void getUserPermission4Rules(RulesFilter rulesFilter) {
		rulesFilter.setPermissionList(getAllPermissionList (CommonUtil.getCurrentUserName()) ) ;
	}
	*/
	
	private List<String>  getAllPermissionList(String userEmail) {
		List<Permission> permissionList = DAOFactory.getInstance().getPermissionDAO().getPermsnForCurrentUser(userEmail);;
		List<String> permissionIDList = new ArrayList<String>() ;
		if (permissionList != null && !permissionList.isEmpty()) {
			for (Permission permission : permissionList ) {
				permissionIDList.add(permission.getPermissionID()+"" ) ;
			}
		}
		return permissionIDList ;
	}

	@Override
	public FVOProcessValidationRequestList getAllValidationsList(Quote quote, RulesFilter rulesFilter) {
		populateRulesFilter (quote, rulesFilter) ;
		if (rulesFilter.getTaskRoleList().isEmpty()) {
			BusinessApplicationException re = new BusinessApplicationException(7221, ExceptionSeverity.WARN, "LCL_NO_PERMISSION_KEY");
			LOG.throwException(re) ;
		}

		List <FieldRule> fieldRuleList = getRules(rulesFilter) ;

		if (fieldRuleList == null || fieldRuleList.isEmpty()) {
			BusinessApplicationException re = new BusinessApplicationException(7221, ExceptionSeverity.WARN, "FieldRuleList is empty");
			LOG.throwException(re) ;
		}

		FVOProcessValidationRequestList fvoProcessValidationRqstList = new FVOProcessValidationRequestList() ;
		fvoProcessValidationRqstList.setQuote(quote) ;
		String validationType = "" ;
		Map<String, List<FieldRule>> processRuleMap= new HashMap<String, List<FieldRule>>() ;
		
		for (FieldRule fRule : fieldRuleList ) {
			validationType = fRule.getPrcssCd() ;
			/*
			if ( !fRule.isProcessExecutionRule() ) {
				validationType = "VALIDATION_MSGS" ;
			}
			 */
			List<FieldRule> processRuleList =	processRuleMap.get(validationType) ;
			if (processRuleList == null) {
				processRuleList = new ArrayList<FieldRule>() ;
				processRuleMap.put(validationType, processRuleList) ;
			}
			processRuleList.add(fRule) ;
		}

		for (String key : processRuleMap.keySet()) {
			FVOProcessValidationRequest fvoProcReqObj = new FVOProcessValidationRequest() ;
			fvoProcReqObj.setValidationType(key) ;
			fvoProcReqObj.setDescription("LCL_"+key+"_KEY") ;
			fvoProcReqObj.setFieldRuleList(processRuleMap.get(key) ) ;
			if (fvoProcReqObj.getFieldRuleList() != null 
					&& !fvoProcReqObj.getFieldRuleList().isEmpty()	) {
				fvoProcReqObj.setDisplaySequence(fvoProcReqObj.getFieldRuleList().get(0).getDisplaySequence() ) ;
			}			
			fvoProcessValidationRqstList.add(fvoProcReqObj) ;			
		}
		
		Comparator<FVOProcessValidationRequest> sequenceComparator = new Comparator<FVOProcessValidationRequest>() {
			@Override
			public int compare(FVOProcessValidationRequest o1, FVOProcessValidationRequest o2) {       
				return o1.getDisplaySequence() - o2.getDisplaySequence();
			}
		};
		Collections.sort(fvoProcessValidationRqstList, sequenceComparator) ;		


		return fvoProcessValidationRqstList ;		
	}
	
	public FVOProcessValidationRequestList getAllValidationsList(QidsFilter qidsFilter, RulesFilter rulesFilter) {
		List<Quote> quoteList = null ;
		if (!qidsFilter.isCcoaFlow()) {
			quoteList = quoteCacheHandler.getQuotationDetails(qidsFilter);	
		} else {
			LocalDBQuoteFilter ldbFilter = new LocalDBQuoteFilter() ;
			ldbFilter.setSlsQtnId(qidsFilter.getSlsQtnId());
			ldbFilter.setAssetQuoteNrAndVrsn(qidsFilter.getAssetQuoteNrAndVrsn());
			ldbFilter.setSlsQtnRvsnSqnNr(qidsFilter.getSlsQtnRvsnSqnNr());
			quoteList = quoteCacheHandler.getQuoteFromAOE (ldbFilter) ;	
		}
		
		Quote quote = quoteList.get(0) ;
		checkCoutryCodeMismatch(quote,qidsFilter);
		return getAllValidationsList (quote, rulesFilter) ;
	}

	private void checkCoutryCodeMismatch(Quote quote,QidsFilter qidsFilter) {


		StringBuilder validationmsg = null;
		String shiptoCountry = qidsFilter.getShiptoCountryCd();
		String resellerCountry = qidsFilter.getResellerCountryCd();
		String distributorCountry = qidsFilter.getDistributorCountryCd(); //US-18038 Used to validated distributor country in response
		String endCustCountry = qidsFilter.getEndCustomerCountryCd();
		String slsOrg = qidsFilter.getSalesOrg();

		if ( "STCK".equalsIgnoreCase( quote.getQtPrchOrdReqt().getPoCategory() ) ) {
			return ;
		}

		
		if ((quote.getQtPrchOrdReqt().isSerpFlag() && 
				!( StringUtils.equalsIgnoreCase( "NL60", quote.getSalesOrg() ) || StringUtils.equalsIgnoreCase( "NL60", quote.getCstmAtr().getSlsOrg())))
				|| (!quote.getQtPrchOrdReqt().isSerpFlag() && !(StringUtils.equalsIgnoreCase("E8", slsOrg)
						|| StringUtils.equalsIgnoreCase("CB", slsOrg)))) { 
			validationmsg = new StringBuilder();
			if (StringUtils.equalsIgnoreCase(quote.getRtm(), "VALUE_INDIRECT")) {
				if (!StringUtils.equalsIgnoreCase(shiptoCountry, endCustCountry) || !StringUtils.equalsIgnoreCase(resellerCountry, endCustCountry) || !StringUtils.equalsIgnoreCase(distributorCountry, endCustCountry) || !StringUtils.equalsIgnoreCase(shiptoCountry, distributorCountry) || !StringUtils.equalsIgnoreCase(shiptoCountry, resellerCountry)) {
					validationmsg.append(prepareResponse("Country", endCustCountry, resellerCountry, distributorCountry, shiptoCountry)); //US-18038 Added distributorCountry for validation of response
				}
				/*if (!StringUtils.equalsIgnoreCase(shiptoCountry, endCustCountry)) {
					validationmsg = StringUtils.isEmpty(validationmsg.toString())
							? validationmsg.append("Country Code Mismatch : \n" + " ( ShipTo Country: " + shiptoCountry
									+ " <> EndCustomer Country: " + endCustCountry + " )\n")
							: validationmsg.append(" ( ShipTo Country: " + shiptoCountry + " <> EndCustomer Country: "
									+ endCustCountry + " )\n");
				}

				if (!StringUtils.equalsIgnoreCase(resellerCountry, endCustCountry)) {
					validationmsg = StringUtils.isEmpty(validationmsg.toString())
							? validationmsg.append("Country Code Mismatch : \n" + " ( EndCustomer Country: "
									+ endCustCountry + " <> Reseller Country: " + resellerCountry + " )\n")
							: validationmsg.append(" ( EndCustomer Country: " + endCustCountry
									+ " <> Reseller Country: " + resellerCountry + " )");
				}
				if (!StringUtils.equalsIgnoreCase(shiptoCountry, resellerCountry)) {
					validationmsg = StringUtils.isEmpty(validationmsg.toString())
							? validationmsg.append("Country Code Mismatch : \n" + " ( ShipTo Country: " + shiptoCountry
									+ " <> Reseller Country: " + resellerCountry + " )")
							: validationmsg.append(" ( ShipTo Country: " + shiptoCountry + " <> Reseller Country: "
									+ resellerCountry + " )");
				}*/
			}
		}
		if (validationmsg != null) {
			QtCmt qtCmt = new QtCmt();
			qtCmt.setCmtTxt1(validationmsg.toString());
			qtCmt.setQtCmtType(QtCmtType.COUNTRYCODE);
			try {
				//Following method call will be uncommented after approval from IT
			    //qtCmt = validateFieldQTCmt(qtCmt);
			} catch(IllegalArgumentException e) {	
				e.printStackTrace();
				//System.out.println(e.getMessage());				
				LOG.throwException(new BusinessApplicationException(3005, e.getMessage()));    
			} catch(Exception e) {
				e.printStackTrace();
				//System.out.println(e.getMessage());
				LOG.throwException(new BusinessApplicationException(3005, e.getMessage()));
			}
			quote.getComments().put(QtCmtType.COUNTRYCODE, qtCmt);
		}
	}
	
	/**
	 * method to validate field cmtTxt1 from QtCmt class
	 * @param qtCmt
	 * @return qtCmt
	 */
	public QtCmt validateFieldQTCmt(QtCmt qtCmt) {
		//Validating field cmtTxt1 from QtCmt class
		//Following regular expression is subject to change after confirmation from IT
		if (!qtCmt.getCmtTxt1().matches("^[A-Z a-z 0-9]*$")) { 
			throw new IllegalArgumentException(String.format("String field cmtTxt1 [%s] of QTCmt class contains invalid characters", qtCmt.getCmtTxt1())); 
		}
		return qtCmt;
	}

	
	private String prepareResponse(String fieldName, String endCustomer, String reseller,String distributor , String shipTo) { //US-18038 Added Distributor to response
		String value="{\"fldName\":\""+fieldName+"\",\"EndCustomer\":\""+(StringUtils.isBlank(endCustomer)?"BLANK":endCustomer)+"\",\"Reseller\":\""+(StringUtils.isBlank(reseller)?"BLANK":reseller)+"\",\"Distributor\":\"" + (StringUtils.isBlank(distributor) ? "BLANK" : distributor) + "\",\"ShipTo\":\""+(StringUtils.isBlank(shipTo)?"BLANK":shipTo)+"\"}";
		return value;
	}
	
	@Override
	public IGenericDisplayResponse getNonZeroMeterialFrmLineItm(Quote quote, FieldRule fieldRule) {
		ProcessCheckResponse pcResponse = new ProcessCheckResponse() ;
		List<ProductProcessActions> productProcessActions=new ArrayList<ProductProcessActions>();
		List<QuoteItem> items = quote.getItems();
		for(QuoteItem item : items){
			ProductProcessActions ppa=new ProductProcessActions();
			if(item.getQuantity() != null)
				if(item.getQuantity()==0){
					ppa.setProdNr(item.getProductId());
					ppa.setSlsQtnItmSqnNr(item.getSlsQtnItmSqnNr());
					productProcessActions.add(ppa);
				}
		}
		if (!productProcessActions.isEmpty()) {
			pcResponse.setMaxSeverity(fieldRule.getSvrtyCd() == null ? "" : fieldRule.getSvrtyCd().toString()) ;
		}
		pcResponse.setProcessCheckActionsList(productProcessActions) ;
		return pcResponse;
	}

	@Override
	public IGenericDisplayResponse getItemsGroupByMCCDiscounts(Quote quote, FieldRule fieldRule) {
		List<QuoteItem> mainItemList = new ArrayList<QuoteItem>() ;
		List<QuoteItem> childItemList = new ArrayList<QuoteItem>() ;
		for (QuoteItem eachItem : quote.getItems()) {
			if (CommonUtil.greaterThanZero( CommonUtil.zeroOnNull( eachItem.getLclUntNtAmt()) ) ) {
				if (StringUtils.isNotEmpty(eachItem.getHeartLineItemNr()) 
						&&	( StringUtils.equalsIgnoreCase( eachItem.getProductClassCode(), "HW" ) 
								|| 	StringUtils.equalsIgnoreCase( eachItem.getProductClassCode(), "SW" ) )  ) {
					mainItemList.add(eachItem) ;
				}
				if (StringUtils.isEmpty(eachItem.getHeartLineItemNr() ) ) { 
					childItemList.add(eachItem) ;
				}	
			}
		}

		boolean mainItemAdded = false ;
		List<ProductProcessActions> processCheckActionsList = new ArrayList<ProductProcessActions> () ;

		for (QuoteItem mainItem : mainItemList) {
			for (QuoteItem childItem : childItemList) {
				if (StringUtils.equalsIgnoreCase( childItem.getProductNr() , mainItem.getProductNr() )
						&&  StringUtils.equalsIgnoreCase( mainItem.getSlsQtnItmSqnNr(),childItem.getCnfgnParentLineItemId() ) ) {
					BigDecimal mainItemDiscount = BigDecimal.ZERO ;
					BigDecimal childItemDiscount = BigDecimal.ZERO ;
					for (QuoteItemMcc mainItemMcc : mainItem.getItemMccs() ) {
						if (mainItemMcc.getRateFl()) {
							mainItemDiscount = CommonUtil.zeroOnNull ( mainItemMcc.getRateoramount() ).setScale( 3, BigDecimal.ROUND_HALF_UP );
							for (QuoteItemMcc childItemMcc : childItem.getItemMccs() ) {
								childItemDiscount = CommonUtil.zeroOnNull ( childItemMcc.getRateoramount() ).setScale( 3, BigDecimal.ROUND_HALF_UP );
								if (mainItemDiscount.compareTo(childItemDiscount) != 0 ) {
									if (!mainItemAdded) {
										ProductProcessActions productProcessAction = new ProductProcessActions();
										mainItemAdded = true ;
										productProcessAction.setMccDiscountPercent(mainItemDiscount.toString()) ;
										productProcessAction.setProdNr(mainItem.getProductNr()) ;
										productProcessAction.setSlsQtnItmSqnNr( mainItem.getHeartLineItemNr() ) ;
										processCheckActionsList.add(productProcessAction) ;
									}
									ProductProcessActions productProcessAction = new ProductProcessActions();
									productProcessAction.setMccDiscountPercent(childItemDiscount.toString()) ;
									productProcessAction.setProdNr(childItem.getProductId()) ;
									processCheckActionsList.add(productProcessAction) ;								
								}
							}
						}
					}
				}
			}
		}


		ProcessCheckResponse pcResponse = new ProcessCheckResponse() ;
		if (processCheckActionsList.size() > 0 ) {
			pcResponse.setMaxSeverity(fieldRule.getSvrtyCd() == null ? "" : fieldRule.getSvrtyCd().toString()) ;
		}
		pcResponse.setProcessCheckActionsList(processCheckActionsList) ;
		return pcResponse ;

	}


	public IGenericDisplayResponse getItemsGroupByMCCDiscounts1(Quote quote, FieldRule fieldRule) {
		Map<String, List<ProductProcessActions>> itemMccMap = new HashMap<String, List<ProductProcessActions>>() ;
		for (QuoteItem eachItem : quote.getItems()) {
			if (StringUtils.equalsIgnoreCase( eachItem.getProductClassCode(), "HW" ) 
					|| 	StringUtils.equalsIgnoreCase( eachItem.getProductClassCode(), "SW" )  ) {
				BigDecimal discount = BigDecimal.ZERO ;
				for (QuoteItemMcc itemMcc : eachItem.getItemMccs() ) {
					if (itemMcc.getRateFl()) {
						discount = CommonUtil.zeroOnNull ( itemMcc.getRateoramount() ).add(CommonUtil.zeroOnNull ( (eachItem.getPaDiscountAmount() ) ) ) ;
					}
				}
				ProductProcessActions productProcessAction = new ProductProcessActions();
				String mccValue = String.valueOf( discount ) ;
				List<ProductProcessActions> processCheckActionsList = itemMccMap.get(mccValue) ;
				if (processCheckActionsList == null) {
					processCheckActionsList = new ArrayList<ProductProcessActions>() ;
					itemMccMap.put( mccValue , processCheckActionsList ) ;
				}
				productProcessAction.setMccDiscountPercent(mccValue) ;
				productProcessAction.setProdNr(eachItem.getProductNr()) ;
				productProcessAction.setSlsQtnItmSqnNr(eachItem.getSlsQtnItmSqnNr()) ;
				processCheckActionsList.add(productProcessAction) ;				

				/*
				for (QuoteItemMcc itemMcc : eachItem.getItemMccs() ) {
					if (itemMcc.getRateFl()) {
						ProductProcessActions productProcessAction = new ProductProcessActions();

						String mccValue = String.valueOf( itemMcc.getRateoramount() ) ;
						List<ProductProcessActions> processCheckActionsList = itemMccMap.get(mccValue) ;
						if (processCheckActionsList == null) {
							processCheckActionsList = new ArrayList<ProductProcessActions>() ;
							itemMccMap.put( mccValue , processCheckActionsList ) ;
						}
						productProcessAction.setMccDiscountPercent(mccValue) ;
						productProcessAction.setProdNr(eachItem.getProductNr()) ;
						productProcessAction.setSlsQtnItmSqnNr(eachItem.getHeartLineItemNr()) ;
						processCheckActionsList.add(productProcessAction) ;

						break ;
					}
				}
				 */
			}
		}
		ProcessCheckResponse pcResponse = new ProcessCheckResponse() ;
		if (itemMccMap.size() > 1) {
			pcResponse.setMaxSeverity(fieldRule.getSvrtyCd() == null ? "" : fieldRule.getSvrtyCd().toString()) ;
			for (String key : itemMccMap.keySet())
				pcResponse.getProcessCheckActionsList().addAll(itemMccMap.get(key));
		}

		return pcResponse ;
	}	
	
	@Override
	public IGenericDisplayResponse getItemsGroupByMCCDiscounts05(Quote quote, FieldRule fieldRule) {
		List<QuoteItem> mainItemList = new ArrayList<QuoteItem>() ;
		List<QuoteItem> childItemList = new ArrayList<QuoteItem>() ;
		for (QuoteItem eachItem : quote.getItems()) {
			if (CommonUtil.greaterThanZero( CommonUtil.zeroOnNull( eachItem.getLclUntNtAmt()) ) ) {
				if (StringUtils.isNotEmpty(eachItem.getHeartLineItemNr()) 
						&&	( StringUtils.equalsIgnoreCase( eachItem.getProductClassCode(), "HW" ) 
								|| 	StringUtils.equalsIgnoreCase( eachItem.getProductClassCode(), "SW" ) )  ) {
					mainItemList.add(eachItem) ;
				}
				if (StringUtils.isEmpty(eachItem.getHeartLineItemNr() ) ) { 
					childItemList.add(eachItem) ;
				}	
			}
		}

		boolean discountDiffExists = false ;
		List<DiscountItemModel> itemList = new ArrayList<DiscountItemModel>() ;
		Set<String> mccKeyList = new HashSet<String>() ; 
		DiscountCheckResponse discountResponse = new DiscountCheckResponse() ;


		List<DiscountItemModel> optionsList = null ;
		for (QuoteItem mainItem : mainItemList) {
			DiscountItemModel discountItemBaseModel = discountResponse.new DiscountItemModel ();
			discountItemBaseModel.setProdNr(mainItem.getProductNr());
			discountItemBaseModel.setSlsQtnItmSqnNr( mainItem.getHeartLineItemNr() );
			optionsList = new ArrayList<DiscountItemModel>() ;
			discountDiffExists = false ;
			Set<String> mainItemMccKeyList = new HashSet<String>() ; 
			for (QuoteItem childItem : childItemList) {
				Set<String> optionItemMccKeyList = new HashSet<String>() ; 
				if (StringUtils.equalsIgnoreCase( childItem.getProductNr() , mainItem.getProductNr() )
						&&  StringUtils.equalsIgnoreCase( mainItem.getSlsQtnItmSqnNr(),childItem.getCnfgnParentLineItemId() ) ) {
					DiscountItemModel  discountItemOptionModel = discountResponse.new DiscountItemModel () ;
					discountItemOptionModel.setProdNr(childItem.getProductId());
					
					BigDecimal mainItemDiscount = BigDecimal.ZERO ;
					BigDecimal childItemDiscount = BigDecimal.ZERO ;
					for (QuoteItemMcc mainItemMcc : mainItem.getItemMccs() ) {
						mainItemDiscount = CommonUtil.zeroOnNull ( mainItemMcc.getRateoramount() ).setScale( 3, BigDecimal.ROUND_HALF_UP );
						for (QuoteItemMcc childItemMcc : childItem.getItemMccs() ) {

							if (!mainItemMcc.getRateFl() && !childItemMcc.getRateFl()) {
								mainItemMcc.setUntagDiscounted(true);
								childItemMcc.setUntagDiscounted(true);
								continue ;
							}

							childItemDiscount = CommonUtil.zeroOnNull ( childItemMcc.getRateoramount() ).setScale( 3, BigDecimal.ROUND_HALF_UP );
							/*
							if (mainItemMcc.getRateFl() && !childItemMcc.getRateFl()) {
								childItemDiscount = BigDecimal.ZERO ;
							}

							if (!mainItemMcc.getRateFl() && childItemMcc.getRateFl()) {
								mainItemDiscount = BigDecimal.ZERO ;
							}
							*/

							if (StringUtils.equalsIgnoreCase( mainItemMcc.getPriceCondType() ,  childItemMcc.getPriceCondType())) {
								if (mainItemDiscount.compareTo(childItemDiscount) != 0 ) {
									discountDiffExists = true ;
								}
							
								if (!mainItemMccKeyList.contains(mainItemMcc.getPriceCondType())) { 
									MCCObject mccBaseObject = discountItemBaseModel.new MCCObject();
									mccBaseObject.setPriceCondType(mainItemMcc.getPriceCondType());
									mccBaseObject.setMccDiscountPercent(mainItemDiscount.toString().concat(mainItemMcc.getRateFl() ? "%" : ""));
									mainItemMccKeyList.add(mainItemMcc.getPriceCondType()) ;
									discountItemBaseModel.getMccObjectList().add(mccBaseObject) ;
								}
								

								MCCObject mccOptionObject = discountItemOptionModel.new MCCObject();
								mccOptionObject.setPriceCondType(childItemMcc.getPriceCondType());
								mccOptionObject.setMccDiscountPercent(childItemDiscount.toString().concat(childItemMcc.getRateFl() ? "%" : ""));
								optionItemMccKeyList.add(childItemMcc.getPriceCondType()) ;
								discountItemOptionModel.getMccObjectList().add(mccOptionObject) ;
							}
						}
						if (!mainItemMccKeyList.contains(mainItemMcc.getPriceCondType())) {
							if (!mainItemMcc.isUntagDiscounted() && mainItemMcc.getRateFl()) {
								discountDiffExists = true ;
							}
							MCCObject mccBaseObject = discountItemBaseModel.new MCCObject();
							mccBaseObject.setPriceCondType(mainItemMcc.getPriceCondType());
							mccBaseObject.setMccDiscountPercent(mainItemDiscount.toString().concat(mainItemMcc.getRateFl() ? "%" : ""));
							mainItemMccKeyList.add(mainItemMcc.getPriceCondType()) ;
							discountItemBaseModel.getMccObjectList().add(mccBaseObject) ;
						}

					} 
					for (QuoteItemMcc childItemMcc : childItem.getItemMccs() ) { 
						childItemDiscount = CommonUtil.zeroOnNull ( childItemMcc.getRateoramount() ).setScale( 3, BigDecimal.ROUND_HALF_UP );
						if (!optionItemMccKeyList.contains(childItemMcc.getPriceCondType())) {
							if (!childItemMcc.isUntagDiscounted() && childItemMcc.getRateFl()) {
								discountDiffExists = true ;
							}
							MCCObject mccOptionObject = discountItemOptionModel.new MCCObject();
							mccOptionObject.setPriceCondType(childItemMcc.getPriceCondType());
							mccOptionObject.setMccDiscountPercent(childItemDiscount.toString().concat(childItemMcc.getRateFl() ? "%" : ""));
							optionItemMccKeyList.add(childItemMcc.getPriceCondType()) ;
							discountItemOptionModel.getMccObjectList().add(mccOptionObject) ;
						}
					}
					
					optionsList.add(discountItemOptionModel) ;
					
					if (discountDiffExists) {
						mccKeyList.addAll(optionItemMccKeyList);
					}
				}
			}
			if (discountDiffExists) {
				itemList.add(discountItemBaseModel);
				itemList.addAll(optionsList) ;
				mccKeyList.addAll(mainItemMccKeyList);
			}
		}


		if (itemList != null && !itemList.isEmpty()) {
			discountResponse.setMaxSeverity(fieldRule.getSvrtyCd() == null ? "" : fieldRule.getSvrtyCd().toString()) ;
		}
		discountResponse.setItemList(itemList) ;
		discountResponse.setMccKeyList(mccKeyList);
		return discountResponse ;

	}		

	@Override
	public IGenericDisplayResponse getItemsExceededThresholdDiscount (Quote quote, FieldRule fieldRule) {
		List<ProductProcessActions> processCheckActionsList = new ArrayList<ProductProcessActions>() ;
		for (QuoteItem eachItem : quote.getItems()) {
			BigDecimal discount = CommonUtil.zeroOnNull (eachItem.getPaDiscountRate() ) ;
			for (QuoteItemMcc itemMcc : eachItem.getItemMccs() ) {
				if (itemMcc.getRateFl()!=null && itemMcc.getRateFl()) {
					discount = CommonUtil.zeroOnNull ( itemMcc.getRateoramount() ).abs().add(  discount   ) ;
					break ;
				}
			}
			if ( discount.compareTo(new BigDecimal("99.8")) > 0 ) {
				ProductProcessActions productProcessAction = new ProductProcessActions();
				productProcessAction.setMccDiscountPercent(String.valueOf( discount )) ;
				productProcessAction.setProdNr(eachItem.getProductId()) ;
				productProcessAction.setSlsQtnItmSqnNr(eachItem.getHeartLineItemNr()) ;
				processCheckActionsList.add(productProcessAction) ;
			}
		}
		ProcessCheckResponse pcResponse = new ProcessCheckResponse() ;
		if (!processCheckActionsList.isEmpty()) {
			pcResponse.setMaxSeverity(fieldRule.getSvrtyCd() == null ? "" : fieldRule.getSvrtyCd().toString()) ;
		}

		pcResponse.setProcessCheckActionsList(processCheckActionsList) ;
		return pcResponse ;
	}

	@Override
	public IGenericDisplayResponse getDemoBuyoutReporting (Quote quote, FieldRule fieldRule) {
		IGenericDisplayResponse response = new TableProductResponse() ;
		if (CommonUtil.AND(quote.getBitWiseQtChar(), QuoteCharBitConstants.DEMOBUYOUT_INDICATOR)) {
			if (CommonUtil.AND(quote.getBitWiseQtChar(), QuoteCharBitConstants.ORDERS05_INDICATOR)) {
				response = getDemoBuyoutReportingOrder05 (quote, fieldRule) ;
			} else {
				response = getDemoBuyoutReportingOrder04 (quote, fieldRule) ;
			}
		}
		return response ;
	}
	
	
	public IGenericDisplayResponse getDemoBuyoutReportingOrder05 (Quote quote, FieldRule fieldRule) {
		TableProductResponse response = new TableProductResponse() ;
		response.setDisplayAlways( true );
		response.setMaxSeverity(fieldRule.getSvrtyCd() == null ? "" : fieldRule.getSvrtyCd().toString()) ;
		for (QuoteItem eachItem : quote.getItems()) {
			if (/*StringUtils.isNotEmpty( eachItem.getHeartLineItemNr())*/ true ) {
				
				String lineItemLoan = "" ;
				if (quote.getQtPrchOrdReqt().isSerpFlag()) {
					lineItemLoan = StringUtils.startsWith(StringUtils.upperCase( eachItem.getProductDescription() ) , "DEMO BUNDLE") ?  eachItem.getProductDescription() : "" ;
				} else {
					lineItemLoan = CommonUtil.getValueFromJSON(eachItem.getAdditionalInfo(), NGQConstants.LOAN_NUMBER);
				}
				String serialNumber = "" ;
				for (QuoteItemCarePack eachQuoteItemCarePack : eachItem.getItemCarePacks()) {
					 serialNumber = new StringBuffer(serialNumber).append( StringUtils.trim(eachQuoteItemCarePack.getSerialNr()) ).toString();
				}
				if (StringUtils.isNotEmpty(lineItemLoan)
					|| StringUtils.isNotEmpty(serialNumber)	) {
					ProductResponse eachRecord = response.new ProductResponse() ;
					if (StringUtils.isNotEmpty(lineItemLoan)) {
						eachRecord.setDemoBundleId(lineItemLoan);
						eachRecord.setDemoBundleItem(true);	
					}
					if (StringUtils.isNotEmpty(serialNumber)) {
						eachRecord.setSerialNumber(serialNumber);
						eachRecord.setSerialNumberItem(true);						
					}
					
					eachRecord.setProdNr(eachItem.getProductNr());
					eachRecord.setSlsQtnItmSqnNr(StringUtils.isNotEmpty( eachItem.getHeartLineItemNr()) ? eachItem.getHeartLineItemNr() : eachItem.getAssetItemNr());
					eachRecord.setProductDescription(eachItem.getProductDescription());
					response.getProductResponseList().add(eachRecord) ;		
				}
			}
		}
		if (quote.getQtPrchOrdReqt().isSerpFlag()) {
			response.setAuxString1(quote.getDealNrDerived());
			response.setAuxString2(quote.getLoanNumber());
			response.setPoRcvDt(quote.getPrchOrdAtachmt().getPoRcvDt());
		} 
		return response ;		
	}
	
	public IGenericDisplayResponse getDemoBuyoutReportingOrder04 (Quote quote, FieldRule fieldRule) {
		TableProductResponse response = new TableProductResponse() ;
		response.setDisplayAlways( (NumberUtils.toLong( quote.getBitWiseQtChar()  ) & QuoteCharBitConstants.DEMOBUYOUT_INDICATOR) > 0 );
		List<QuoteItem> demobundleList = new ArrayList<QuoteItem>() ;
		List<QuoteItem> slNoList = new ArrayList<QuoteItem>() ;
		for (QuoteItem eachItem : quote.getItems()) {
			if ( StringUtils.startsWith(eachItem.getProductDescription(), "Demo bundle") ) {
				demobundleList.add(eachItem) ;
			}

			if ( StringUtils.startsWith(eachItem.getProductDescription(), "S/N") ) {
				slNoList.add(eachItem) ;
			}

		}
		
		for (QuoteItem eachItem : quote.getItems()) {
			if (StringUtils.isNotEmpty( eachItem.getHeartLineItemNr()) ) {
				ProductResponse eachRecord = response.new ProductResponse() ;
				boolean itemAdded = false ;
				for (QuoteItem bundleItem : demobundleList) {
					if (StringUtils.equalsIgnoreCase(  eachItem.getAssetItemNr(),  String.valueOf(NumberUtils.toInt(bundleItem.getAssetItemNr()) + 1) ) ) {
						eachRecord.setDemoBundleId(bundleItem.getProductDescription());
						eachRecord.setDemoBundleItem(true);
						itemAdded = true ;
					}
				}
				
				for (QuoteItem slNoItem : slNoList ) {
					if (StringUtils.equalsIgnoreCase(  eachItem.getAssetItemNr(),  String.valueOf(NumberUtils.toInt(slNoItem.getAssetItemNr()) - 1) ) )  {
						eachRecord.setSerialNumber(StringUtils.replaceOnce( slNoItem.getProductDescription() , "S/N", "") );
						eachRecord.setSerialNumberItem(true);
						itemAdded = true ;
					}
				}
				if (itemAdded) {
					eachRecord.setProdNr(eachItem.getProductNr());
					eachRecord.setSlsQtnItmSqnNr(eachItem.getHeartLineItemNr());
					eachRecord.setProductDescription(eachItem.getProductDescription());
					response.getProductResponseList().add(eachRecord) ;						
				}
			}
		}
		
		response.setMaxSeverity(fieldRule.getSvrtyCd() == null ? "" : fieldRule.getSvrtyCd().toString()) ;
		return response ;
	}
	
	@Override
	public IGenericDisplayResponse getDemoBuyoutMCodeCheck (Quote quote, FieldRule fieldRule) {
		TableProductResponse response = new TableProductResponse() ;
		List<String> mccLStringist  ;
		long quoteBitIndicator = NumberUtils.toLong( quote.getBitWiseQtChar()  ) & QuoteCharBitConstants.DEMOBUYOUT_INDICATOR ;
		response.setDisplayAlways( quoteBitIndicator > 0 );

		boolean isDemoBuyoutQuote = quoteBitIndicator > 0 ;
		if (isDemoBuyoutQuote) {
			for (QuoteItem eachItem : quote.getItems()) {
				mccLStringist = new ArrayList<String>() ;
				for(QuoteItemMcc itemMcc: eachItem.getItemMccs()){
					mccLStringist.add(StringUtils.upperCase( itemMcc.getPriceCondType() ) ) ;
				}
				if (StringUtils.isNotEmpty(eachItem.getSupplyingDiv()) 
						&& !mccLStringist.isEmpty()
						&& StringUtils.isNotEmpty(eachItem.getHeartLineItemNr())) {
					if (appService.getOrderMgmtApplnType(quote).isDemoBuyoutMCCVariation(mccLStringist, eachItem)) {
						ProductResponse eachRecord = response.new ProductResponse() ;
						eachRecord.setMccCodeList(mccLStringist);
						eachRecord.setProdNr(eachItem.getProductNr());
						eachRecord.setProductDescription(eachItem.getProductDescription());
						eachRecord.setSlsQtnItmSqnNr(eachItem.getHeartLineItemNr());
						eachRecord.setSupplyingDiv(eachItem.getSupplyingDiv());
						response.getProductResponseList().add(eachRecord) ;
					}
				}
			}			
		}

		if (!response.getProductResponseList().isEmpty()) {
			response.setMaxSeverity(fieldRule.getSvrtyCd() == null ? "" : fieldRule.getSvrtyCd().toString()) ;
		}
		return response ;
	}

	@Override
	public IGenericDisplayResponse getDealQuoteEndCustomerAddress(Quote quote, FieldRule fieldRule) {

		Map<CustomerType,QuoteCustomerAddress> customerAddresses = new HashMap<CustomerType,QuoteCustomerAddress>();

		FVOReportResponse fvoReportResponse = new FVOReportResponse();

		if(quote != null && quote.getDealQuoteInfo() != null){
			customerAddresses.put(CustomerType.ENDCUSTOMER,quote.getDealQuoteInfo().getCustomerAddresses().get(CustomerType.ENDCUSTOMER));	
			fvoReportResponse.getQuoteCustomerAddressList().add(quote.getDealQuoteInfo().getCustomerAddresses().get(CustomerType.ENDCUSTOMER)) ;
			customerAddresses.put(CustomerType.QIDSENDCUSTOMER,quote.getDealQuoteInfo().getCustomerAddresses().get(CustomerType.QIDSENDCUSTOMER));	
			fvoReportResponse.getQuoteCustomerAddressList().add(quote.getDealQuoteInfo().getCustomerAddresses().get(CustomerType.QIDSENDCUSTOMER)) ;

		}


		fvoReportResponse.setCustomerAddresses(customerAddresses);
		fvoReportResponse.setMaxSeverity(fieldRule.getSvrtyCd() == null ? "" : fieldRule.getSvrtyCd().toString()) ;
		return fvoReportResponse;
	}

	@Override

	public IGenericDisplayResponse getSupplierCodesFrmLineItm(Quote quote, FieldRule fieldRule) {
		ProcessCheckResponse pcResponse = new ProcessCheckResponse() ;
		List<ProductProcessActions> productProcessActions=new ArrayList<ProductProcessActions>();
		List<QuoteItem> items = quote.getItems();

		Set<String> supplyingDivList = new HashSet<String>() ;
		for(QuoteItem item : items){

			if(StringUtils.isNotEmpty( item.getSupplyingDiv() ) 
					&& !supplyingDivList.contains(item.getSupplyingDiv())		){
				ProductProcessActions ppa = new ProductProcessActions();		
				supplyingDivList.add(item.getSupplyingDiv()) ;	
				ppa.setSupplyingDivOrMCC(item.getSupplyingDiv()) ;
				ppa.setSeverity(ExceptionSeverity.INFO.toString()) ;
				productProcessActions.add(ppa);
			}			
		}
		supplyingDivList.clear() ;
		supplyingDivList = null ;
		pcResponse.setProcessCheckActionsList(productProcessActions) ;
		pcResponse.setMaxSeverity(fieldRule.getSvrtyCd() == null ? "" : fieldRule.getSvrtyCd().toString()) ;

		return pcResponse;
	}
	
	@Override
	public IGenericDisplayResponse getFusionSupplierCodesFrmLineItm(Quote quote, FieldRule fieldRule) {
		ProcessCheckResponse pcResponse = new ProcessCheckResponse() ;
		List<ProductProcessActions> productProcessActions=new ArrayList<ProductProcessActions>();
		List<QuoteItem> items = quote.getItems();
		Set<String> supplyingDivList = new HashSet<String>() ;
		HashMap<String, String> supplyDivMap = new HashMap<String, String>();
		List<FusionSupplierModel> supplModelList = dropDownListService.getFusionSupplierList();
		boolean missingSupplyDivHeaderLevel = false;

		for(FusionSupplierModel supplierCodeList : supplModelList){
			supplyDivMap.put(  supplierCodeList.getFusionSupplierCD() == null ? "" : supplierCodeList.getFusionSupplierCD().toUpperCase() , supplierCodeList.getFusionSupplierDN() );
		}

		for(QuoteItem item : items){
			String itemSupplyDiv = item.getSupplyingDiv();
			if (StringUtils.isNotEmpty( itemSupplyDiv ) ) 
			{
				itemSupplyDiv = itemSupplyDiv.toUpperCase() ;
				if ( !supplyingDivList.contains(itemSupplyDiv)		){
					ProductProcessActions ppa = new ProductProcessActions();	
					ppa = new ProductProcessActions();
					supplyingDivList.add(itemSupplyDiv) ;	
					ppa.setSupplyingDivOrMCC(itemSupplyDiv) ;
					if(supplyDivMap.containsKey(itemSupplyDiv)){  
						ppa.setSeverity(ExceptionSeverity.INFO.toString()) ;
						ppa.setSupplyingDivDesc(supplyDivMap.get(itemSupplyDiv)+itemSupplyDiv);
					}
					else{
						missingSupplyDivHeaderLevel = true;
						ppa.setSeverity(ExceptionSeverity.Error_Save_Allowed.toString());
						ppa.setSupplyingDivDesc("Not a valid Fusion Supplier");
	
	
					}
					productProcessActions.add(ppa);
				}				
			}
		
		}

		supplyingDivList.clear() ;
		supplyingDivList = null ;
		//remove next line
		pcResponse.setProcessCheckActionsList(productProcessActions) ;
		if(missingSupplyDivHeaderLevel){
			pcResponse.setMaxSeverity(ExceptionSeverity.Error_Save_Allowed.toString()) ;
		}else{
			pcResponse.setMaxSeverity(ExceptionSeverity.INFO.toString()) ;

		}

 		return pcResponse;
	}

	@Override
	public IGenericDisplayResponse getMCCCodesFrmLineItm(Quote quote, FieldRule fieldRule) {
		ProcessCheckResponse pcResponse = new ProcessCheckResponse() ;
		List<ProductProcessActions> productProcessActions=new ArrayList<ProductProcessActions>();
		List<QuoteItem> items = quote.getItems();
		Set<String> mccList = new HashSet<String>() ;

		for(QuoteItem item : items){

			for(QuoteItemMcc itemMcc:item.getItemMccs()){
				if(StringUtils.isNotEmpty( itemMcc.getPriceCondType() ) 
						&& 	!mccList.contains(itemMcc.getPriceCondType())	) {
					ProductProcessActions ppa = new ProductProcessActions();		
					mccList.add(itemMcc.getPriceCondType()) ;
					ppa.setSupplyingDivOrMCC(itemMcc.getPriceCondType()) ;
					ppa.setSeverity(ExceptionSeverity.INFO.toString()) ;
					productProcessActions.add(ppa);
				}
			}		
		}

		mccList.clear() ;
		mccList = null ;

		pcResponse.setProcessCheckActionsList(productProcessActions) ;
		pcResponse.setMaxSeverity(fieldRule.getSvrtyCd() == null ? "" : fieldRule.getSvrtyCd().toString()) ;
		return pcResponse;
	}
	
	public IGenericDisplayResponse checkForTradeInProducts(Quote quote, FieldRule fieldRule) {
		List<ProductProcessActions> processCheckActionsList = new ArrayList<ProductProcessActions>() ;
		for (QuoteItem eachItem : quote.getItems()) {
			if(eachItem.getLclUntNtAmt() != null && eachItem.getLclUntNtAmt().intValue() < 0){
				ProductProcessActions productProcessAction = new ProductProcessActions();
				productProcessAction.setProdNr(eachItem.getProductNr()) ;
				productProcessAction.setSlsQtnItmSqnNr(eachItem.getHeartLineItemNr()) ;
				processCheckActionsList.add(productProcessAction) ;
			}
		}
		ProcessCheckResponse pcResponse = new ProcessCheckResponse() ;
		pcResponse.setProcessCheckActionsList(processCheckActionsList) ;
		if (!processCheckActionsList.isEmpty() ) {
			pcResponse.setMaxSeverity(fieldRule.getSvrtyCd() == null ? "" : fieldRule.getSvrtyCd().toString()) ;
		}
		return pcResponse ;
	}	
	
	@Override
	public IGenericDisplayResponse checkForMultipleDeals (Quote quote, FieldRule fieldRule) {
		ProcessCheckResponse pcResponse = new ProcessCheckResponse() ;
		Set<String> dealList = new HashSet<String>() ;
		List<ProductProcessActions> processCheckActionsList = new ArrayList<ProductProcessActions>() ;


		if (StringUtils.isNotEmpty( quote.getDealNr() ) ) {
			dealList.add( quote.getDealNr() ) ;
			ProductProcessActions productProcessAction = new ProductProcessActions();
			productProcessAction.setProdNr( quote.getDealNr() ) ;
			processCheckActionsList.add(productProcessAction) ;
		}
		for (QuoteItem eachItem : quote.getItems()) {
			if (StringUtils.isNotEmpty(eachItem.getDealNr() )
				&& !dealList.contains(eachItem.getDealNr())	) {
				dealList.add(eachItem.getDealNr()) ;
				ProductProcessActions productProcessAction = new ProductProcessActions();
				productProcessAction.setProdNr(eachItem.getDealNr()) ;
				processCheckActionsList.add(productProcessAction) ;
			}
		}
		if (dealList == null || ( dealList.size() == 0  || dealList.size() == 1 )  ) {
			//pcResponse.setMaxSeverity(fieldRule.getSvrtyCd() == null ? "" : fieldRule.getSvrtyCd().toString()) ;
			pcResponse.setDisplayAlways(false);;
		} else {
			pcResponse.setMaxSeverity(fieldRule.getSvrtyCd() == null ? "" : fieldRule.getSvrtyCd().toString()) ;
			pcResponse.setProcessCheckActionsList(processCheckActionsList) ;
			pcResponse.setDisplayAlways(true);;
		}
		return pcResponse ;
	}
	
	@Override
	public IGenericDisplayResponse findObsoleteProductsFromCorona(Quote quote, FieldRule fieldRule) {
		String additionalInfo = null ;
		List<ProductProcessActions> processCheckActionsList = new ArrayList<ProductProcessActions>() ;
		ObsoleteLineItemsResponse response = new ObsoleteLineItemsResponse();

		for (QuoteItem eachItem : quote.getItems()) {
			if (!(StringUtils.equalsIgnoreCase(eachItem.getLineTypeCd(), "Product")
				|| StringUtils.equalsIgnoreCase(eachItem.getLineTypeCd(), "Option")
				|| StringUtils.equalsIgnoreCase(eachItem.getLineTypeCd(), "PN")
				|| StringUtils.equalsIgnoreCase(eachItem.getLineTypeCd(), "OP") ) ) {
				continue ;
			}
			additionalInfo = eachItem.getAdditionalInfo();
			if (additionalInfo != null) {
				String plc = CommonUtil.getValueFromJSON(additionalInfo, "plc");
				if (!CommonUtil.isEmptyString(plc) ) {
					String eol = CommonUtil.getValueFromJSON(plc, "eol" ) ;
					if (!CommonUtil.isEmptyString(eol) ) {
						Calendar calendar = Calendar.getInstance();
						calendar.setTimeInMillis(NumberUtils.toLong(eol));
						Date eolDate = calendar.getTime() ;
						if (eolDate != null && eolDate.before(new Date())) {
							ProductProcessActions productProcessAction = new ProductProcessActions();
							productProcessAction.setProdNr(eachItem.getProductId()) ;
							productProcessAction.setSlsQtnItmSqnNr(eachItem.getHeartLineItemNr()) ;
							processCheckActionsList.add(productProcessAction) ;
						}
					}
				}
			}
		}
		response.setProcessCheckActionsList(processCheckActionsList);
		if (!processCheckActionsList.isEmpty() ) {
			response.setMaxSeverity(fieldRule.getSvrtyCd() == null ? "" : fieldRule.getSvrtyCd().toString()) ;
		}
		return response;
	}

	@Override
	public IGenericDisplayResponse checkLineItemSolution(Quote quote, FieldRule fieldRule) {
		ProcessCheckResponse response = new ProcessCheckResponse() ;
		List<ProductProcessActions> processCheckActionsList = new ArrayList<ProductProcessActions>() ;

		response.setDisplayAlways( true );
		response.setMaxSeverity(fieldRule.getSvrtyCd() == null ? "" : fieldRule.getSvrtyCd().toString()) ;
		if (quote != null && quote.getItems() != null ) {
			List<QuoteItem> allParentQuoteItems = new ArrayList<QuoteItem>() ;
			for (QuoteItem input : quote.getItems()) {
				if ("config".equalsIgnoreCase(input.getLineTypeCd()) 
						|| "bundle".equalsIgnoreCase(input.getLineTypeCd()) ) {
					allParentQuoteItems.add(input) ;
				}
			}
			
			for (QuoteItem item : allParentQuoteItems) {
				
				//Iterable<QuoteItem> itemsWithoutSolutioId =  Iterables.filter(quote.getItems(), predicateForEmptySolutionID(item.getCnfgnSystemName()));
				for ( QuoteItem component : quote.getItems() ) {
					if ("Product".equalsIgnoreCase(component.getLineTypeCd())
							|| "Option".equalsIgnoreCase(component.getLineTypeCd()) ) {
						if (item.getCnfgnSystemName() != null 
								&& item.getCnfgnSystemName().equalsIgnoreCase(component.getCnfgnSystemName())
								&&	StringUtils.isEmpty(component.getCnfgnSolId())) {
							ProductProcessActions productProcessAction = new ProductProcessActions();
							productProcessAction.setProdNr(component.getProductId()) ;
							productProcessAction.setSlsQtnItmSqnNr(component.getHeartLineItemNr()) ;
							processCheckActionsList.add(productProcessAction) ;		
							response.setProcessCheckActionsList( processCheckActionsList );
						}
					}					
				}
			}

			


			/*
			Iterable<QuoteItem> itemsByParent = Iterables.filter(quote.getItems(), predicateForConfigParent());
			if (!Iterables.isEmpty(itemsByParent) ) {
				Iterable<QuoteItem> itemsWithoutSolutioId =  Iterables.filter(itemsByParent, predicateForEmptySolutionID());
				if (!Iterables.isEmpty(itemsWithoutSolutioId) ) {
					for (QuoteItem item : itemsWithoutSolutioId) {
						ProductProcessActions productProcessAction = new ProductProcessActions();
						productProcessAction.setProdNr(item.getProductId()) ;
						productProcessAction.setSlsQtnItmSqnNr(item.getHeartLineItemNr()) ;
						processCheckActionsList.add(productProcessAction) ;		
						response.setProcessCheckActionsList( processCheckActionsList );
					}
				}
			}
			*/
			/* For non S4 a
			else {
				Iterable<QuoteItem> itemsWithoutSolutioId =  Iterables.filter(quote.getItems(), predicateForEmptySolutionID());
				for (QuoteItem item : itemsWithoutSolutioId) {
					ProductProcessActions productProcessAction = new ProductProcessActions();
					productProcessAction.setProdNr(item.getProductId()) ;
					productProcessAction.setSlsQtnItmSqnNr(item.getHeartLineItemNr()) ;
					processCheckActionsList.add(productProcessAction) ;		
					response.setProcessCheckActionsList( processCheckActionsList );
				} 
			}*/
		}
		if (response.getProcessCheckActionsList() == null 
				|| response.getProcessCheckActionsList().isEmpty()) {
			response.setMaxSeverity("") ;
		} 
		return response ;
	}
	
	
	private Predicate<QuoteItem> predicateForEmptySolutionID(final String cnfgnSystemName) {
		return new Predicate<QuoteItem>() {
			@Override
			public boolean apply(final QuoteItem input) {
				if ("Product".equalsIgnoreCase(input.getLineTypeCd())
					|| "Option".equalsIgnoreCase(input.getLineTypeCd()) ) {
					if (cnfgnSystemName != null 
						&& cnfgnSystemName.equalsIgnoreCase(input.getCnfgnSystemName())
						&&	StringUtils.isEmpty(input.getCnfgnSolId())) {
						return true;
					}
					return false ;
				}
				return false;
			}
		};
	}


	private Predicate<QuoteItem> predicateForConfigParent() {

		return new Predicate<QuoteItem>() {
			@Override
			public boolean apply(final QuoteItem input) {
				return "config".equalsIgnoreCase(input.getLineTypeCd()) 
						|| "bundle".equalsIgnoreCase(input.getLineTypeCd()) ;
			}
		};
	}

	@Override
	public IGenericDisplayResponse findObsoleteProducts(Quote quote, FieldRule fieldRule) {
		
		if (!AssetType.WATSON.getCode().equalsIgnoreCase(quote.getOrigAsset())
			&& CommonUtil.isEmptyString(quote.getVsoeApproverId()) 
			&& !CommonUtil.AND(quote.getBitWiseQtChar(), QuoteCharBitConstants.EORDER_INDICATOR)) {
			return findObsoleteProductsFromCorona (quote, fieldRule) ;
		}

		List<ProductProcessActions> processCheckActionsList = new ArrayList<ProductProcessActions>() ;

		ObsoleteLineItemsResponse response = new ObsoleteLineItemsResponse();
		QuotePricingable pricingInterface = new WrappedQuote(quote);

		List<QuoteItemPricingable> obsoleteProductsList = null ; //ezprsService.findObsoleteProducts(pricingInterface);
		String exceptionCode = "" ;
		String exceptionMsg = "" ;
		long startingTime = System.currentTimeMillis();
		try {
			obsoleteProductsList = ezprsService.findObsoleteProducts(pricingInterface);
			// Log ELK with SUCCESS
			String objectDesc = obsoleteProductsList.size() > 0 ? obsoleteProductsList.toString() : "None";
			Map<ElkOrderHistoryBasicType, String> attribs =
					CommonUtil.createAttribsMapForElkLog("SUCCESS",
					"",
					ElkOrderHistoryActionType.OBSOLETE_PROD_CALL.getCode(),
					quote.getAssetQuoteNrAndVrsn(),
					quote.getQtPrchOrdReqt().getWfmCaseId(),
					quote.getPrchOrdAtachmt().getPoNr(),
					quote.getSlsQtnRvsnSqnNr(),
					detailChangeService.getTransactionID(quote),
					objectDesc);
			CommonUtil.setInfoToElkLog(startingTime, attribs);
		} catch (SystemApplicationException sae) {
			sae.printStackTrace();
			exceptionCode = sae.getExCode() + "";
			exceptionMsg = "LCL_VLDTN_SMRY_EZPRS_BAE";// sae.getMessage() ;
			// Log ELK with FAILURE
			Map<ElkOrderHistoryBasicType, String> attribs =
					CommonUtil.createAttribsMapForElkLog("FAILED",
					"Product validation service is unavailable",
					ElkOrderHistoryActionType.OBSOLETE_PROD_CALL.getCode(),
					quote.getAssetQuoteNrAndVrsn(),
					quote.getQtPrchOrdReqt().getWfmCaseId(),
					quote.getPrchOrdAtachmt().getPoNr(),
					quote.getSlsQtnRvsnSqnNr(),
					detailChangeService.getTransactionID(quote),
					"");
			CommonUtil.setInfoToElkLog(startingTime, attribs);
		} catch (Exception e) {
			e.printStackTrace();
			exceptionCode = "UNKNOWN";
			exceptionMsg = "LCL_VLDTN_SMRY_EZPRS_EXCEPTION";// e.getMessage() ;
			// Log ELK with FAILURE
			Map<ElkOrderHistoryBasicType, String> attribs =
					CommonUtil.createAttribsMapForElkLog("FAILED",
					"Unknown Exception in Product validation service",
					ElkOrderHistoryActionType.OBSOLETE_PROD_CALL.getCode(),
					quote.getAssetQuoteNrAndVrsn(),
					quote.getQtPrchOrdReqt().getWfmCaseId(),
					quote.getPrchOrdAtachmt().getPoNr(),
					quote.getSlsQtnRvsnSqnNr(),
					detailChangeService.getTransactionID(quote),
					"");
			attribs.put(ElkOrderHistoryBasicType.Quote_Version, quote.getSlsQtnVrsnSqnNr());
			CommonUtil.setInfoToElkLog(startingTime, attribs);
		}
		
		if (obsoleteProductsList ==  null) {
			response.setStatusCode(false);
			response.setExceptionCode(exceptionCode);
			response.setExceptionMsg(exceptionMsg);
			response.setMaxSeverity ( ExceptionSeverity.Error_Save_Allowed.toString()) ;
	        return response ;			
		}

		
		for (QuoteItemPricingable eachItem : obsoleteProductsList)
		{
			ProductProcessActions productProcessAction = new ProductProcessActions();
			productProcessAction.setProdNr(eachItem.getProductId()) ;
			productProcessAction.setSlsQtnItmSqnNr(eachItem.getHeartLineItemNr()) ;
			processCheckActionsList.add(productProcessAction) ;
		}
		response.setProcessCheckActionsList(processCheckActionsList);

		if (!processCheckActionsList.isEmpty() ) {
			response.setMaxSeverity(fieldRule.getSvrtyCd() == null ? "" : fieldRule.getSvrtyCd().toString()) ;
		}
		
		return response;
	}

	@Override
	public Map<String, FieldValidation> getFieldValidationMapByPrflHeader(LocalizationFilter filterbase, ProfileHeaderFilter profileHeader) {
		Map<String, FieldValidation> fieldValidationMap =   rulesCacheHandler.getAllStaticAnnotation () ;
		/*
		if (quote != null && quote.getQtPrchOrdReqt() != null && quote.getQtPrchOrdReqt().isSerpFlag()) {
			filterbase.setOmTypCd("SERP");	
		}
		*/
		List<FieldValidation> fieldValidationsList = checkListRepository.getAllRegexRules(filterbase);
		Map<String, FieldValidation> filteredFieldValidationMap = new HashMap<String, FieldValidation>() ;

		for(FieldValidation fv : fieldValidationsList) {
			fv.setFldNm( StringUtils.replaceEach(fv.getFldNm(), 
					new String[]{"['", "']", "?", "[#QtCmtType", "]"},
					new String[]{".", "", "", "", ""}) );
			List<FieldValidationDetail> newFvdList = new ArrayList<FieldValidationDetail>() ;

			for (FieldValidationDetail fvd : fv.getValidations()) {
				String bitOpType = String.valueOf(fvd.getProperties().get("BITOP_TYP") );
				String ruleBitWiseChar = String.valueOf( fvd.getProperties().get("BIT_WISE_QT_CHAR") );
				if ("ALL".equals(bitOpType)) {
					if (StringUtils.isEmpty(ruleBitWiseChar) || 
						CommonUtil.AND(profileHeader.getBitWiseQtChar(), NumberUtils.toLong(ruleBitWiseChar ) ) ) {
						newFvdList.add(fvd) ;
					}				
				} else if ("ANY".equals(bitOpType)) {
					if (StringUtils.isEmpty(ruleBitWiseChar) || 
						CommonUtil.ANDANY(profileHeader.getBitWiseQtChar(), NumberUtils.toLong(ruleBitWiseChar ) ) ) {
						newFvdList.add(fvd) ;
					}						
				}		
			}
			
			FieldValidation newFv = filteredFieldValidationMap.get(fv.getFldNm()) ;
			if (newFv == null ) {
				newFv = new FieldValidation() ;
				newFv.setFldNm(fv.getFldNm());
				filteredFieldValidationMap.put(fv.getFldNm(), newFv);
			} 
			newFv.getValidations().addAll(newFvdList) ;
			
		}	
		
		for (String key : fieldValidationMap.keySet()) {
			FieldValidation newFv = filteredFieldValidationMap.get(key) ;
			if (newFv == null) {
				filteredFieldValidationMap.put(key, fieldValidationMap.get(key)) ;
			} else {
				filteredFieldValidationMap.get(key).getValidations().addAll(fieldValidationMap.get(key).getValidations()) ;
			}
		}

		return filteredFieldValidationMap ;
	}	

	@Override
	public Map<String, FieldValidation> getFieldValidationMapByPrfl(LocalizationFilter filterbase, Quote quote) {
		Map<String, FieldValidation> fieldValidationMap =   rulesCacheHandler.getAllStaticAnnotation () ;
		if (quote != null && quote.getQtPrchOrdReqt() != null && quote.getQtPrchOrdReqt().isSerpFlag()) {
			filterbase.setOmTypCd("SERP");	
		}
		List<FieldValidation> fieldValidationsList = checkListRepository.getAllRegexRules(filterbase);
		Map<String, FieldValidation> filteredFieldValidationMap = new HashMap<String, FieldValidation>() ;

		for(FieldValidation fv : fieldValidationsList) {
			fv.setFldNm( StringUtils.replaceEach(fv.getFldNm(), 
					new String[]{"['", "']", "?", "[#QtCmtType", "]"},
					new String[]{".", "", "", "", ""}) );
			List<FieldValidationDetail> newFvdList = new ArrayList<FieldValidationDetail>() ;

			for (FieldValidationDetail fvd : fv.getValidations()) {
				String bitOpType = String.valueOf(fvd.getProperties().get("BITOP_TYP") );
				String ruleBitWiseChar = String.valueOf( fvd.getProperties().get("BIT_WISE_QT_CHAR") );
				if ("ALL".equals(bitOpType)) {
					if (StringUtils.isEmpty(ruleBitWiseChar) || 
						CommonUtil.AND(quote.getBitWiseQtChar(), NumberUtils.toLong(ruleBitWiseChar ) ) ) {
						newFvdList.add(fvd) ;
					}				
				} else if ("ANY".equals(bitOpType)) {
					if (StringUtils.isEmpty(ruleBitWiseChar) || 
						CommonUtil.ANDANY(quote.getBitWiseQtChar(), NumberUtils.toLong(ruleBitWiseChar ) ) ) {
						newFvdList.add(fvd) ;
					}						
				}		
			}
			
			FieldValidation newFv = filteredFieldValidationMap.get(fv.getFldNm()) ;
			if (newFv == null ) {
				newFv = new FieldValidation() ;
				newFv.setFldNm(fv.getFldNm());
				filteredFieldValidationMap.put(fv.getFldNm(), newFv);
			} 
			newFv.getValidations().addAll(newFvdList) ;
			
		}	
		
		for (String key : fieldValidationMap.keySet()) {
			FieldValidation newFv = filteredFieldValidationMap.get(key) ;
			if (newFv == null) {
				filteredFieldValidationMap.put(key, fieldValidationMap.get(key)) ;
			} else {
				filteredFieldValidationMap.get(key).getValidations().addAll(fieldValidationMap.get(key).getValidations()) ;
			}
		}

		return filteredFieldValidationMap ;
	}
	

	
	/** Description of getFieldValidationMap()
	 * 
	 * This method will scan the Quote object and get all the fields and if the occurs the Valid Annotation.
	 * It will check for Class Type if Class its a Object if  ParameterizedTypeImpl then that object contain 
	 * more that one parameter. This method will return all the field validations from the DataBase and Quote
	 * Object.
	 * 
	 * @return			Map<String, FieldValidation> of fieldValidationMap
	 */

	//@PostConstruct
	@Cacheable(value =  "C_FIELD_VALIDATION" )
	@Override
	public Map<String, FieldValidation> getFieldValidationMap(LocalizationFilter filterbase) {
		Map<String, FieldValidation> fieldValidationMap = checkListRepository.getFieldValidationMap(filterbase);
		
		try {
			Quote ob = new Quote();
			Field[] allFields =	ob.getClass().getDeclaredFields();
			for(int i=0; i< allFields.length; i++ ){
				Field currentField  = allFields[i];
				Annotation[] annos = currentField.getAnnotations();
				
				 for(Annotation annotation : annos){
					 if(annotation instanceof Valid){
						 currentField.getGenericType();
						 Type type =  currentField.getGenericType();
						 if(type.getClass().getSimpleName().equals(Constants.FieldValidatorConstants.CLASS_TEXT)){
							 Field[] fields =  FieldValidationUtil.getDeclaredFields(currentField.getType(), true);
							 fieldValidationMap = FieldValidationUtil.getAnnotatedFieldValidationMap(fields, fieldValidationMap,currentField.getName());
						  }
						 if(type.getClass().getSimpleName().equals(Constants.FieldValidatorConstants.PARAMETERIZED_TYPE_TEXT)){
							 ParameterizedType parameterizedType = (ParameterizedType) currentField.getGenericType();
							 Type[] types =parameterizedType.getActualTypeArguments();
								if(types.length == 1){
									 Class<?> parameterizedType1 = (Class<?>) parameterizedType.getActualTypeArguments()[0];
									Field[] fields =  FieldValidationUtil.getDeclaredFields(parameterizedType1, true);
									fieldValidationMap =  FieldValidationUtil.getAnnotatedFieldValidationMap(fields, fieldValidationMap,currentField.getName());
								}else if(types.length == 2)
								{
									 if(parameterizedType.getActualTypeArguments()[0].getClass().getSimpleName().equals(Constants.FieldValidatorConstants.CLASS_TEXT)){
										 Class<?> parameterizedMapKeyType = (Class<?>) parameterizedType.getActualTypeArguments()[0];
										 if(parameterizedMapKeyType.isEnum()){
											 Class<?> parameterizedMapValueType = (Class<?>) parameterizedType.getActualTypeArguments()[1];
											 fieldValidationMap = FieldValidationUtil.FieldValidateMap(parameterizedMapKeyType,parameterizedMapValueType,currentField.getName(),fieldValidationMap);
										 }
									 }
									 
								 }
						 }
					 }else{
						 fieldValidationMap =  FieldValidationUtil.getAnnotatedFieldValidationMap(currentField, fieldValidationMap,currentField.getName());
					 }
				 }
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return fieldValidationMap;
	}


	@Override
	public boolean isWFMCaseNumberExists(String wfmCaseNum) {
		return 	checkListRepository.isWFMCaseNumberExists(wfmCaseNum);

	}
	
	@Override
	public IGenericDisplayResponse isWFMCaseNumberExists(Quote quote, FieldRule fieldRule) {
		FVOProcessResponse response = new FVOProcessResponse() ;
		boolean statusCode = (quote == null || quote.getQtPrchOrdReqt().getWfmCaseId() == null) ;
		if (!statusCode) {
			statusCode = !isWFMCaseNumberExists( quote.getQtPrchOrdReqt().getWfmCaseId() ) ;
		}
		response.setStatusCode(statusCode);
		return response ;

	}
	
	
	
	@Override
	public void loadEntryRuleResponseInQuote( Quote quote) {
		RulesFilter rFilter = new RulesFilter () ; 
		rFilter.setPrcssGrpCd("FVO_QD");
		if (quote.isAutoFlow()) {
			rFilter.setElectronicOrder(true);
		}
		UiComponentMap uiCompMap = this.getEntryRuleQuote(quote, rFilter);
		Gson gson = new Gson();
		processEntryRuleResponse(quote) ;	
		quote.getQtEntryResponse().setEntryRuleResponse(gson.toJson(uiCompMap));
		quote.getQtEntryResponse().setUpdated(true);
		
	}
	
	@Override
	public void getEntryRuleResponseFromQuote (Quote quote) {
		if (quote.getQtEntryResponse() != null ) {
			/*
			List<Quote> quoteList = new ArrayList<Quote>() ;
			quoteList.add(quote) ;
			Map<String,Quote> quoteMap = myWorkSpaceService.createMapOfQuotes( quoteList ) ;
			myWorkSpaceService.processEntryRuleResponse(quoteMap, new MyWorkspaceFilter(), quoteList);
			*/
			processEntryRuleResponse(quote);
			Gson gson = new Gson();
			UiComponentMap entryRuleResponsePersisted = gson.fromJson( quote.getQtEntryResponse().getEntryRuleResponse(),  UiComponentMap.class);
			quote.setUiCompMap(entryRuleResponsePersisted);
		}		
	}
	
	private void processEntryRuleResponse (Quote quote) {
		List<Quote> quoteList = new ArrayList<Quote>() ;
		quoteList.add(quote) ;
		Map<String,Quote> quoteMap = myWorkSpaceService.createMapOfQuotes( quoteList ) ;
		myWorkSpaceService.processEntryRuleResponse(quoteMap, new MyWorkspaceFilter(), quoteList);
	}
	
	@Override
	public List<QtEntryRuleResponse> getEntryRuleResponseForConversion(QuoteKeyInterface quote) {
		return checkListRepository.getEntryRuleResponseForConversion(quote);
	}
	
	public int persistEntryRuleResponseForConversion(QuoteKeyInterface quote, QtEntryRuleResponse qtEntryRuleResponse){
		return checkListRepository.persistEntryRuleResponseForConversion(quote, qtEntryRuleResponse);
	};
	
	
	@Override
	public List<QtEntryRuleResponse> getQuoteEntryRuleResponse(int batchCount) {
		return checkListRepository.getEntryRuleResponseForConversion(batchCount);
	}	
	
	@Override
	public IGenericDisplayResponse copyEndCustomerDataForValueIndirect(Quote quote, FieldRule fieldRule){
		FVOProcessResponse response = new FVOProcessResponse() ;
		QuoteCustomerAddress qidsEndCustomerAddress = quote.getCustomerAddresses().get(CustomerType.QIDSENDCUSTOMER) ;

		if (qidsEndCustomerAddress != null) {
			for (CustomerType customerType : quote.getCustomerAddresses().keySet() ) {
				if (CustomerType.ENDCUSTOMER.equals(  customerType )) {
					QuoteCustomerAddress endCustAddress = quote.getCustomerAddresses().get(CustomerType.ENDCUSTOMER) ;
					endCustAddress.setUpdated(true);
					endCustAddress.setCity(qidsEndCustomerAddress.getCity());
					endCustAddress.setCityarea(qidsEndCustomerAddress.getCityarea());
					endCustAddress.setCountry(qidsEndCustomerAddress.getCountry());		
					endCustAddress.setEmail(qidsEndCustomerAddress.getEmail());		
					endCustAddress.setPhone(qidsEndCustomerAddress.getPhone());
					endCustAddress.setPhoneExt(qidsEndCustomerAddress.getPhoneExt());
					endCustAddress.setPostalcode(qidsEndCustomerAddress.getPostalcode());
					endCustAddress.setRegion(qidsEndCustomerAddress.getRegion());
					endCustAddress.setState(qidsEndCustomerAddress.getState());
					endCustAddress.setStreet(qidsEndCustomerAddress.getStreet());
					endCustAddress.setStreet2(qidsEndCustomerAddress.getStreet2());
					endCustAddress.setStreet3(qidsEndCustomerAddress.getStreet3());
					endCustAddress.setName(qidsEndCustomerAddress.getName());
					endCustAddress.setFaxNo(qidsEndCustomerAddress.getFaxNo());
					endCustAddress.setMobileNo(qidsEndCustomerAddress.getMobileNo());
					QuoteCustomer quoteCustomer = quote.getCustomers().get(CustomerType.ENDCUSTOMER) ;
					quoteCustomer.setUpdated(true);
					quoteCustomer.setCompanyName(qidsEndCustomerAddress.getName());
					break;
				}

			}			
		}
		response.setStatusCode(true);
		return response ;
	}
	
	@Override
	public Quote getCrsIdForCustomerAddress(Quote quote){
		Map<CustomerType, QuoteCustomer> quoteCust = quote.getCustomers();
		Map<CustomerType, QuoteCustomerAddress> quoteCustAddr = quote.getCustomerAddresses();
		for (Map.Entry<CustomerType, QuoteCustomerAddress>  entryMap : quoteCustAddr.entrySet()){
			QuoteCustomerAddress quoteCustomerAddress = entryMap.getValue();
			QuoteCustomer quoteCustomer = (QuoteCustomer) MapUtils.getObject(quoteCust, entryMap.getKey());
			Long opsId = quoteCustomer.getOtrPrtySiteInsnId();
			if (opsId!= null && StringUtils.isEmpty(quoteCustomerAddress.getCrsId())){
				CustSearchCreateV3Request custSearchCreateV3Request = new CustSearchCreateV3Request();
				custSearchCreateV3Request.setOtherPartySiteInstanceId(opsId.toString());
				custSearchCreateV3Request.setRetOPOCustFlag("Y");
				LOG.debug("Start:MdcpCustomerSearchRestEndPoint:mdcpCustomerSearch");
				//US-MAR26 : mdcp code clean up - commented now - starts
				//CustomerSearchCreateV3Response responsecust = mdmServices.getCustomerSearchCrt(custSearchCreateV3Request);
				//quoteCustomer.setCrsId(responsecust.getCrossRefCrsID());
				//US-MAR26 : mdcp code clean up - commented now - ends
				LOG.debug("END:MdcpCustomerSearchRestEndPoint:mdcpCustomerSearch");		
			}
		}
	
		return quote;	
	}
	
	@Override
	public IGenericDisplayResponse copySoldToToCSP(Quote quote, FieldRule fieldRule){
		FVOProcessResponse response = new FVOProcessResponse() ;
		quote.getCstmAtr().setSlsOrgChanged(false);
		QuoteCustomer cspCustomer = quote.getCustomers().get(CustomerType.CSP) ;
		QuoteCustomerAddress cspCustomerAddr = quote.getCustomerAddresses().get(CustomerType.CSP) ;
		boolean existingCspCustomer = cspCustomer != null && BooleanUtils.isTrue( cspCustomer.getExisting() );
		boolean existingCspCustAddr = cspCustomerAddr != null && BooleanUtils.isTrue( cspCustomerAddr.getExisting() );
		frictionlessOrderService.copyCustomerTypes(CustomerType.SOLDTO, CustomerType.CSP, quote);
		if (!existingCspCustomer) {
			cspCustomer.setNewBean(true);
		}
		cspCustomer.setUpdated(true);
		
		if (!existingCspCustAddr) {
			cspCustomerAddr.setNewBean(true);
		}
		cspCustomerAddr.setUpdated(true);
		
		response.setStatusCode(true);
		return response ;
	}

@Override
public IGenericDisplayResponse isOverrideCustomerToData(Quote quote, FieldRule fieldRule){
	
	FVOProcessResponse response = new FVOProcessResponse() ;
	
	boolean isOverride=false;
	
	QuoteCustomerAddress shiptoAddress=null;
	QuoteCustomerAddress origShiptoAddress=null;
	QuoteCustomer shiptoCustomer=null;
	QuoteCustomer origShiptoCustomer=null;
	
	shiptoAddress=quote.getCustomerAddresses().get(CustomerType.SHIPTO);
	origShiptoAddress=quote.getCustomerAddresses().get(CustomerType.ORGSHIPTO);
	
	shiptoCustomer=quote.getCustomers().get(CustomerType.SHIPTO);
	origShiptoCustomer=quote.getCustomers().get(CustomerType.ORGSHIPTO);
	
	if(!StringUtils.equalsIgnoreCase(shiptoAddress.getCity(), origShiptoAddress.getCity()) || 
		!StringUtils.equalsIgnoreCase(shiptoAddress.getStreet(), origShiptoAddress.getStreet()) || 
		!StringUtils.equalsIgnoreCase(shiptoAddress.getCountry(), origShiptoAddress.getCountry()) ||
		!StringUtils.equalsIgnoreCase(shiptoAddress.getPostalcode(), origShiptoAddress.getPostalcode()) ||
		!StringUtils.equalsIgnoreCase(shiptoCustomer.getCompanyName(), origShiptoCustomer.getCompanyName()) ||
		!StringUtils.equalsIgnoreCase(shiptoCustomer.getCompanyName2(), origShiptoCustomer.getCompanyName2()) ||
		!StringUtils.equalsIgnoreCase(shiptoCustomer.getCompanyName3(), origShiptoCustomer.getCompanyName3()) ||
		!StringUtils.equalsIgnoreCase(shiptoCustomer.getCompanyName4(), origShiptoCustomer.getCompanyName4()) ||
		!StringUtils.equalsIgnoreCase(shiptoCustomer.getCrsId(), origShiptoCustomer.getCrsId()) ||
		!StringUtils.equalsIgnoreCase(shiptoCustomer.getVatRegistrationNo(), origShiptoCustomer.getVatRegistrationNo())){

		isOverride=true;
	}
	
	if(isOverride && (shiptoAddress.getStreet()!=null && shiptoAddress.getStreet().length()>35)){
		response.setStatusCode(false);
	}else{
		response.setStatusCode(true);
	}
	
	return response;
}


@Override
public List<CustomerPoNrMapping> getCstmrPoNrMappingFromDb(Quote quote) {
	return checkListRepository.getCstmrPoNrMappingFromDb(quote);
private static final Logger LOG = LoggerFactory.getLogger(ChecklistServiceImpl.class);

    private final S4DealNumberValidationService s4DealNumberValidationServiceImpl;

    public ChecklistServiceImpl(S4DealNumberValidationService s4DealNumberValidationServiceImpl) {
        this.s4DealNumberValidationServiceImpl = s4DealNumberValidationServiceImpl;
    }

    /**
     * This method processes and validates the Deal Version Number for the given quote.
     * Adds validation errors to the provided ApplyRuleResponse if the Quote Deal Version Number mismatches with S4 Deal Version Number.
     *
     * @param quote             the Quote object containing deal and version information
     * @param applyRuleResponse the ApplyRuleResponse to which validation errors will be added
     */
    @Transactional(readOnly = true)
    public void processAndValidateDealVersionNr(Quote quote, ApplyRuleResponse applyRuleResponse) {
        String dealNr = quote.getDealNr();
        LOG.debug("ChecklistServiceImpl.processAndValidateDealVersionNr method dealNr: {}", dealNr);
        String dealVersionNrStr = "";
        Integer qidsDealVersionNr = null;
        try {
            dealVersionNrStr = quote.getDealVersionNr();
            if (StringUtils.isNotBlank(dealVersionNrStr)) {
                qidsDealVersionNr = Integer.parseInt(dealVersionNrStr);
            }
        } catch (NumberFormatException e) {
            LOG.error("Invalid deal version number: {}", quote.getDealVersionNr(), e);
        }
        String jsonPayload = "";
        String exceptionMsg = "";

        if (StringUtils.isNotBlank(dealNr)) {
            jsonPayload = s4DealNumberValidationServiceImpl.getS4DealNumberResponse(dealNr);
            if (StringUtils.isBlank(jsonPayload)) {
                LOG.warn("No response received from S4 for deal number: {}", dealNr);
                exceptionMsg = "No response received from S4 for deal number: " + dealNr;
                ResponseException re = new ResponseException("", ApplicationDomainType.OMUI, 2021, ExceptionSeverity.Error_Save_Allowed, exceptionMsg, "");
                applyRuleResponse.getResponseExceptionList().add(re);
                applyRuleResponse.setQuote(quote);
                applyRuleResponse.setMaxSeverity(ExceptionSeverity.Error_Save_Allowed.toString());
                return;
            }
            LOG.info("DealNrS4Addison Call JSON Response: {}", jsonPayload);

            Map<String, List<String>> dealVersionsMap = CommonUtil.extractDealVersionNrs(jsonPayload);
            Integer s4LatestDealVersionNr = CommonUtil.getMaxDealVersionNr(dealVersionsMap, dealNr);

            //checking - if in case both values are null - edge case scenario
            if (s4LatestDealVersionNr == null && qidsDealVersionNr == null) {
                LOG.warn("Qidsquote has deal version : {}, latest deal version in S4 is : {}", qidsDealVersionNr, s4LatestDealVersionNr);
                exceptionMsg = "Qidsquote has deal version : " + qidsDealVersionNr + ", latest deal version in S4 is : " + s4LatestDealVersionNr;
                if (StringUtils.isNotEmpty(exceptionMsg)) {
                    ResponseException re = new ResponseException("", ApplicationDomainType.OMUI, 2021, ExceptionSeverity.Error_Save_Allowed, exceptionMsg, "");
                    applyRuleResponse.getResponseExceptionList().add(re);
                    applyRuleResponse.setQuote(quote);
                    applyRuleResponse.setMaxSeverity(ExceptionSeverity.Error_Save_Allowed.toString());
                }
                return;
            }

            //checking - if both values are not equal
            if (!Objects.equals(s4LatestDealVersionNr, qidsDealVersionNr)) {
                LOG.warn("Qidsquote has deal version : {}, latest deal version in S4 is : {}", qidsDealVersionNr, s4LatestDealVersionNr);
                exceptionMsg = "Qidsquote has deal version : " + qidsDealVersionNr + ", latest deal version in S4 is : " + s4LatestDealVersionNr;
                if (StringUtils.isNotEmpty(exceptionMsg)) {
                    ResponseException re = new ResponseException(quote.getAssetQuoteNrAndVrsn(), ApplicationDomainType.OMUI, 2021, ExceptionSeverity.Error_Save_Allowed, exceptionMsg, "");
                    applyRuleResponse.getResponseExceptionList().add(re);
                    applyRuleResponse.setQuote(quote);
                    applyRuleResponse.setMaxSeverity(ExceptionSeverity.Error_Save_Allowed.toString());
                }
            }
        }
    }
}

	@Override
	public IGenericDisplayResponse setFlagPoNrAlreadyExist(Quote quote, FieldRule fieldRule) {
		FVOProcessResponse response = new FVOProcessResponse();
		List<CustomerPoNrMapping> list = getCstmrPoNrMappingFromDb(quote);

		QtFlag flagObject = quote.getFlags().get(QtFlagType.PONRDUPLICATEFLAG);
		if (flagObject == null) {
			flagObject = new QtFlag();
		}
		flagObject.setQtFlagType(QtFlagType.PONRDUPLICATEFLAG);

		if (list.size() > 0) {
			flagObject.setFlgVl("true");
		} else {
			flagObject.setFlgVl("false");
		}
		flagObject.setUpdated(true);

		quote.getFlags().put(QtFlagType.PONRDUPLICATEFLAG, flagObject);

		response.setStatusCode(true);
		return response;
	}
	
	@Override
	public IGenericDisplayResponse setQuotePropertiesBasedonFinModel(Quote quote, FieldRule fieldRule) {
		FVOProcessResponse response = new FVOProcessResponse();
		List<FinancialModel> returnList = null;

		if (StringUtils.isNotEmpty(quote.getRtm())) {
			LocalizationFilter filterbase = RuleUtil.getLocalizationFilterByQuote(quote);
		/*	
			if (!quote.isAutoFlow()) {
				filterbase = appService.getLocalizationFilterByCookies();
			} else {
				filterbase = RuleUtil.getLocalizationFilterByQuote(quote);
			}
			*/
			filterbase.setRtm(quote.getRtm());
			returnList = frictionlessOrderService.getFinancialModelList(filterbase);
		}

		if (returnList != null) {
			for (FinancialModel finModel : returnList) {
				if (StringUtils.equalsIgnoreCase(quote.getQtPrchOrdReqt().getFnModelGrpUUID(),
						finModel.getFnModelUUID())) {
					quote.getQtPrchOrdReqt().setOrdTyp(finModel.getOrdTyp());
					quote.getQtPrchOrdReqt().setIdCd(finModel.getIdCd());
					quote.getQtPrchOrdReqt().setReasonCd(finModel.getReasonCd());
					break;
				}
			}
		}

		response.setStatusCode(true);
		return response;

	}	
	
	@Override
	public IGenericDisplayResponse checkForInternalDemoInvalidSupplyingDiv(Quote quote, FieldRule fieldRule) {
		ProcessCheckResponse pcResponse = new ProcessCheckResponse() ;
		List<ProductProcessActions> productProcessActions=new ArrayList<ProductProcessActions>();
		if (StringUtils.equalsIgnoreCase(quote.getQtPrchOrdReqt().getFnModelUUID(), "INTRNL_DEMO")
				|| StringUtils.equalsIgnoreCase(quote.getQtPrchOrdReqt().getFnModelUUID(), "INTRNL_DEMO_APJ")
				|| StringUtils.equalsIgnoreCase(quote.getQtPrchOrdReqt().getFnModelUUID(), "AMS_INTRNL_DEMO")) {

			LocalizationFilter filterbase = RuleUtil.getLocalizationFilterByQuote(quote);
			/*
			if (!quote.isAutoFlow()) {
				filterbase = appService.getLocalizationFilterByCookies();
			} else {
				filterbase = RuleUtil.getLocalizationFilterByQuote(quote);
			}
			*/
			
			List<String> internalDemoInvalidFusionSupplierList = frictionlessOrderService
					.getInternalDemoInvalidFusionSupplierList(filterbase);
			boolean found = false ;
			if (internalDemoInvalidFusionSupplierList != null && !internalDemoInvalidFusionSupplierList.isEmpty()) {
				for (QuoteItem eachItem : quote.getItems()) {
					if (internalDemoInvalidFusionSupplierList.contains(eachItem.getSupplyingDiv())) {
						ProductProcessActions ppa = new ProductProcessActions();
						ppa.setProdNr(eachItem.getProductId()) ;
						ppa.setSlsQtnItmSqnNr( eachItem.getHeartLineItemNr() ) ;
						productProcessActions.add(ppa) ;
						ppa.setSupplyingDivOrMCC(eachItem.getSupplyingDiv()) ;
						found = true ;
					}
				}

				if (found) {
					pcResponse.setProcessCheckActionsList(productProcessActions) ;
					pcResponse.setMaxSeverity(fieldRule.getSvrtyCd() == null ? "" : fieldRule.getSvrtyCd().toString()) ;
				}

			}

		}

		return pcResponse;

	}

	@Override
	public IGenericDisplayResponse checkForInternalDemoProductLine(Quote quote, FieldRule fieldRule) {
		ProcessCheckResponse pcResponse = new ProcessCheckResponse() ;
		List<ProductProcessActions> productProcessActions=new ArrayList<ProductProcessActions>();

			LocalizationFilter filterbase = RuleUtil.getLocalizationFilterByQuote(quote);
			/*
			if (!quote.isAutoFlow()) {
				filterbase = appService.getLocalizationFilterByCookies();
			} else {
				filterbase = RuleUtil.getLocalizationFilterByQuote(quote);
			}
			*/
			filterbase.setRtm(quote.getRtm());
			List<String> internalDemoInvalidProductLineList = frictionlessOrderService
					.getInternalDemoInvalidProductLineList(filterbase);

			if (internalDemoInvalidProductLineList != null && !internalDemoInvalidProductLineList.isEmpty()) {
				boolean found = false ;
				for (QuoteItem item : quote.getItems()) {
					if (internalDemoInvalidProductLineList.contains(StringUtils.upperCase(item.getProductLine()))) {
						ProductProcessActions ppa = new ProductProcessActions();
						ppa.setProdNr(item.getProductId()) ;
						ppa.setSlsQtnItmSqnNr( item.getHeartLineItemNr() ) ;
						ppa.setSupplyingDivOrMCC(item.getProductLine()) ;
						found = true ;
						productProcessActions.add(ppa) ;
					}

				}
				if (found) {
					pcResponse.setProcessCheckActionsList(productProcessActions) ;
					pcResponse.setMaxSeverity(fieldRule.getSvrtyCd() == null ? "" : fieldRule.getSvrtyCd().toString()) ;
				}
			}
		

		return pcResponse;

	}
	
	@Override
	public IGenericDisplayResponse setQuotePropertiesBasedonInternalDemo(Quote quote, FieldRule fieldRule) {
		FVOProcessResponse response = new FVOProcessResponse();
		if (StringUtils.equalsIgnoreCase(quote.getQtPrchOrdReqt().getFnModelUUID(), "INTRNL_DEMO")
				|| StringUtils.equalsIgnoreCase(quote.getQtPrchOrdReqt().getFnModelUUID(), "INTRNL_DEMO_APJ")
				|| StringUtils.equalsIgnoreCase(quote.getQtPrchOrdReqt().getFnModelUUID(), "AMS_INTRNL_DEMO")) {
		/*	QtFlag qtFlag = quote.getFlags().get(QtFlagType.SPLITAPPLICABLE);
			if (qtFlag != null) {
				quote.getFlags().get(QtFlagType.SPLITAPPLICABLE).setFlgVl("false");
				quote.getFlags().get(QtFlagType.SPLITAPPLICABLE).setUpdated(true);*/
				if ("true".equalsIgnoreCase(quote.getQtPrchOrdReqt().getFnModelChanged()) || "true".equalsIgnoreCase(quote.getQtPrchOrdReqt().getReasonCdChanged())) {
					List<InternalDemoModel> internalDemoCbnList = new ArrayList<InternalDemoModel>();
					internalDemoCbnList = frictionlessOrderService.getIDODefaultData();
					appService.getOrderMgmtApplnType(quote).integrateInternalDemoOrderData(internalDemoCbnList, quote);
				}

		//	}
		}

		return response;

	}
	
	@Override
	public IGenericDisplayResponse setInternalDemoValues4ApplyCBN(Quote quote, FieldRule fieldRule) {
		FVOProcessResponse response = new FVOProcessResponse();
		dropDownListService.getInternalDemoOrderCBNInfo(quote);
		return response;
	}
	

	@Override
	public IGenericDisplayResponse setContractStartAndEndDate(Quote quote, FieldRule fieldRule){
		FVOProcessResponse response = new FVOProcessResponse();
		qidsQuoteService.setContractStartAndEndDate(quote);
		return response;
	}

	@Override
	public IGenericDisplayResponse setPostCBNSetters(Quote quote, FieldRule fieldRule){
			dropDownListService.getSalesorgdataBasedonCBN(quote);
		return determinePOCategory (quote, fieldRule);
	}
	
	@Override
	public IGenericDisplayResponse determinePOCategory(Quote quote, FieldRule fieldRule) {
		FVOProcessResponse response = new FVOProcessResponse();
		quoteService.getOMHandler(quote).setPOCategory(quote);
		return response;
	}


	@Override
	public IGenericDisplayResponse setContextualQuoteDefaults(Quote quote, FieldRule fieldRule) {
		FVOProcessResponse response = new FVOProcessResponse();
		IOrderMgmtServices orderMgmtServices = null ;
		if (quote.isAutoFlow()) {
			LocalizationFilter filterbase = RuleUtil.getLocalizationFilterByQuote(quote);
			orderMgmtServices = appService.getOrderMagementApplicationType(  ApplTypeType.FLOM.getCode(), filterbase);
		} else {
			orderMgmtServices = appService.getOrderMgmtApplnType(quote) ;
		}
		if ( orderMgmtServices != null) {
			orderMgmtServices.setContextualQuoteDefaults(quote);
		}
		
		orderMgmtServices = null ;
		
		
		return response;
	}


/*	@Override
	public IGenericDisplayResponse setRequestedDeliveryDateForS4BisnessModel(Quote quote, FieldRule fieldRule) {
		FVOProcessResponse response = new FVOProcessResponse();
		if (StringUtils.equalsIgnoreCase(quote.getQtPrchOrdReqt().getFnModelUUID(), "S4_POSRQ")
				|| StringUtils.equalsIgnoreCase(quote.getQtPrchOrdReqt().getFnModelGrpUUID(), "S4_POSRQ")) {
			qidsQuoteService.setRequestedDeliveryDateForS4BisnessModel(quote);
		}
		return response;
	}
	*/
	@Override
	public IGenericDisplayResponse checkForEDIPoHeaderData(Quote quote, FieldRule fieldRule) {
		FVOProcessResponse response = new FVOProcessResponse();
		boolean statusCode = (quote == null || quote.getPoHederDataModel() == null);
		if (!statusCode && (quote.getPoHederDataModel().isExisting())) {
			response.setDisplayAlways(true);
			response.setStatusCode(true);
			String msg=checkEDIPoHeader(quote);
			if(StringUtils.length(msg) > 0) {
				QtCmt poHeaderComment=new QtCmt();
				poHeaderComment.setSlsQtnId(quote.getSlsQtnId());
				poHeaderComment.setSlsQtnRvsnSqnNr(quote.getSlsQtnRvsnSqnNr());
				poHeaderComment.setSlsQtnVrsnSqnNr(quote.getSlsQtnVrsnSqnNr());
				int lastIndexOf = msg.charAt(msg.length()-1);
				if(',' ==(char) lastIndexOf) {
					poHeaderComment.setCmtTxt1("["+msg.substring(0, msg.length()-1)+"]");
				}else {
					poHeaderComment.setCmtTxt1("["+msg+"]");
				}
				poHeaderComment.setQtCmtType(QtCmtType.QTVSPOHEADER);
				poHeaderComment.setNewBean(true);
				poHeaderComment.setUpdated(true);
				quote.getComments().put(QtCmtType.QTVSPOHEADER,poHeaderComment);
				
			}
			if(StringUtils.isNotEmpty(msg))
			{
				fieldRule.setMsgId(msg);
				response.setMaxSeverity(fieldRule.getSvrtyCd() == null ? "" : fieldRule.getSvrtyCd().toString());
			}
		}else if(quote.getComments().containsKey(QtCmtType.QTVSPOHEADER) &&
				StringUtils.isNotBlank(quote.getComments().get(QtCmtType.QTVSPOHEADER).getCmtTxt1())){
			response.setDisplayAlways(true);
			response.setStatusCode(true);
			response.setStatusCode(true);			
			response.setExceptionCode(StringUtils.isEmpty(quote.getComments().get(QtCmtType.QTVSPOHEADER).getCmtTxt1())? "" : "BAE");
			response.setExceptionMsg(quote.getComments().get(QtCmtType.QTVSPOHEADER).getCmtTxt1());
			response.setMaxSeverity(StringUtils.isEmpty(quote.getComments().get(QtCmtType.QTVSPOHEADER).getCmtTxt1())? "" : fieldRule.getSvrtyCd().toString());
		
		}

		return response;
	}


	private String prepareTableResponse(String poHeaderValue,String ngqQuoteHeader,String fldName) {
		String value="{\"fldName\":\""+fldName+"\",\"ediPoValue\":\""+(StringUtils.isBlank(poHeaderValue)?"BLANK":poHeaderValue)+"\",\"quotePoValue\":\""+(StringUtils.isBlank(ngqQuoteHeader)?"BLANK":ngqQuoteHeader)+"\"}";
		return value;
	}
	private String checkEDIPoHeaderOld(Quote quote) {
		StringBuilder validationmsg = new StringBuilder();

		if (!(StringUtils.startsWith(quote.getAssetQuoteNr(), "D") && StringUtils.equalsIgnoreCase("AP", quote.getRegionCd()) && StringUtils.equalsIgnoreCase("CN", quote.getCountryCd()) 
				&& "H3C".equalsIgnoreCase(quote.getQtPrchOrdReqt().getSourceSystemPartnerId()))
				&& !StringUtils.equalsIgnoreCase(quote.getPaNr(),quote.getPoHederDataModel().getPoPaNr())) {
			validationmsg.append(prepareTableResponse(quote.getPaNr(),quote.getPoHederDataModel().getPoPaNr(),"PA Number")+",");
		}
		if (!StringUtils.equalsIgnoreCase(quote.getSalesOrg(),quote.getPoHederDataModel().getPoSalesOrg())) {
			validationmsg.append(
					prepareTableResponse(quote.getSalesOrg(),quote.getPoHederDataModel().getPoSalesOrg(),"Sales Organization")+",");
		}
		if (!(StringUtils.startsWith(quote.getAssetQuoteNr(), "D")
				&& StringUtils.equalsIgnoreCase("AP", quote.getRegionCd())
				&& StringUtils.equalsIgnoreCase("CN", quote.getCountryCd()) 
				&& "H3C".equalsIgnoreCase(quote.getQtPrchOrdReqt().getSourceSystemPartnerId()))
				&& !StringUtils.equalsIgnoreCase(quote.getDealNr(),quote.getPoHederDataModel().getPoDealNr())) {
			validationmsg.append(prepareTableResponse(quote.getDealNr(),quote.getPoHederDataModel().getPoDealNr(),"Deal ID")+",");
		}
		
		if (!StringUtils.equalsIgnoreCase(((QuoteCustomer) MapUtils.getObject(quote.getCustomers(), CustomerType.SOLDTO))
				.getPartyId() , quote.getPoHederDataModel().getPoSoldToPartyId())) {
			validationmsg.append(prepareTableResponse(((QuoteCustomer) MapUtils.getObject(quote.getCustomers(), CustomerType.SOLDTO))
					.getPartyId(),quote.getPoHederDataModel().getPoSoldToPartyId(),"SoldTo/Dist Party Id")+",");
		}
		
		if (!StringUtils.equalsIgnoreCase(((QuoteCustomer) MapUtils.getObject(quote.getCustomers(), CustomerType.PAYER))
				.getPartyId() , quote.getPoHederDataModel().getPoSoldToPartyId())) {
			validationmsg.append(prepareTableResponse(((QuoteCustomer) MapUtils.getObject(quote.getCustomers(), CustomerType.PAYER))
					.getPartyId(),quote.getPoHederDataModel().getPoPayerPartyId(),"Payer Party Id"));
		}
		
		if(!StringUtils.equalsIgnoreCase(quote.getQtPrchOrdReqt().getPoCategory(), "STCK"))
		{
			if(quote.getCustomers().get(CustomerType.ENDCUSTOMER)!=null 
					&& ((QuoteCustomer) MapUtils.getObject(quote.getCustomers(), CustomerType.ENDCUSTOMER)) !=null
					&& ((QuoteCustomer) MapUtils.getObject(quote.getCustomers(), CustomerType.ENDCUSTOMER)).getPartyId() !=null){
				if (!StringUtils.equalsIgnoreCase(
						((QuoteCustomer) MapUtils.getObject(quote.getCustomers(), CustomerType.ENDCUSTOMER)).getPartyId(),
						quote.getPoHederDataModel().getEndCustomerPartyId())) {
					validationmsg.append(prepareTableResponse(
							((QuoteCustomer) MapUtils.getObject(quote.getCustomers(), CustomerType.ENDCUSTOMER)).getPartyId(),
							quote.getPoHederDataModel().getEndCustomerPartyId(), "EndCustomer PartyID") + ",");
				}
			}
			
			else {
				if(quote.getCustomers().get(CustomerType.ENDCUSTOMER)!=null 
						&& ((QuoteCustomer) MapUtils.getObject(quote.getCustomers(), CustomerType.ENDCUSTOMER))!=null 
						&& ((QuoteCustomer) MapUtils.getObject(quote.getCustomers(), CustomerType.ENDCUSTOMER)).getCompanyName()!=null){
			if(!StringUtils.equalsIgnoreCase(
					((QuoteCustomer) MapUtils.getObject(quote.getCustomers(), CustomerType.ENDCUSTOMER)).getCompanyName(),
					quote.getPoHederDataModel().getEndCustomerCompany1())) {
					validationmsg.append(prepareTableResponse(((QuoteCustomer) MapUtils.getObject(quote.getCustomers(), CustomerType.ENDCUSTOMER)).getCompanyName(),
						quote.getPoHederDataModel().getEndCustomerCompany1(), "EndCustomer Company1") + ",");
					}
				}
				if(quote.getCustomerAddresses().get(CustomerType.ENDCUSTOMER)!=null
						&& ((QuoteCustomerAddress) MapUtils.getObject(quote.getCustomerAddresses(),CustomerType.ENDCUSTOMER)) !=null 
						&& ((QuoteCustomerAddress) MapUtils.getObject(quote.getCustomerAddresses(),CustomerType.ENDCUSTOMER)).getCountry()!=null){
				if (!StringUtils.equalsIgnoreCase(
						((QuoteCustomerAddress) MapUtils.getObject(quote.getCustomerAddresses(), CustomerType.ENDCUSTOMER)).getCountry(),
						quote.getPoHederDataModel().getEndCustomerCountry())) {
						validationmsg.append(prepareTableResponse(((QuoteCustomerAddress) MapUtils.getObject(quote.getCustomerAddresses(), CustomerType.ENDCUSTOMER))
											.getCountry(),quote.getPoHederDataModel().getEndCustomerCountry(), "EndCustomer Country") + ",");
				}
			  }
			}
		
					
			if (StringUtils.equalsIgnoreCase(quote.getRtm(), "VALUE_INDIRECT")) {
				
				if(quote.getCustomers().get(CustomerType.RESELLER)!=null 
						&& ((QuoteCustomer) MapUtils.getObject(quote.getCustomers(), CustomerType.RESELLER)) !=null
						&& ((QuoteCustomer) MapUtils.getObject(quote.getCustomers(), CustomerType.RESELLER)).getPartyId() !=null){
					if (!StringUtils.equalsIgnoreCase(
							((QuoteCustomer) MapUtils.getObject(quote.getCustomers(), CustomerType.RESELLER)).getPartyId(),
							quote.getPoHederDataModel().getResellerPartyId())) {
						validationmsg.append(prepareTableResponse(
								((QuoteCustomer) MapUtils.getObject(quote.getCustomers(), CustomerType.RESELLER)).getPartyId(),
								quote.getPoHederDataModel().getResellerPartyId(), "Reseller PartyID") + ",");
					}
				}
				
				else {
					if(quote.getCustomers().get(CustomerType.RESELLER)!=null 
						&& ((QuoteCustomer) MapUtils.getObject(quote.getCustomers(), CustomerType.RESELLER))!=null 
						&& ((QuoteCustomer) MapUtils.getObject(quote.getCustomers(), CustomerType.RESELLER)).getCompanyName()!=null){
							
						if(!StringUtils.equalsIgnoreCase(
						((QuoteCustomer) MapUtils.getObject(quote.getCustomers(), CustomerType.RESELLER)).getCompanyName(),
						quote.getPoHederDataModel().getResellerCompany1())) {
							validationmsg.append(prepareTableResponse(
							((QuoteCustomer) MapUtils.getObject(quote.getCustomers(), CustomerType.RESELLER))
									.getCompanyName(),
							quote.getPoHederDataModel().getResellerCompany1(), "Reseller Company1") + ",");
							}
						}
					if(quote.getCustomerAddresses().get(CustomerType.RESELLER)!=null
							&& ((QuoteCustomerAddress) MapUtils.getObject(quote.getCustomerAddresses(),CustomerType.RESELLER)) !=null 
							&& ((QuoteCustomerAddress) MapUtils.getObject(quote.getCustomerAddresses(),CustomerType.RESELLER)).getCountry()!=null){
						
					if (!StringUtils.equalsIgnoreCase(
							((QuoteCustomerAddress) MapUtils.getObject(quote.getCustomerAddresses(),CustomerType.RESELLER)).getCountry(),
							quote.getPoHederDataModel().getResellerCountry())) {
							validationmsg.append(prepareTableResponse(((QuoteCustomerAddress) MapUtils.getObject(quote.getCustomerAddresses(),
							CustomerType.RESELLER)).getCountry(),quote.getPoHederDataModel().getResellerCountry(), "Reseller Country") + ",");
					}
				  }
				}
			  }
			}
		if (StringUtils.equalsIgnoreCase(quote.getRtm(), "VALUE_INDIRECT")) {  /*US-18038 validates presence of distributor info including partId
																					company names and country details within quote object*/
			if (!(quote.getPoHederDataModel().isDistributorCustomerNotExist()
					&& StringUtils.startsWith(quote.getAssetQuoteNr(), "D"))) {
				if (quote.getCustomers().get(CustomerType.DISTRIBUTORINFO) != null
						&& ((QuoteCustomer) MapUtils.getObject(quote.getCustomers(), CustomerType.DISTRIBUTORINFO)) != null
						&& ((QuoteCustomer) MapUtils.getObject(quote.getCustomers(), CustomerType.DISTRIBUTORINFO)).getPartyId() != null) {
					if (!StringUtils.equalsIgnoreCase(
							((QuoteCustomer) MapUtils.getObject(quote.getCustomers(), CustomerType.DISTRIBUTORINFO)).getPartyId(),
							quote.getPoHederDataModel().getDistributorPartyId())) {
						validationmsg.append(prepareTableResponse(
								((QuoteCustomer) MapUtils.getObject(quote.getCustomers(), CustomerType.DISTRIBUTORINFO)).getPartyId(),
								quote.getPoHederDataModel().getDistributorPartyId(), "Distributor PartyID") + ",");
					}
				} else {
					/*
					 *  //US-18038 QIDS Distributor company Name is compared with
					 *  //US-18038 NQGC Distributor company Names(1,2,3,4)
					 */

					String distributorCompanyName = null;
					if (quote.getCustomers().get(CustomerType.DISTRIBUTORINFO) != null
							&& ((QuoteCustomer) MapUtils.getObject(quote.getCustomers(), CustomerType.DISTRIBUTORINFO)) != null
							&& ((QuoteCustomer) MapUtils.getObject(quote.getCustomers(), CustomerType.DISTRIBUTORINFO)).getCompanyName() != null) {
						List<String> fuzzyString = frictionlessOrderService.getFuzzyString("POHEADER");

						distributorCompanyName = quoteService.getConsolidatedCompanyName(CustomerType.DISTRIBUTORINFO, quote);
						// US-16980 NGQC COMPARISON method call with additional argument synonymMap
						if (!CommonUtil.fuzzyEqualsIgnoreCase(distributorCompanyName,
								quote.getPoHederDataModel().getDistributorCompany1(), fuzzyString, frictionlessOrderService.getSynonym2Map())) {
							validationmsg.append(prepareTableResponse(distributorCompanyName,
									quote.getPoHederDataModel().getDistributorCompany1(), "Distributor Company") + ",");
						}
					}
					if (quote.getCustomerAddresses().get(CustomerType.DISTRIBUTORINFO) != null
							&& ((QuoteCustomerAddress) MapUtils.getObject(quote.getCustomerAddresses(), CustomerType.DISTRIBUTORINFO)) != null
							&& ((QuoteCustomerAddress) MapUtils.getObject(quote.getCustomerAddresses(), CustomerType.DISTRIBUTORINFO)).getCountry() != null) {

						if (!StringUtils.equalsIgnoreCase(
								((QuoteCustomerAddress) MapUtils.getObject(quote.getCustomerAddresses(), CustomerType.DISTRIBUTORINFO)).getCountry(),
								quote.getPoHederDataModel().getDistributorCountry())) {
							validationmsg.append(prepareTableResponse(((QuoteCustomerAddress) MapUtils.getObject(quote.getCustomerAddresses(),
									CustomerType.DISTRIBUTORINFO)).getCountry(), quote.getPoHederDataModel().getDistributorCountry(), "Distributor Country") + ",");
						}
					}
				}
			}
		}
		return validationmsg.toString();
	}
	
	private String checkEDIPoHeader(Quote quote) {
		StringBuilder validationmsg = new StringBuilder();
		
		QuoteCustomer endCustomer = (QuoteCustomer) MapUtils.getObject(quote.getCustomers(), CustomerType.ENDCUSTOMER);
		QuoteCustomer resellerCustomer = (QuoteCustomer) MapUtils.getObject(quote.getCustomers(), CustomerType.RESELLER);
		QuoteCustomer distributorCustomer = (QuoteCustomer) MapUtils.getObject(quote.getCustomers(), CustomerType.DISTRIBUTORINFO);  //US-18038
		if (!(StringUtils.startsWith(quote.getAssetQuoteNr(), "D")
				&& StringUtils.equalsIgnoreCase("AP", quote.getRegionCd())
				&& "H3C".equalsIgnoreCase(quote.getQtPrchOrdReqt().getSourceSystemPartnerId()))
				&& !StringUtils.equalsIgnoreCase(quote.getDealNr(),quote.getPoHederDataModel().getPoDealNr())) {
			validationmsg.append(prepareTableResponse(quote.getDealNr(),quote.getPoHederDataModel().getPoDealNr(),"Deal ID")+",");
		}
		
		if(!StringUtils.equalsIgnoreCase(quote.getQtPrchOrdReqt().getOrigPOCategory(), "STCK")) //US-18175
		{
			if(!(quote.getPoHederDataModel().isEndCustomerNotExist() && StringUtils.startsWith(quote.getAssetQuoteNr(), "D"))){
			if(quote.getCustomers().get(CustomerType.ENDCUSTOMER)!=null 
					&& endCustomer !=null
					&& endCustomer.getPartyId() !=null){
				if (!StringUtils.equalsIgnoreCase(
						((QuoteCustomer) MapUtils.getObject(quote.getCustomers(), CustomerType.ENDCUSTOMER)).getPartyId(),
						quote.getPoHederDataModel().getEndCustomerPartyId())) {
					validationmsg.append(prepareTableResponse(
							endCustomer.getPartyId(),
							quote.getPoHederDataModel().getEndCustomerPartyId(), "EndCustomer PartyID") + ",");
				}
			}
			
			else {
				/*
				 * US-16980 NGQC COMPARISON
				 * QIDS End Customer company Name is compared with 
				 * NQGC End Customer company Names(1,2,3,4) 
				 */
				String endCustCompanyName = null;
				if(quote.getCustomers().get(CustomerType.ENDCUSTOMER)!=null 
						&& endCustomer !=null 
						&& endCustomer.getCompanyName()!=null){
					List<String> fuzzyString = frictionlessOrderService.getFuzzyString("POHEADER");
					
					endCustCompanyName =  quoteService.getConsolidatedCompanyName(CustomerType.ENDCUSTOMER, quote);
			// US-16980 NGQC COMPARISON method call with additional argument synonymMap		
			if(!CommonUtil.fuzzyEqualsIgnoreCase(endCustCompanyName, 
				quote.getPoHederDataModel().getEndCustomerCompany1(), fuzzyString, frictionlessOrderService.getSynonym2Map())) {
					validationmsg.append(prepareTableResponse(endCustCompanyName,
						quote.getPoHederDataModel().getEndCustomerCompany1(), "EndCustomer Company") + ",");
					}
				}
				if(quote.getCustomerAddresses().get(CustomerType.ENDCUSTOMER)!=null
						&& ((QuoteCustomerAddress) MapUtils.getObject(quote.getCustomerAddresses(),CustomerType.ENDCUSTOMER)) !=null 
						&& ((QuoteCustomerAddress) MapUtils.getObject(quote.getCustomerAddresses(),CustomerType.ENDCUSTOMER)).getCountry()!=null){
				if (!StringUtils.equalsIgnoreCase(
						((QuoteCustomerAddress) MapUtils.getObject(quote.getCustomerAddresses(), CustomerType.ENDCUSTOMER)).getCountry(),
						quote.getPoHederDataModel().getEndCustomerCountry())) {
						validationmsg.append(prepareTableResponse(((QuoteCustomerAddress) MapUtils.getObject(quote.getCustomerAddresses(), CustomerType.ENDCUSTOMER))
											.getCountry(),quote.getPoHederDataModel().getEndCustomerCountry(), "EndCustomer Country") + ",");
				}
			  }
			}
		}
		
			if (StringUtils.equalsIgnoreCase(quote.getRtm(), "VALUE_INDIRECT")) {
				if(!(quote.getPoHederDataModel().isResellerCustomerNotExist() 
						&& StringUtils.startsWith(quote.getAssetQuoteNr(), "D"))){
				if(quote.getCustomers().get(CustomerType.RESELLER)!=null 
						&& ((QuoteCustomer) MapUtils.getObject(quote.getCustomers(), CustomerType.RESELLER)) !=null
						&& ((QuoteCustomer) MapUtils.getObject(quote.getCustomers(), CustomerType.RESELLER)).getPartyId() !=null){
					if (!StringUtils.equalsIgnoreCase(
							((QuoteCustomer) MapUtils.getObject(quote.getCustomers(), CustomerType.RESELLER)).getPartyId(),
							quote.getPoHederDataModel().getResellerPartyId())) {
						validationmsg.append(prepareTableResponse(
								((QuoteCustomer) MapUtils.getObject(quote.getCustomers(), CustomerType.RESELLER)).getPartyId(),
								quote.getPoHederDataModel().getResellerPartyId(), "Reseller PartyID") + ",");
					}
				}
				
				else {
					/*
					 * US-16980 NGQC COMPARISON
					 * QIDS Reseller company Name is compared with 
					 * NQGC Reseller company Names(1,2,3,4) 
					 */
					
					String resellerCompanyName = null;
					if(quote.getCustomers().get(CustomerType.RESELLER)!=null 
						&& ((QuoteCustomer) MapUtils.getObject(quote.getCustomers(), CustomerType.RESELLER))!=null 
						&& ((QuoteCustomer) MapUtils.getObject(quote.getCustomers(), CustomerType.RESELLER)).getCompanyName()!=null){
						List<String> fuzzyString = frictionlessOrderService.getFuzzyString("POHEADER");
						
						resellerCompanyName =  quoteService.getConsolidatedCompanyName(CustomerType.RESELLER, quote);	
						// US-16980 NGQC COMPARISON method call with additional argument synonymMap
						if(!CommonUtil.fuzzyEqualsIgnoreCase(resellerCompanyName, 
								quote.getPoHederDataModel().getResellerCompany1(), fuzzyString, frictionlessOrderService.getSynonym2Map())) {
							validationmsg.append(prepareTableResponse(resellerCompanyName,
							quote.getPoHederDataModel().getResellerCompany1(), "Reseller Company") + ",");
							}
						}
					if(quote.getCustomerAddresses().get(CustomerType.RESELLER)!=null
							&& ((QuoteCustomerAddress) MapUtils.getObject(quote.getCustomerAddresses(),CustomerType.RESELLER)) !=null 
							&& ((QuoteCustomerAddress) MapUtils.getObject(quote.getCustomerAddresses(),CustomerType.RESELLER)).getCountry()!=null){
						
					if (!StringUtils.equalsIgnoreCase(
							((QuoteCustomerAddress) MapUtils.getObject(quote.getCustomerAddresses(),CustomerType.RESELLER)).getCountry(),
							quote.getPoHederDataModel().getResellerCountry())) {
							validationmsg.append(prepareTableResponse(((QuoteCustomerAddress) MapUtils.getObject(quote.getCustomerAddresses(),
							CustomerType.RESELLER)).getCountry(),quote.getPoHederDataModel().getResellerCountry(), "Reseller Country") + ",");
					}
				  }
				}
			  }
			}
			if (StringUtils.equalsIgnoreCase(quote.getRtm(), "VALUE_INDIRECT")) {  //US-18038 validating purchase order, quotes, customers and distributors
				if (!(quote.getPoHederDataModel().isDistributorCustomerNotExist()
						&& StringUtils.startsWith(quote.getAssetQuoteNr(), "D"))) {
					if (quote.getCustomers().get(CustomerType.DISTRIBUTORINFO) != null
							&& ((QuoteCustomer) MapUtils.getObject(quote.getCustomers(), CustomerType.DISTRIBUTORINFO)) != null
							&& ((QuoteCustomer) MapUtils.getObject(quote.getCustomers(), CustomerType.DISTRIBUTORINFO)).getPartyId() != null) {
						if (!StringUtils.equalsIgnoreCase(
								((QuoteCustomer) MapUtils.getObject(quote.getCustomers(), CustomerType.DISTRIBUTORINFO)).getPartyId(),
								quote.getPoHederDataModel().getDistributorPartyId())) {
							validationmsg.append(prepareTableResponse(
									((QuoteCustomer) MapUtils.getObject(quote.getCustomers(), CustomerType.DISTRIBUTORINFO)).getPartyId(),
									quote.getPoHederDataModel().getDistributorPartyId(), "Distributor PartyID") + ",");
						}
					} else {
						/*
						 *  //US-18038 QIDS Distributor company Name is compared with
						 *  //US-18038 NQGC Distributor company Names(1,2,3,4)
						 */

						String distributorCompanyName = null;
						if (quote.getCustomers().get(CustomerType.DISTRIBUTORINFO) != null
								&& ((QuoteCustomer) MapUtils.getObject(quote.getCustomers(), CustomerType.DISTRIBUTORINFO)) != null
								&& ((QuoteCustomer) MapUtils.getObject(quote.getCustomers(), CustomerType.DISTRIBUTORINFO)).getCompanyName() != null) {
							List<String> fuzzyString = frictionlessOrderService.getFuzzyString("POHEADER");

							distributorCompanyName = quoteService.getConsolidatedCompanyName(CustomerType.DISTRIBUTORINFO, quote);
							// US-16980 NGQC COMPARISON method call with additional argument synonymMap
							if (!CommonUtil.fuzzyEqualsIgnoreCase(distributorCompanyName,
									quote.getPoHederDataModel().getDistributorCompany1(), fuzzyString, frictionlessOrderService.getSynonym2Map())) {
								validationmsg.append(prepareTableResponse(distributorCompanyName,
										quote.getPoHederDataModel().getDistributorCompany1(), "Distributor Company") + ",");
							}
						}
						if (quote.getCustomerAddresses().get(CustomerType.DISTRIBUTORINFO) != null
								&& ((QuoteCustomerAddress) MapUtils.getObject(quote.getCustomerAddresses(), CustomerType.DISTRIBUTORINFO)) != null
								&& ((QuoteCustomerAddress) MapUtils.getObject(quote.getCustomerAddresses(), CustomerType.DISTRIBUTORINFO)).getCountry() != null) {

							if (!StringUtils.equalsIgnoreCase(
									((QuoteCustomerAddress) MapUtils.getObject(quote.getCustomerAddresses(), CustomerType.DISTRIBUTORINFO)).getCountry(),
									quote.getPoHederDataModel().getDistributorCountry())) {
								validationmsg.append(prepareTableResponse(((QuoteCustomerAddress) MapUtils.getObject(quote.getCustomerAddresses(),
										CustomerType.DISTRIBUTORINFO)).getCountry(), quote.getPoHederDataModel().getDistributorCountry(), "Distributor Country") + ",");
							}
						}
					}
				}
			}
		}
		
		return validationmsg.toString();
	}
	
	@Override
	public IGenericDisplayResponse checkForCustomerCountryCode(Quote quote, FieldRule fieldRule) {
		FVOProcessResponse response = new FVOProcessResponse();
		boolean statusCode = (quote == null || quote.getCustomerAddresses() == null);
		String excMsg = quote.getComments().containsKey(QtCmtType.COUNTRYCODE)?quote.getComments().get(QtCmtType.COUNTRYCODE).getCmtTxt1(): null;
		if (!statusCode && excMsg != null) {
			response.setStatusCode(true);			
			response.setExceptionCode(StringUtils.isEmpty(excMsg)? "" : "BAE");
			response.setExceptionMsg(excMsg);
			response.setMaxSeverity(StringUtils.isEmpty(excMsg)? "" : fieldRule.getSvrtyCd().toString());
		}

		return response;
	}
	
	public IGenericDisplayResponse checkForPOCategoryMismatch (Quote quote, FieldRule fieldRule) {
		FVOProcessResponse response = new FVOProcessResponse();
		boolean status = true ;
		String exceptionMsg = null ;
		// US-18714 - Skiping PO category mimatch for EDI and EOP - where PO category is null
		if (!CommonUtil.isEmptyString(quote.getQtPrchOrdReqt().getOrigPOCategory()) && !CommonUtil.isEmptyString(quote.getQtPrchOrdReqt().getPoCategory())){
			if (!StringUtils.equalsIgnoreCase(quote.getQtPrchOrdReqt().getOrigPOCategory(), quote.getQtPrchOrdReqt().getPoCategory())) {
					status = false;
					exceptionMsg = " Original PO category " + quote.getQtPrchOrdReqt().getOrigPOCategory() + " <> NGQC PO category " + quote.getQtPrchOrdReqt().getPoCategory();
					response.setDescription(exceptionMsg);
				}
			}

		response.setStatusCode(status);	
		response.setMaxSeverity(StringUtils.isEmpty(exceptionMsg)? "" : fieldRule.getSvrtyCd().toString());
		return response;
	}
	
	public IGenericDisplayResponse check4SandHAmountNonZero4SandHFlag (Quote quote, FieldRule fieldRule) {
		FVOProcessResponse response = new FVOProcessResponse();
		boolean status = true ;
		if (quote != null &&
				quote.getFlags() != null  ) {
			QtFlag qtFlag =quote.getFlags().get(QtFlagType.SHIPHANDEXEMPTION);
			if (qtFlag != null && "true".equalsIgnoreCase(qtFlag.getFlgVl())) {
				if ( BigDecimal.ZERO.compareTo(CommonUtil.zeroOnNull (quote.getShippingAndHandlingAmt() ) ) != 0 ) {
					response.setDescription(fieldRule.getMsgId());
					status = false ;
				}
			}
		}
		response.setStatusCode(status);	
		response.setMaxSeverity(status ? "" : fieldRule.getSvrtyCd().toString());
		return response;
	}
	
	//US-15293: NGQC upgrade requirements
	public IGenericDisplayResponse checkAtleastOneSerialNumberIsPresent(Quote quote, FieldRule fieldRule) {
		FVOProcessResponse response = new FVOProcessResponse();
		boolean status = true;
		boolean check = true;
		if(quote != null && quote.getFlags() != null) {		
			QtFlag qtFlag = quote.getFlags().get(QtFlagType.UPDATEQUOTEFLAG);						
			if(qtFlag != null && "true".equalsIgnoreCase(qtFlag.getFlgVl())) {			
				for(QuoteItem quoteItem : quote.getItems()) {
					if(check) {
						List<QuoteItemCarePack> quoteItemCarePackList = quoteItem.getItemCarePacks();		
						for(QuoteItemCarePack quoteItemCarePack : quoteItemCarePackList) {
							if(quoteItemCarePack.getSerialNr() == null || quoteItemCarePack.getSerialNr().equals("")) {
								continue;							
							} else if(quoteItemCarePack.getSerialNr() != null && !quoteItemCarePack.getSerialNr().equals("")) {	
								check = false;
								break;
							}						
						}	
					}
				}	
				if(check) {
					response.setDescription(fieldRule.getMsgId());
					status = false;	
				}
			}
		}
		response.setStatusCode(status);
		response.setMaxSeverity(status ? "" : fieldRule.getSvrtyCd().toString() );
		return response;
	}
	
	//for pa id list testing purpose
	@Override
	public IGenericDisplayResponse getPaIdList(Quote quote, FieldRule fieldRule) {
		ProcessCheckResponse pcResponse = new ProcessCheckResponse();
		List<ProductProcessActions> productProcessActions = new ArrayList<>();		
		if (quote != null && quote.getPaNumberList() != null) {
			for (String st : quote.getPaNumberList()) {
				ProductProcessActions ppa = new ProductProcessActions();
				ppa.setPaId(st);
				ppa.setSeverity(ExceptionSeverity.INFO.toString());
				productProcessActions.add(ppa);
			}
		}
		quote.getPaNumberList().clear();
		pcResponse.setProcessCheckActionsList(productProcessActions);
		pcResponse.setMaxSeverity(fieldRule.getSvrtyCd() == null ? "" : fieldRule.getSvrtyCd().toString());
		return pcResponse;
	}

	/****
	 * 
	 * INC6484467 >> chinaGTM shipping condition validation check(US-18081)
	 * 
	 * IF PO Type <> H3C / NEC
	 * 
	 * AND
	 * 
	 * Currency CNY SO CN02 No split Ship2 CN
	 * 
	 * OR
	 * 
	 * Currency USD SO SG00 No split Ship2 CN
	 * 
	 * AND
	 * 
	 * Shipping condition CO or CC
	 * 
	 * THEN Show no error message
	 * 
	 * ELSE (Show error message > )
	 * 
	 * @param quote
	 * @param fieldRule
	 * @return
	 */
	public IGenericDisplayResponse gtmValidationcheck(Quote quote, FieldRule fieldRule) {
		FVOProcessResponse response = new FVOProcessResponse();
		boolean status = true;
		if (quote != null) {
			if (quote.getQtPrchOrdReqt().getSourceSystemPartnerId() == null
					|| !Set.of("H3C", "NEC").contains(quote.getQtPrchOrdReqt().getSourceSystemPartnerId())) {
					String ShipToCountry = getShipToCountry(quote);
					boolean isCNYConditionMet = "CNY".equalsIgnoreCase(quote.getCurrencyCd())
							&& "CN02".equalsIgnoreCase(quote.getCstmAtr().getSlsOrg())
							&& "CN".equalsIgnoreCase(ShipToCountry);
					boolean isUSDConditionMet = "USD".equalsIgnoreCase(quote.getCurrencyCd())
							&& "SG00".equalsIgnoreCase(quote.getCstmAtr().getSlsOrg())
							&& "CN".equalsIgnoreCase(ShipToCountry);
					if (!(isCNYConditionMet || isUSDConditionMet)) {
						boolean isDeliverySpeedValid = "CO".equalsIgnoreCase(quote.getDeliverySpeed())
								|| "CC".equalsIgnoreCase(quote.getDeliverySpeed());
						status = !isDeliverySpeedValid;
					}
				}
			}
		response.setStatusCode(status);
		return response;

	}

	public String getShipToCountry(Quote quote) {
		if (quote.getCustomerAddresses() == null || quote.getCustomerAddresses().get(CustomerType.SHIPTO) == null)
			return null;
		return quote.getCustomerAddresses().get(CustomerType.SHIPTO).getCountry();
	}
	/**
	 * Validates EDI and EOP orders for required customer details and PO category. (US- 18174)
	 *
	 * @param quote the Quote object containing order and customer details
	 * @param applyRuleRespnse to sent the error reponse
	 *
	 */
	public void validateEDIandEOPOrders(Quote quote , ApplyRuleResponse applyRuleRespnse) {
		LOG.info("Executing validateEDIandEOPOrders method " );
		String exceptionMsg="";

		if (quote!= null || quote.getQtPrchOrdReqt() != null) {
			String orderSource = quote.getQtPrchOrdReqt().getOrderSource();
			String origPOCategory = quote.getQtPrchOrdReqt().getOrigPOCategory();

			boolean isResellerPresent = isCustomerDetailsPresent(quote.getCustomers(), CustomerType.RESELLER);
			boolean isEndCustomerPresent = isCustomerDetailsPresent(quote.getCustomers(), CustomerType.ENDCUSTOMER);

			if (!CommonUtil.isEmptyString(quote.getRtm()) &&
					Constants.rTM.INDIRECT.equalsIgnoreCase(quote.getRtm()) &&
					(Constants.OrderSource.EDI.equalsIgnoreCase(orderSource) || Constants.OrderSource.EOP.equalsIgnoreCase(orderSource))) {

				boolean isEDI = Constants.OrderSource.EDI.equalsIgnoreCase(orderSource);
				boolean isSTGEDrop = "STGE".equals(origPOCategory) || "DROP".equals(origPOCategory);
				boolean isSTCK = "STCK".equals(origPOCategory);

				if (isEDI) {
					if (isSTGEDrop) {
						if (!isResellerPresent || !isEndCustomerPresent) {
							exceptionMsg=buildEDIMissingCustomerMessage(origPOCategory, isResellerPresent, isEndCustomerPresent);
						}
					} else if (isSTCK) {
						if (isResellerPresent || isEndCustomerPresent) {
							exceptionMsg=buildEDINotAllowedCustomerMessage(origPOCategory, isResellerPresent, isEndCustomerPresent);
						}
					}
				} else { // EOP
					if (isSTGEDrop) {
						if (!isResellerPresent) {
							exceptionMsg= origPOCategory + " PO Category RESELLER is required.";
						}
					} else if (isSTCK) {
						if (isResellerPresent) {
							exceptionMsg=origPOCategory + " PO Category RESELLER needs to be removed to process the order.";
						}
					}
				}
			}
		}

		if (StringUtils.isNotEmpty(exceptionMsg)) {
			ResponseException re = new ResponseException("", ApplicationDomainType.OMUI, 2021, ExceptionSeverity.Error_Save_Allowed, exceptionMsg , "");
			applyRuleRespnse.getResponseExceptionList().add(re) ;
			applyRuleRespnse.setQuote(quote) ;
			applyRuleRespnse.setMaxSeverity((ExceptionSeverity.Error_Save_Allowed.toString()) ) ;
		}
		LOG.info("Executing validateEDIandEOPOrders method -completed" );
	}

	private String buildEDIMissingCustomerMessage(String poCategory, boolean resellerPresent, boolean endCustomerPresent) {
		if (!resellerPresent && !endCustomerPresent) {
			return  poCategory +" PO Category both RESELLER and ENDCUSTOMER are missing.";
		} else if (!resellerPresent) {
			return  poCategory + " PO Category  RESELLER is missing.";
		} else {
			return  poCategory + " PO Category ENDCUSTOMER is required.";
		}
	}

	private String buildEDINotAllowedCustomerMessage(String poCategory, boolean resellerPresent, boolean endCustomerPresent) {
		if (resellerPresent && endCustomerPresent) {
			return "For" +poCategory + " PO Category both RESELLER and ENDCUSTOMER need to be removed to process the order.";
		} else if (resellerPresent) {
			return "For" +poCategory + " PO Category RESELLER needs to be removed to process the order.";
		} else {
			return "For"+ poCategory + " PO Category ENDCUSTOMER needs to be removed to process the order.";
		}
	}

	public boolean isCustomerDetailsPresent(Map<CustomerType, QuoteCustomer> customers, CustomerType customerType) {
		if (customers == null) {
			return false;
		}
		QuoteCustomer customer = (QuoteCustomer) MapUtils.getObject(customers, customerType);
		return customer != null && customer.getCompanyName() != null;
	}
	
	//US-18175 - code changes to  STOP EC & RESELLER validations
	   /**
     * method provided to get inclusivePartyIdsSet from DB
     * @return returns set of inclusivePartyIdsSet
     */
	public Set<String> getInclusivePartyIds() {
		
		LOG.info("Inside ChecklistServiceImpl >>>>> getInclusivePartyIds method" );
		
		Set<String> inclusivePartyIdsSet = new HashSet<>();
		
		List<InclusivePartyIdModel> inclusivePartyIdModels = dropDownListService.getInclusivePartyIds();
		for (InclusivePartyIdModel inclusivePartyIdModel : inclusivePartyIdModels) {
			inclusivePartyIdsSet.add(inclusivePartyIdModel.getSoldToPartyId());
		}
		return inclusivePartyIdsSet;
	}
	//US-18358 - code changes for USAGE as INB
	/**
	 * Sets the delivery terms to "FOB" if the usage is "INB" and the source price list type is "EXW".
	 * If the usage is "INB" but the source price list type is not "EXW", adds a validation error to the response.
	 *
	 * @param quote the {@link Quote} object containing the purchase order request and price list relevant information
	 * @param applyRuleRespnse the {@link ApplyRuleResponse} to which validation errors will be added if conditions are not met which is essential to show as Validation Error IN UI
	 */
	public void setPriceListForUsageChangeINB( Quote quote, ApplyRuleResponse applyRuleRespnse){
		if((quote.getQtPrchOrdReqt().getUsage()!=null)&&(quote.getQtPrchOrdReqt().getUsage().equalsIgnoreCase("INB"))){
			String srcPriceListType=quote.getSrcPriceListType();
			if(srcPriceListType!=null) {
				if (srcPriceListType.equalsIgnoreCase("EXW")) {
					quote.setDeliveryTerms("FOB");

				}
				else {
					ResponseException re = new ResponseException("", ApplicationDomainType.OMUI, 2021, ExceptionSeverity.Error_Save_Allowed, "EDI Usage is INB but quote price list is not EXW", "");
					applyRuleRespnse.getResponseExceptionList().add(re);
					applyRuleRespnse.setQuote(quote);
					applyRuleRespnse.setMaxSeverity((ExceptionSeverity.Error_Save_Allowed.toString()));
				}
			}
			else{
				ResponseException re = new ResponseException("", ApplicationDomainType.OMUI, 2021, ExceptionSeverity.Error_Save_Allowed, "EDI Usage is INB but quote price list is not EXW", "");
				applyRuleRespnse.getResponseExceptionList().add(re);
				applyRuleRespnse.setQuote(quote);
				applyRuleRespnse.setMaxSeverity((ExceptionSeverity.Error_Save_Allowed.toString()));
			}

		}
	}

	// In ChecklistServiceImpl.java, add wiring in existing apply rule flow method (example method name applyRules or similar)

// Add import for CacheConfig and StringUtils if needed
import com.hp.common.CacheConfig;
import org.apache.commons.lang3.StringUtils;

// Near the existing apply rule flow method, insert call to processAndValidateDealVersionNr
// Only if feature flag ENABLE_DQM_DEALVERSION_VALIDATION is true,
// quote assetQuoteNr starts with 'D', and dealVersionNr is not blank

if ("TRUE".equalsIgnoreCase(CacheConfig.getCacheValue("ENABLE_DQM_DEALVERSION_VALIDATION"))
        && quote.getAssetQuoteNr() != null
        && quote.getAssetQuoteNr().startsWith("D")
        && StringUtils.isNotBlank(quote.getDealVersionNr())) {

    // create or get existing ApplyRuleResponse applyRuleResponse
    checklistService.processAndValidateDealVersionNr(quote, applyRuleResponse);
}

		String jsonPayload = "";
		String exceptionMsg = "";

			if (StringUtils.isNotBlank(dealNr)) {
				jsonPayload = s4DealNumberValidationServiceImpl.getS4DealNumberResponse(dealNr);
				if (StringUtils.isBlank(jsonPayload)) {
					LOG.warn("No response received from S4 for deal number: " + dealNr);
					exceptionMsg = "No response received from S4 for deal number: " + dealNr;
					ResponseException re = new ResponseException("", ApplicationDomainType.OMUI, 2021, ExceptionSeverity.Error_Save_Allowed, exceptionMsg, "");
					applyRuleResponse.getResponseExceptionList().add(re);
					applyRuleResponse.setQuote(quote);
					applyRuleResponse.setMaxSeverity((ExceptionSeverity.Error_Save_Allowed.toString()));
				}
				LOG.info("DealNrS4Addison Call JSON Response: " + jsonPayload);


				Map<String, List<String>> dealVersionsMap = CommonUtil.extractDealVersionNrs(jsonPayload);
				Integer s4LatestDealVersionNr = CommonUtil.getMaxDealVersionNr(dealVersionsMap, dealNr);

				//checking - if in case both values are null - edge case scenario
				if (s4LatestDealVersionNr == null && qidsDealVersionNr == null) {
					LOG.warn("Qidsquote has deal version : " + qidsDealVersionNr + ", latest deal version in S4 is : " + s4LatestDealVersionNr);
					exceptionMsg = "Qidsquote has deal version : " + qidsDealVersionNr + ", latest deal version in S4 is : " + s4LatestDealVersionNr;
					if (StringUtils.isNotEmpty(exceptionMsg)) {
						ResponseException re = new ResponseException("", ApplicationDomainType.OMUI, 2021, ExceptionSeverity.Error_Save_Allowed, exceptionMsg, "");
						applyRuleResponse.getResponseExceptionList().add(re);
						applyRuleResponse.setQuote(quote);
						applyRuleResponse.setMaxSeverity((ExceptionSeverity.Error_Save_Allowed.toString()));
					}
				}
				//checking - if both values are not equal
				if (!Objects.equals(s4LatestDealVersionNr, qidsDealVersionNr)) {
					LOG.warn("Qidsquote has deal version : " + qidsDealVersionNr + ", latest deal version in S4 is : " + s4LatestDealVersionNr);
					exceptionMsg = "Qidsquote has deal version : " + qidsDealVersionNr + ", latest deal version in S4 is : " + s4LatestDealVersionNr;
					if (StringUtils.isNotEmpty(exceptionMsg)) {
						ResponseException re = new ResponseException(quote.getAssetQuoteNrAndVrsn(), ApplicationDomainType.OMUI, 2021, ExceptionSeverity.Error_Save_Allowed, exceptionMsg, "");
						applyRuleResponse.getResponseExceptionList().add(re);
						applyRuleResponse.setQuote(quote);
						applyRuleResponse.setMaxSeverity((ExceptionSeverity.Error_Save_Allowed.toString()));
					}
				}
			}
		LOG.info("ChecklistServiceImpl.processAndValidateDealVersionNr method - completed");
	}
	//US-18346 - DQM Validation for S4 DealVersionNumber - ENDS

	//US-18665
	public void checkFanApproval(Quote quote, ApplyRuleResponse applyRuleRespnse) {
		if (quote.getVldtnSmry() == null) {
			return;
		}

		// US-18665: Read fresh vldtnSmryInfo from DB to recover ExternalID and Status stored
		// by SettingExternalID (in SfdcGenericWebServiceDao). The frontend's POST body carries
		// a stale vsResponseList that has ExternalID="" because it was loaded before
		// SettingExternalID ran on the backend.  We merge: take isFormLinkClicked from the
		// in-memory (frontend) value, take ExternalID/Status from DB.
		try {
			String dbVldtnSmryInfo = detailChangeDAO.getVldtnSmryInfoFromDB(quote);
			LOG.info("checkFanApproval: DB vldtnSmryInfo=" + dbVldtnSmryInfo);
			if (dbVldtnSmryInfo != null && !dbVldtnSmryInfo.trim().isEmpty()) {
				Gson gsonMerge = new Gson();
				List<Map<String, Object>> dbList = gsonMerge.fromJson(dbVldtnSmryInfo, List.class);
				String inMemoryInfo = quote.getVldtnSmry().getVldtnSmryInfo();
				if (dbList != null && inMemoryInfo != null && !inMemoryInfo.trim().isEmpty()) {
					List<Map<String, Object>> memList = gsonMerge.fromJson(inMemoryInfo, List.class);
					if (memList != null) {
						// Extract non-empty ExternalID and Status from DB
						String dbExternalId = null;
						String dbStatus = null;
						for (Map<String, Object> item : dbList) {
							if (item == null) continue;
							if (item.containsKey("ExternalID") && item.get("ExternalID") != null
									&& !item.get("ExternalID").toString().isEmpty()) {
								dbExternalId = item.get("ExternalID").toString();
							}
							if (item.containsKey("Status") && item.get("Status") != null
									&& !item.get("Status").toString().isEmpty()) {
								dbStatus = item.get("Status").toString();
							}
						}
						LOG.info("checkFanApproval: DB ExternalID=" + dbExternalId + " | DB Status=" + dbStatus);
						// Overlay DB ExternalID and Status into the in-memory list (preserve isFormLinkClicked from frontend)
						boolean externalIdOverlaid = false;
						boolean statusOverlaid = false;
						for (Map<String, Object> item : memList) {
							if (item == null) continue;
							if (dbExternalId != null && item.containsKey("ExternalID")) {
								item.put("ExternalID", dbExternalId);
								externalIdOverlaid = true;
							}
							if (dbStatus != null && item.containsKey("Status")) {
								item.put("Status", dbStatus);
								statusOverlaid = true;
							}
						}
						// If keys were missing in memList, add them from DB
						if (!externalIdOverlaid && dbExternalId != null) {
							Map<String, Object> entry = new java.util.LinkedHashMap<>();
							entry.put("ExternalID", dbExternalId);
							memList.add(entry);
						}
						if (!statusOverlaid && dbStatus != null) {
							Map<String, Object> entry = new java.util.LinkedHashMap<>();
							entry.put("Status", dbStatus);
							memList.add(entry);
						}
						quote.getVldtnSmry().setVldtnSmryInfo(gsonMerge.toJson(memList));
						LOG.info("checkFanApproval: merged vldtnSmryInfo=" + gsonMerge.toJson(memList));
					}
				} else if (dbList != null && (inMemoryInfo == null || inMemoryInfo.trim().isEmpty())) {
					// No in-memory data at all — use DB as-is
					quote.getVldtnSmry().setVldtnSmryInfo(dbVldtnSmryInfo);
				}
			}
		} catch (Exception mergeEx) {
			LOG.warn("checkFanApproval: Could not merge DB vldtnSmryInfo, proceeding with in-memory value. " + mergeEx.getMessage());
		}

		String vldtnSmryInfo = quote.getVldtnSmry().getVldtnSmryInfo();

		if (vldtnSmryInfo == null) {
			// No existing info - initialise with defaults, ExternalID blank until settingExternalId populates it
			quote.getVldtnSmry().setVldtnSmryInfo("[{\"isFormLinkClicked\":\"false\"},{\"Status\":\"\"},{\"ExternalID\":\"\"}]");
			quote.getVldtnSmry().setUpdated(true);
			persistQuote(quote);
			return;
		}

		try {
			Gson gson = new Gson();
			List<Map<String, Object>> vldtnList = gson.fromJson(vldtnSmryInfo, List.class);

			if (vldtnList == null) {
				persistQuote(quote);
				return;
			}

			// Read all three keys — preserve ANY existing value (especially ExternalID set by settingExternalId)
			String isFormLinkClickedValue = null;
			String statusValue = null;       // null means key absent, "" means key present but empty
			String externalIdValue = null;   // null means key absent — must NOT overwrite if already set
			boolean isFormLinkClickedPresent = false;
			boolean isStatusPresent = false;
			boolean isExternalIdPresent = false;

			for (Map<String, Object> item : vldtnList) {
				if (item == null) continue;
				if (item.containsKey("isFormLinkClicked")) {
					isFormLinkClickedPresent = true;
					isFormLinkClickedValue = item.get("isFormLinkClicked") != null
							? item.get("isFormLinkClicked").toString() : null;
				}
				if (item.containsKey("Status")) {
					isStatusPresent = true;
					statusValue = item.get("Status") != null ? item.get("Status").toString() : "";
				}
				if (item.containsKey("ExternalID")) {
					isExternalIdPresent = true;
					// Preserve the real value — do NOT blank it out
					externalIdValue = item.get("ExternalID") != null ? item.get("ExternalID").toString() : "";
				}
			}

			LOG.info("checkFanApproval: isFormLinkClicked=" + isFormLinkClickedValue
					+ " | Status=" + statusValue
					+ " | ExternalID=" + externalIdValue);

			boolean modified = false;

			// Add missing keys — each in its OWN separate map entry
			if (!isFormLinkClickedPresent) {
				Map<String, Object> entry = new java.util.LinkedHashMap<>();
				entry.put("isFormLinkClicked", "false");
				vldtnList.add(entry);
				isFormLinkClickedValue = "false";
				modified = true;
			}
			if (!isStatusPresent) {
				Map<String, Object> entry = new java.util.LinkedHashMap<>();
				entry.put("Status", "");
				vldtnList.add(entry);
				modified = true;
			}
			if (!isExternalIdPresent) {
				// Only add a blank ExternalID placeholder if it was truly never set
				Map<String, Object> entry = new java.util.LinkedHashMap<>();
				entry.put("ExternalID", "");
				vldtnList.add(entry);
				modified = true;
			}

			if (modified) {
				quote.getVldtnSmry().setVldtnSmryInfo(gson.toJson(vldtnList));
				quote.getVldtnSmry().setUpdated(true);
			}

			// If isFormLinkClicked is "false" or Status is not yet set — just persist and stop
			if (!"true".equals(isFormLinkClickedValue)) {
				quote.getVldtnSmry().setUpdated(true);
				persistQuote(quote);
				return;
			}

			// isFormLinkClicked is "true" — check approval status
			boolean isApproved = false;
			for (Map<String, Object> item : vldtnList) {
				if (item != null && item.containsKey("Status")
						&& "Approved".equalsIgnoreCase(item.get("Status").toString())) {
					isApproved = true;
					break;
				}
			}

			if (!isApproved) {
				ResponseException re = new ResponseException("", ApplicationDomainType.OMUI, 2021,
						ExceptionSeverity.Error_Save_Allowed, "Awaiting FAN Approval", "");
				applyRuleRespnse.getResponseExceptionList().add(re);
				applyRuleRespnse.setQuote(quote);
				applyRuleRespnse.setMaxSeverity(ExceptionSeverity.Error_Save_Allowed.toString());
			}
//			else {
//				if (applyRuleRespnse.getResponseExceptionList().isEmpty()) {
//					quote.getQtPrchOrdReqt().setPoStatusCD("DEC");
//					quote.getQtPrchOrdReqt().setPoStatusDN("Data Entry Complete");
//
//					// FAN Approval granted and no other validation errors — trigger Convert to Order
//					try {
//						LOG.info("checkFanApproval: Status is Approved, no other errors. Triggering Convert to Order for quote: "
//								+ quote.getAssetQuoteNrAndVrsn());
//
//						ConvertToOrderRequestInternal ctoRequest = new ConvertToOrderRequestInternal();
//						ctoRequest.setQuote(quote);
//
//						OrderRequestHeader orderReqHeader = new OrderRequestHeader();
//						orderReqHeader.setSourceSytem(ConvertToOrderConstants.SourceSystem.SOURCE_SYSTEM_OMUI);
//						orderReqHeader.setRegion(quote.getRegionCd());
//						orderReqHeader.setCountryCD(quote.getCountryCd());
//						ctoRequest.setOrderRequestHeader(orderReqHeader);
//
//						ConvertToOrderResponseInternal ctoResponse = convertToOrderGateway.processInternalOrder(ctoRequest);
//
//						if (ctoResponse != null && ConvertToOrderConstants.CTO_Operation_State.CTO_SUCCESS
//								.equalsIgnoreCase(ctoResponse.getStatus())) {
//							LOG.info("checkFanApproval: Convert to Order SUCCESS for quote: "
//									+ quote.getAssetQuoteNrAndVrsn()
//									+ " | HPE Order: " + ctoResponse.getHpeOrderNumber()
//									+ " | SO: " + ctoResponse.getSalesOrderNumber());
//						} else {
//							LOG.error("checkFanApproval: Convert to Order FAILED for quote: "
//									+ quote.getAssetQuoteNrAndVrsn()
//									+ " | Response: " + (ctoResponse != null ? ctoResponse.getMessage() : "null"));
//						}
//					} catch (Exception e) {
//						LOG.error("checkFanApproval: Exception during Convert to Order for quote: "
//								+ quote.getAssetQuoteNrAndVrsn() + " | " + e.getMessage(), e);
//					}
//				} else {
//					// Other validation errors exist — do not convert
//					LOG.info("checkFanApproval: Status is Approved but other validation errors exist. Skipping CTO for quote: "
//							+ quote.getAssetQuoteNrAndVrsn());
//				}
//			}

		} catch (Exception e) {
			LOG.error("Error in checkFanApproval: " + e.getMessage(), e);
		}

		persistQuote(quote);
	}
	public void persistQuote(Quote quote)  {
		try {
			quote.getVldtnSmry().setNewBean(false);
			detailChangeDAO.persistVldtnSmry(quote);
		} catch (Exception e) {
			LOG.error("Error persisting quote validation summary: " + e.getMessage(), e);
			throw e;
		}
	}

	}