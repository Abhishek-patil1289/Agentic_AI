package com.hp.om.integration.qidsdata;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.hp.om.beans.ApplyRuleResponse;
import com.hp.om.util.DateUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.hp.bcs.common.Constants.CommonConstants;
import com.hp.bcs.common.Constants.NGQConstants;
import com.hp.bcs.utils.CacheConfig;
import com.hp.common.CommonUtil;
import com.hp.common.Constants;
import com.hp.common.beans.AdditionalCharBitConstants;
import com.hp.common.beans.QuoteCharBitConstants;
import com.hp.om.beans.AssetType;
import com.hp.om.beans.AugmentedLocalizationFilter;
import com.hp.om.beans.BundleType;
import com.hp.om.beans.Cinv;
import com.hp.om.beans.ContactType;
import com.hp.om.beans.CustomerType;
import com.hp.om.beans.DealQuoteInfo;
import com.hp.om.beans.DlvrySpeed;
import com.hp.om.beans.FanModel;
import com.hp.om.beans.InclusivePartyIdModel;
import com.hp.om.beans.InternalDemoModel;
import com.hp.om.beans.LocalizationFilter;
import com.hp.om.beans.PrchOrdAtachmt;
import com.hp.om.beans.ProductLineModel;
import com.hp.om.beans.QtCmt;
import com.hp.om.beans.QtCmtType;
import com.hp.om.beans.QtCstmAtr;
import com.hp.om.beans.QtFlag;
import com.hp.om.beans.QtFlagType;
import com.hp.om.beans.QtPrchOrdReqt;
import com.hp.om.beans.QtSorgDcDiv;
import com.hp.om.beans.QtSorgDcDivFilter;
import com.hp.om.beans.Quote;
import com.hp.om.beans.QuoteCategory;
import com.hp.om.beans.QuoteContact;
import com.hp.om.beans.QuoteCustomer;
import com.hp.om.beans.QuoteCustomerAddress;
import com.hp.om.beans.QuoteCustomerContact;
import com.hp.om.beans.QuoteItem;
import com.hp.om.beans.QuoteItemMcc;
import com.hp.om.beans.QuoteItemModel;
import com.hp.om.beans.QuoteLineItemFlags;
import com.hp.om.beans.SalesOrgModel;
import com.hp.om.business.checkList.interfaces.ChecklistService;
import com.hp.om.business.checkList.interfaces.IValidationSummaryService;
import com.hp.om.business.dropdownlist.interfaces.DropDownListService;
import com.hp.om.business.frictionless.interfaces.FrictionlessOrderService;
import com.hp.om.business.generic.interfaces.WeightHandler;
import com.hp.om.business.myworkspace.interfaces.MyWorkspaceService;
import com.hp.om.business.qidsdata.QidsQuoteheader;
import com.hp.om.business.quote.interfaces.QidsQuoteService;
import com.hp.om.business.quote.interfaces.QuoteService;
import com.hp.om.business.quoteitem.interfaces.QuoteItemService;
import com.hp.om.business.quoteitemmcc.interfaces.IMccConditionTypeDAO;
import com.hp.om.business.quoteitemmcc.interfaces.IMccConditionTypeService;
import com.hp.om.business.transformutil.TransformUtilService;
import com.hp.om.business.emdm.interfaces.IEMDMService;
import com.hp.om.integration.qidsdata.interfaces.IPrepareOMUIQuote;
import com.hp.om.service.application.ApplicationService;
import com.hp.om.service.qids.readquote.xjc.generated.CommentType;
import com.hp.om.service.qids.readquote.xjc.generated.Comments;
import com.hp.om.service.qids.readquote.xjc.generated.CustomerContactType;
import com.hp.om.service.qids.readquote.xjc.generated.HPQuote;
import com.hp.om.service.qids.readquote.xjc.generated.HpQuoteCIDList;
import com.hp.om.service.qids.readquote.xjc.generated.HpQuoteLineItemList;
import com.hp.om.service.qids.readquote.xjc.generated.HpQuoteLineItemMCCList;
import com.hp.om.service.qids.readquote.xjc.generated.QuoteFlagType;
import com.hp.om.service.qids.readquote.xjc.generated.QuoteFlags;
import com.hp.om.service.qids.readquote.xjc.generated.QuoteHeader;
import com.hp.service.core.LoggingDomainType;
import com.hp.service.core.Q2CLogger;
import com.hp.service.core.Q2CLoggerFactory;
import com.hp.service.core.exception.BusinessApplicationException;
import com.hp.service.core.exception.SystemApplicationException;

public class PrepareOMUIQuote implements IPrepareOMUIQuote {

	@Autowired
	private ApplicationService applicationService;

	@Autowired
	private TransformUtilService transformUtilService;

	@Autowired
	private MyWorkspaceService myWorkspaceService;

	@Autowired
	private FrictionlessOrderService frictionlessOrderService;

	@Autowired
	DropDownListService dropDownListService;
	
	@Autowired
	private QuoteService quoteService;	
	
	@Autowired
	private QuoteItemService quoteItemService ;
	

	@Autowired
	public IMccConditionTypeService iMccConditionTypeService;
	
	@Autowired
	IMccConditionTypeDAO mccConditionTypeDao;
	
	@Autowired
	public QidsQuoteService qidsQuoteService;
	
	@Autowired
	public IValidationSummaryService validationSummaryService ;
	
	
	@Autowired
	public ChecklistService checklistService;
	
	@Autowired
	public IEMDMService eMDMService;

	private static final Q2CLogger LOG = Q2CLoggerFactory.getLogger(PrepareOMUIQuote.class, LoggingDomainType.QIDS);
	
	/*
	 * private AsyncTaskExecutor taskExecutor;
	 * 
	 * @Autowired public PrepareOMUIQuote(AsyncTaskExecutor taskExecutor) {
	 * this.taskExecutor = taskExecutor; package com.hp.om.business.checkList.interfaces;

import com.hp.om.beans.Quote;
import com.hp.om.business.applyrules.ApplyRuleResponse;

public interface ChecklistService {
    void processAndValidateDealVersionNr(Quote quote, ApplyRuleResponse applyRuleResponse);
}
	 * 
	 * class ConvertQuoteAsyncProcessor implements Callable<String> { private
	 * String task; private RequestAttributes requestAttrs; private
	 * IOrderMgmtServices orderMgmtServices; private Quote quote;
	 * 
	 * public ConvertQuoteAsyncProcessor(String task, RequestAttributes
	 * requestAttrs, IOrderMgmtServices orderMgmtServices, Quote quote) {
	 * this.task = task; this.requestAttrs = requestAttrs;
	 * this.orderMgmtServices = orderMgmtServices; this.quote = quote; }
	 * 
	 * @Override public String call() throws Exception { if (requestAttrs !=
	 * null) { RequestContextHolder.setRequestAttributes(requestAttrs); } switch
	 * (task) { case "verifyEsdAndCarePackItems":
	 * orderMgmtServices.verifyEsdAndCarePackItems(quote); break; case
	 * "setCustomerPrefence": orderMgmtServices.setCustomerPrefence(quote);
	 * break; case "setBitWiseChar": orderMgmtServices.setBitWiseChar(quote);
	 * break; default: break; } try { return task; } finally {
	 * RequestContextHolder.resetRequestAttributes(); } } }
	 */

	public Quote convertToQuote(HPQuote qidsQuote, QidsFilter filter) {

		Quote quote = null;
		
		if (qidsQuote.getQuoteHeader() != null) {
			boolean serpFlag = getSerpFlagLocal(qidsQuote.getQuoteFlagsList());
			LOG.debug("PrepareOMUIQuote >> convertToQuote >> convertQuote");			
			quote = convertQuote(qidsQuote.getQuoteHeader(), filter, serpFlag);
			
			if (CommonUtil.checkBRIMFlag(qidsQuote)) { 
				quote = setAdditionalFieldsForBRIMQuote(qidsQuote.getQuoteHeader(),quote);
			}
			
			LOG.debug("PrepareOMUIQuote >> convertToQuote >> getSerpFlag");
			applicationService.getOrderMgmtApplnType(quote).getSerpFlag(quote, qidsQuote);
			for (QtFlagType flagTyp : QtFlagType.values() ) {
				QtFlag qtFlag = new QtFlag();
				qtFlag.setSlsQtnId(quote.getSlsQtnId());
				qtFlag.setSlsQtnVrsnSqnNr(quote.getSlsQtnVrsnSqnNr());
				qtFlag.setQtFlagType(flagTyp);
				quote.getFlags().put(flagTyp, qtFlag);                      
			}
			captureHeaderFanDetails(quote, qidsQuote);

			// applicationService.getOrderMgmtApplnType().getQTContactInfo(quote,
			// qidsQuote);
			
			//S4_INTERNAL_DEMO
			if (CommonUtil.checkBRIMFlag(qidsQuote)) {
				filter.setTempPartyID( CommonUtil.getValueFromJSON(qidsQuote.getQuoteHeader().getAdditionalInfo(), "sldPty").toString());
			}else{
			if(null != qidsQuote.getQuoteHeader() 
					&& null != qidsQuote.getQuoteHeader().getSoldTo() 
					&& null != qidsQuote.getQuoteHeader().getSoldTo().getCompany() 
					&& null != qidsQuote.getQuoteHeader().getSoldTo().getCompany().getPartyID()){
				filter.setTempPartyID(qidsQuote.getQuoteHeader().getSoldTo().getCompany().getPartyID());
			}
			}

			if (quote.getQtPrchOrdReqt().isSerpFlag()) {
				quote.setDealNrDerived(qidsQuote.getQuoteHeader().getEclipseDealId());
			}
			if(StringUtils.equalsIgnoreCase(filter.getRtm(), "VALUE_DIRECT")) {
				LOG.debug("PrepareOMUIQuote >> convertToQuote >> S4BUSINESS MODEL");
			quoteService.getOMHandler(quote).s4BusinessModel(quote, filter,qidsQuote);
			}
			quoteService.getOMHandler(quote).setPaymentTerm(quote, qidsQuote.getQuoteHeader().getPaymentTerm());
			
			/*
			 * US-15516 -NGQC HPFS Asset Management  
			 * Populate HPE FS Asset Management Flag from QIDS obj to NGQC quote object 
			 * Setting Debit bitwiseQTChar to bitwiseqtchar value
			 */
			quoteService.populateQidsFlags2NGQC(quote, qidsQuote.getQuoteFlagsList().getQuoteFlags(),
					QtFlagType.HPEFSFLAG);
			if (quoteService.checkFlagValueIsPresent(quote, QtFlagType.HPEFSFLAG)) {
				quote.setBitWiseQtChar(CommonUtil.doOR(quote.getBitWiseQtChar(), QuoteCharBitConstants.DEBIT));
			}

			if (qidsQuote.getHpQuoteLineItemList() != null) {
				WeightHandler wtHandler = applicationService.getWeightHandler();

				applicationService.getOrderMgmtApplnType(quote).setCommnts(qidsQuote, quote);		
				/*
				 * if (StringUtils.equalsIgnoreCase(quote.getOrigAsset(),
				 * Constants.Software.SW_ORIG_ASSET) ||
				 * StringUtils.equalsIgnoreCase(quote.getOrigAsset(),
				 * Constants.Software.SW_PARTNER_ORIG_ASSET)) {
				 * 
				 * applicationService.getOrderMgmtApplnType().
				 * transformBMIProdStruct(qidsQuote,wtHandler,quote); //
				 * addComments(qidsQuote, quote);
				 * 
				 * } else {
				 */

				boolean isOrigAssetExists = false;
				boolean isOrigAssetNGQExists = false;
				List<ProductLineModel> allProdList = new ArrayList<ProductLineModel>();
				Map<String, String> omuiSrvcCtrlMap = dropDownListService.getOmuiservicekeyValues();
				String origAssets = omuiSrvcCtrlMap.get("ORIG_ASSET_LIST");
				String origAssetsForNGQ = omuiSrvcCtrlMap.get("ORIG_ASSET_LIST_FOR_NGQ");
				
				if (origAssetsForNGQ != null && origAssetsForNGQ.contains("~" + quote.getOrigAsset() + "~")) {
					isOrigAssetNGQExists = true;
				}

				if (origAssets != null && origAssets.contains("~" + quote.getOrigAsset() + "~")) {
					isOrigAssetExists = true;
				}

				if (isOrigAssetExists || isOrigAssetNGQExists) {
					allProdList = frictionlessOrderService.getNetworkingProductList();
				}				
				String isAruba = "false";
				String isNimble = "false";
				boolean isStandard = true;
				boolean egProductLineExists = false;				
				Long quoteCharBitLong = NumberUtils.toLong(quote.getBitWiseQtChar());
				/*
				 * if ( "ORDERS05".equals (
				 * omuiSrvcCtrlMap.get("DEFAULT_INTERFACE_TYPE") ) ) {
				 * quoteCharBitLong = quoteCharBitLong |
				 * QuoteCharBitConstants.ORDERS05_INDICATOR ; }
				 */
				List<String> productLineList = new ArrayList<String>();
				//make this only for S4 & EMEA
				List<String> alletraList= quoteService.getOMHandler(quote).getAlletraProducts(quote);
				
				List<QuoteItem> endBOMCnfgList = new ArrayList<QuoteItem>();
				
				for (HpQuoteLineItemList item : qidsQuote.getHpQuoteLineItemList().getHpQuoteLineItems()) {					
					boolean isNonNetwork = false;

					// temprorary check need to confirm

					if (!"Comment".equalsIgnoreCase(item.getLineType()) && item.getProductLine() != null
							&& isOrigAssetExists) {
						for (ProductLineModel product : allProdList) {
							productLineList.add(item.getProductLine());
							if (StringUtils.endsWithIgnoreCase(product.getProductLine(), item.getProductLine())) {															

								switch (product.getProductCategory()) {
								case CommonConstants.ARUBA_NETWORKING: {
									isAruba = "true";
									isNonNetwork = true;
									quoteCharBitLong = quoteCharBitLong | QuoteCharBitConstants.ARUBA_NW_ONLY_INDICATOR;
									isStandard = false;
									break;
								}
								case CommonConstants.LEGACY_HPE_NETWORKING: {
									isAruba = "true";
									isNonNetwork = true;
									quoteCharBitLong = quoteCharBitLong | QuoteCharBitConstants.LEGACY_HPN_ONLY_INDICATOR;
									isStandard = false;
									break;
								}								
								}
								break;
							}
						}
					}
					
					//Nimble logic added null check
					if (!"Comment".equalsIgnoreCase(item.getLineType()) && StringUtils.isNotEmpty(item.getProductLine()) && isOrigAssetNGQExists) {
						for (ProductLineModel product : allProdList) {
							if (StringUtils.endsWithIgnoreCase(product.getProductLine(), item.getProductLine())) {								
								switch (product.getProductCategory()) {
								case CommonConstants.NIMBLE: {
									isNimble = "true";
									quoteCharBitLong = quoteCharBitLong | QuoteCharBitConstants.NIMBLE_INDICATOR;
									isStandard = false;
									break;
								}
								}								
							}
						}
					}
					
					
					QuoteItem quoteItem = convertLineItems(item, qidsQuote.getQuoteHeader().getOriginatingQuoteSystem(), quote);										
					
					wtHandler.setWeightAttributes(quoteItem);
					
					if (AssetType.NGQ.getCode().equalsIgnoreCase(quote.getOrigAsset()) || AssetType.NGQ_Partner.getCode().equalsIgnoreCase(quote.getOrigAsset())) {
						quote.setCbn(filter.getTempCbn() !=null ? filter.getTempCbn() : filter.getCbn());												
						if("RU".equals(quote.getCountryCd())) {
							
							if (quote.getCbn()!=null && quote.getCbn().startsWith("61") && quote.getQtPrchOrdReqt()!=null && !quote.getQtPrchOrdReqt().isSerpFlag()) {
								long qtBitIndicator = quoteService.getOMHandler(quote).demoBuyoutChecksforRussia(quote, quoteItem, quoteCharBitLong);
								quoteCharBitLong = quoteCharBitLong | qtBitIndicator;
							}
						} else {
							if(!("S4_INTRNL_DEMO".equals(quote.getQtPrchOrdReqt().getFnModelUUID()) 
									|| "S4_INTRNL_DEMO".equals(quote.getQtPrchOrdReqt().getFnModelGrpUUID())
									|| "S4_DI_PICKUP".equals(quote.getQtPrchOrdReqt().getFnModelUUID())
									|| "S4_DI_PICKUP".equals(quote.getQtPrchOrdReqt().getFnModelGrpUUID()) 
									|| "S4_WW_ORDER".equals(quote.getQtPrchOrdReqt().getFnModelUUID())
									|| "S4_WW_ORDER".equals(quote.getQtPrchOrdReqt().getFnModelGrpUUID())
									//Start - US-17463 NGQC STARGAZER/CONSIGNMENT
									|| "S4_CONSIG_FILLUP".equals(quote.getQtPrchOrdReqt().getFnModelUUID()) 
									|| "S4_CONSIG_FILLUP".equals(quote.getQtPrchOrdReqt().getFnModelGrpUUID())
									|| 	"S4_CONSIG_ISSUE".equals(quote.getQtPrchOrdReqt().getFnModelUUID())
									|| "S4_CONSIG_ISSUE".equals(quote.getQtPrchOrdReqt().getFnModelGrpUUID())
									|| "S4_CONSIG_PICKUP".equals(quote.getQtPrchOrdReqt().getFnModelUUID())
									|| "S4_CONSIG_PICKUP".equals(quote.getQtPrchOrdReqt().getFnModelGrpUUID()))) 
									//End - US-17463 NGQC STARGAZER/CONSIGNMENT
							{
								long qtBitIndicator = quoteService.getOMHandler(quote).demoBuyoutChecks(quote, quoteItem, quoteCharBitLong);
								quoteCharBitLong = quoteCharBitLong | qtBitIndicator;
							}
							
						}
						//long qtBitIndicator = applicationService.getOrderMgmtApplnType().demoBuyoutChecks(quote, quoteItem, quoteCharBitLong);
						// quoteCharBitLong &= ~0b10;
					}
					
					if (AssetType.WATSON.getCode().equalsIgnoreCase(quote.getOrigAsset())) {
						if (applicationService.getOrderMgmtApplnType(quote).isDemoBuyoutQuote(quoteItem)) {
							quoteCharBitLong = quoteCharBitLong | QuoteCharBitConstants.DEMOBUYOUT_INDICATOR;
							isStandard = false;
						}
					}

					if (StringUtils.equalsIgnoreCase("true", isAruba)) {
						quoteCharBitLong = quoteCharBitLong | QuoteCharBitConstants.ARUBA_INDICATOR;
						isStandard = false;
						/*
						 * if (!"Watson".equalsIgnoreCase(quote.getOrigAsset()))
						 * { quoteCharBitLong = quoteCharBitLong |
						 * QuoteCharBitConstants.ORDERS05_INDICATOR ; } else {
						 * quoteCharBitLong = quoteCharBitLong |
						 * QuoteCharBitConstants.ORDERS04_INDICATOR ; }
						 */
					}									
					if (!isNonNetwork && !"Comment".equalsIgnoreCase(item.getLineType())
							&& item.getProductLine() != null && isOrigAssetExists) {
						// quoteCharBitLong = quoteCharBitLong |
						// QuoteCharBitConstants.MIXED_NW_ONLY_INDICATOR ;
						// isStandard = false ;
						egProductLineExists = true;
					}

					if (!CommonUtil.isEmptyString(quoteItem.getFanNr())) {
						quote.getCstmAtr().setAdditionalBitWiseFlags(
								CommonUtil.doOR(quote.getCstmAtr().getAdditionalBitWiseFlags(), 2L));
					}
					
					quote.setBitWiseQtChar(String.valueOf(quoteCharBitLong));
					if (CommonUtil.checkBRIMFlag(qidsQuote)) {
						QtFlag qtBrimFlag = new QtFlag();
						qtBrimFlag.setFlgVl("true");
						qtBrimFlag.setSlsQtnId(quote.getSlsQtnId());
						qtBrimFlag.setSlsQtnVrsnSqnNr(quote.getSlsQtnVrsnSqnNr());			
						qtBrimFlag.setQtFlagType(QtFlagType.BRIMQUOTE);				
						quote.getFlags().put(QtFlagType.BRIMQUOTE, qtBrimFlag);
					}
					applicationService.getOrderMgmtApplnType(quote).setProductNumberAndSerialNumber(quote, item, quoteItem);

					if (StringUtils.isNotEmpty(filter.getTempCbn())) {
						String cinv = "false";
						if (item.getSupplyingDiv() != null) {

							String cbn = filter.getTempCbn();

							Cinv cinvCd = applicationService.getOrderMgmtApplnType(quote).getSupplierCode(quote, cbn);

							if (cinvCd != null) {

								String supplyDiv = cinvCd.getSupplier();
								String productNr = cinvCd.getProductNumber();

								if (supplyDiv != null && supplyDiv.contains("~" + item.getSupplyingDiv() + "~")) {

									if (productNr != null) {
										if (ischeckProdcutNumber(quoteItem, productNr)) {
											cinv = "true";
										} else {
											cinv = "false";
										}

									} else {
										cinv = "true";

									}

								}

							}

						}
						quoteItem.setCinv(cinv);
					}
					//US-16853 call to setRussiaDelBlockFlag value based on alletra list validations
					if(alletraList!=null && !alletraList.isEmpty()) {
						quoteService.getOMHandler(quote).setRussiaDeliveryBlockFlag(alletraList,quoteItem,quote);
					}
					//Hwaas Code change to differentiate OrderBom & QuoteBom Products
					if ("Y".equalsIgnoreCase(qidsQuote.getQuoteHeader().getGlscFlag())) {
						if (("Config".equalsIgnoreCase(item.getLineType())
								&& !"END".equalsIgnoreCase(item.getBomType()))
						|| (!"Config".equalsIgnoreCase(item.getLineType()) 
								&& null != item.getOrderBom() 
								&& "Y".equalsIgnoreCase(item.getOrderBom()))) {
						quote.add(quoteItem);
					} else {
						if ("Config".equalsIgnoreCase(item.getLineType()) && "END".equalsIgnoreCase(item.getBomType()))
							endBOMCnfgList.add(quoteItem);
						continue;
					}
						} else {
						quote.add(quoteItem);
						}
					
				}
				
				//Hwaas Code change to differentiate OrderBom & QuoteBom Products
				if ("Y".equalsIgnoreCase(qidsQuote.getQuoteHeader().getGlscFlag())) {
					List<QuoteItem> itemList = new ArrayList<QuoteItem>(quote.getItems());
					List<QuoteItem> itemList2Dltd = new ArrayList<QuoteItem>();
					endBOMCnfgList.forEach(cnfgHdr -> {
						itemList2Dltd.addAll(itemList.stream().filter(eachItem -> StringUtils
								.equalsIgnoreCase(eachItem.getCnfgnSystemName(), cnfgHdr.getCnfgnSystemName()))
								.collect(Collectors.toList()));
					});
					if (!itemList2Dltd.isEmpty()) {
						quote.getItems().removeAll(itemList2Dltd);
					}
				}
				
				if (quote.isCiFlag()) {
					quoteService.getOMHandler(quote).getCIFlagStatus(quote);
				}

				if (egProductLineExists && (StringUtils.equalsIgnoreCase("true", isAruba)) && isOrigAssetExists) {
					quoteCharBitLong = quoteCharBitLong | QuoteCharBitConstants.MIXED_NW_ONLY_INDICATOR;
					isStandard = false;
					egProductLineExists = true;
				}

				if (isStandard) {
					quoteCharBitLong = quoteCharBitLong | QuoteCharBitConstants.STANDARD_ORDER_INDICATOR;
				}
				//based on demo flag enabling demobuyot financial model 
				
				/*if (AssetType.NGQ.getCode().equalsIgnoreCase(quote.getOrigAsset())
						|| AssetType.NGQ_Partner.getCode().equalsIgnoreCase(quote.getOrigAsset()) && (!AssetType.WATSON.getCode().equalsIgnoreCase(quote.getOrigAsset()))) {
					if (qidsQuote.getQuoteFlagsList() != null) {
						List<QuoteFlagType> quoteFlagsList = qidsQuote.getQuoteFlagsList().getQuoteFlags();
						for (QuoteFlagType quoteFlagType : quoteFlagsList) {
							if (quoteFlagType != null
									&& StringUtils.equalsIgnoreCase(quoteFlagType.getFlagType(), "demoBuyOut")
									&& StringUtils.equalsIgnoreCase(quoteFlagType.getFlagValue(), "true")) {
								quoteCharBitLong = quoteCharBitLong | QuoteCharBitConstants.DEMOBUYOUT_INDICATOR;
								break;
							}
						}
					}
				}*/
				
				quote.setBitWiseQtChar(String.valueOf(quoteCharBitLong));

				QtFlag qtFlag = new QtFlag();
				qtFlag.setFlgVl(isAruba);			
				qtFlag.setSlsQtnId(quote.getSlsQtnId());
				qtFlag.setSlsQtnVrsnSqnNr(quote.getSlsQtnVrsnSqnNr());			
				qtFlag.setQtFlagType(QtFlagType.ARUBA);				
				quote.getFlags().put(QtFlagType.ARUBA, qtFlag);
				
				QtFlag nimbleFlag = new QtFlag();
				nimbleFlag.setFlgVl(isNimble);
				nimbleFlag.setSlsQtnId(quote.getSlsQtnId());
				nimbleFlag.setSlsQtnVrsnSqnNr(quote.getSlsQtnVrsnSqnNr());
				nimbleFlag.setQtFlagType(QtFlagType.NIMBLE);
				quote.getFlags().put(QtFlagType.NIMBLE, nimbleFlag);				

				QtFlag distributorTotalFlag = new QtFlag();
				distributorTotalFlag.setSlsQtnId(quote.getSlsQtnId());
				distributorTotalFlag.setSlsQtnVrsnSqnNr(quote.getSlsQtnVrsnSqnNr());
				distributorTotalFlag.setFlgVl("false");
				distributorTotalFlag.setQtFlagType(QtFlagType.SHOWDISTRIBUTORAMT);
				quote.getFlags().put(QtFlagType.SHOWDISTRIBUTORAMT, distributorTotalFlag);
				
				setBaseHeartItemNumber(quote.getItems()) ;
				applicationService.getOrderMgmtApplnType(quote).setPrcssAcnIndicator(quote);
				// }

			}
			
			if(StringUtils.isEmpty(filter.getTempCbn()) && StringUtils.isNotEmpty(filter.getCbn())){
				filter.setTempCbn(filter.getCbn());
			}
			// Internal Demo buyout changes
			if(!quote.getQtPrchOrdReqt().isSerpFlag()) {
				if (StringUtils.isNotEmpty(filter.getTempCbn())) {
					quote.setCbn(filter.getTempCbn());
					List<InternalDemoModel> internalDemoCbnList = dropDownListService.getInternalDemoOrderCBNInfo(quote);
					InternalDemoModel pickedModel = null;
					if (internalDemoCbnList != null && !internalDemoCbnList.isEmpty()) {
						pickedModel = internalDemoCbnList.get(0);
						if (pickedModel.getFnMdlTyp() != null && !pickedModel.getFnMdlTyp().isEmpty()
								&& StringUtils.equalsIgnoreCase(filter.getRtm(), "VALUE_DIRECT")) {
							switch(pickedModel.getFnMdlTyp()){
							case "CNTRCT_ORD" :
								setContractOrderFlag(quote);
								//qidsQuoteService.setContractStartAndEndDate(quote);
								break;
							case "S4_NPI" :
								quoteService.getOMHandler(quote).setNPIAttributes(quote, filter);
								break;
							}
						} else if (pickedModel.getFnMdlTyp() != null && !pickedModel.getFnMdlTyp().isEmpty()
								&& StringUtils.equalsIgnoreCase(filter.getRtm(), "VALUE_INDIRECT")){
							
						}else {
						// applicationService.getOrderMgmtApplnType().integrateInternalDemoOrderData(internalDemoCbnList,quote);
						List<InternalDemoModel> internalDemoCbnList1 = frictionlessOrderService.getIDODefaultData();
						applicationService.getOrderMgmtApplnType(quote).integrateInternalDemoOrderData(internalDemoCbnList1,
								quote);
						setInternalDemoFlag(quote);
						}
						// quote.setOrderRegionCode(internalDemoCbnList.get(0).getReasonCD());
						// quote.setFunctionalModelCd("Intrnl_Dm_Ordr");
						// quote.getQtPrchOrdReqt().setFnModelGrpUUID("Intrnl_Dm_Ordr");

					}
				}
			}
			
			// done	
			
			
			applicationService.getOrderMgmtApplnType(quote).setFulfillingInterfaces(quote);

			boolean pickBlanketFan = false ;
			
			if (!CommonUtil.isEmptyString(quote.getFanNr())) {
				quote.getCstmAtr()
						.setAdditionalBitWiseFlags(CommonUtil.doOR(quote.getCstmAtr().getAdditionalBitWiseFlags(), 1L));
				List<String> fanList = CommonUtil.smartSplit(quote.getFanNr(), ",") ;
				if (fanList != null && fanList.size() > 1) {
					Long additionalBitWiseFlagsLong = NumberUtils.toLong(quote.getCstmAtr().getAdditionalBitWiseFlags());
					additionalBitWiseFlagsLong = additionalBitWiseFlagsLong | 8;
					additionalBitWiseFlagsLong &= ~0b01;					
					quote.getCstmAtr().setAdditionalBitWiseFlags(String.valueOf(additionalBitWiseFlagsLong));					
					pickBlanketFan = true ;
				}
			}

			if (CommonUtil.AND(quote.getCstmAtr().getAdditionalBitWiseFlags(), 2L)
					&& CommonUtil.isEmptyString(quote.getFanNr())) {
				pickBlanketFan = true ;
			}
			
			if (pickBlanketFan) {
				LocalizationFilter filterbase = applicationService.getLocalizationFilterByCookies(quote);
				List<FanModel> fanModelList = dropDownListService.getFanModelList(filterbase);
				if (fanModelList != null && !fanModelList.isEmpty()) {
					quote.setFanNr(fanModelList.get(0).getFanNrCd());
				}
			}

			LOG.debug("PrepareOMUIQuote >> convertToQuote >> getQTContactInfo");
			applicationService.getOrderMgmtApplnType(quote).getQTContactInfo(quote, qidsQuote);
			
			LOG.debug("PrepareOMUIQuote >> convertToQuote >> getQidsContactInfo");
			applicationService.getOrderMgmtApplnType(quote).getQidsContactInfo(quote, qidsQuote);

			LOG.debug("PrepareOMUIQuote >> convertToQuote >> getQTContactInfoEndCustAndReseller");
			//applicationService.getOrderMgmtApplnType().getQTContactInfoEndCustAndReseller(quote, qidsQuote);
			quoteService.getOMHandler(quote).getQTContactInfoEndCustAndReseller(quote, qidsQuote);
			
			LOG.debug("PrepareOMUIQuote >> convertToQuote >> getAttachmentList");
			applicationService.getOrderMgmtApplnType(quote).getAttachmentList(quote, qidsQuote);
			
			LOG.debug("PrepareOMUIQuote >> convertToQuote >> getUpdateFlag");
			applicationService.getOrderMgmtApplnType(quote).getUpdateFlag(quote, qidsQuote);	
			setSalesOrgFromQids(quote, qidsQuote.getQuoteHeader());

		}
		//moved to set brim route switch based on GLSC flag (US-18112)
		//Hwaas: Setting the GLSC flag value from Quote
		if (qidsQuote.getQuoteHeader().getGlscFlag() != null && "Y".equalsIgnoreCase(qidsQuote.getQuoteHeader().getGlscFlag())) {
					QtFlag glscFlag = new QtFlag();
					glscFlag.setFlgVl("true");
					glscFlag.setSlsQtnId(quote.getSlsQtnId());
					glscFlag.setSlsQtnVrsnSqnNr(quote.getSlsQtnVrsnSqnNr());
					glscFlag.setQtFlagType(QtFlagType.GLSCFLAG);
					quote.put(QtFlagType.GLSCFLAG, glscFlag);
		}
		//Populate MSP flag from QIDS quote to NGQC quote
		quoteService.populateMspFlag(quote, qidsQuote.getQuoteFlagsList().getQuoteFlags());
		//Populate FSE Exception Flag
	    LOG.debug("PrepareOMUIQuote >> convertToQuote >> setFSEFlag");
		quoteService.populateFSEExceptionFlag(quote, qidsQuote.getQuoteFlagsList().getQuoteFlags());
		//Populate Eagle flag from QIDS quote to NGQC quote
		quoteService.populateEagleFlag(quote, qidsQuote.getQuoteFlagsList().getQuoteFlags());
		// US-18277 - NNLFlag & PCT changes to populate NNL flag from QIDS quote to NGQC quote
		quoteService.populateNNLFlag(quote, qidsQuote.getQuoteFlagsList().getQuoteFlags());
		//US-18504 - ScalablePricingFlag changes to populate ScalablePricingFlag from QIDS quote to NGQC quote
		quoteService.populateScalablePriceHeaderFlag(quote, qidsQuote.getQuoteFlagsList().getQuoteFlags());
		/**
		 * US-18661 Cost Relief Flag.
		 * Populates costReliefEligible header flag for all manual order types (WW, Direct, Indirect).
		 * No source-system restriction applies for manual orders.
		 * For electronic orders this flag is populated in QuoteBasedEorderTypeHandler (EDI Quoted only).
		 */
		quoteService.populateCostReliefFlag(quote, qidsQuote.getQuoteFlagsList().getQuoteFlags());
		LOG.debug("US-18661 costReliefEligible : " + (quote.getFlags().get(QtFlagType.costReliefEligible) != null ? quote.getFlags().get(QtFlagType.costReliefEligible).getFlgVl() : null));
		//setting the RouteToBrimSwitchFlag
		LOG.debug("PrepareOMUIQuote >> convertToQuote >> setRouteToBrimSwitchFlag");
		// US-17712  NGQC QUOTE SPLIT ROUTING .. Polymorphic routing to SERP flow only
		quoteService.getOMHandler(quote).setRouteToBrimSwitchFlag(quote, qidsQuote.getQuoteFlagsList().getQuoteFlags());		
		
		//US 8932 code commneted
	//	setPricingFlg(quote, qidsQuote);
		
		LOG.debug("PrepareOMUIQuote >> convertToQuote >> setNGQFlags");
		applicationService.getOrderMgmtApplnType(quote).setNGQFlags(qidsQuote, quote);

		/*checking before split
		 * LOG.debug("PrepareOMUIQuote >> convertToQuote >> spilt Nimble quote");
		applicationService.getOrderMgmtApplnType().splitNimbleQuote(quote);*/
		
		LOG.debug("PrepareOMUIQuote >> convertToQuote >> splitOrderByQuantity");
		//applicationService.getOrderMgmtApplnType().splitOrderByQuantity(quote);
		
		LOG.debug("PrepareOMUIQuote >> convertToQuote >> splitOrderBySize");
		/*
		 *  US-17976 NGQC HWAAS_038 MULTISHIPTO FOR GLSS QUOTES
		 *  Setting GLSS flag in NGQC quote object
		 */
		quoteService.populateQidsFlags2NGQC(quote, qidsQuote.getQuoteFlagsList().getQuoteFlags(), QtFlagType.glssmultisite) ;
		//US-18521: IQ for aas ECSRD changes - To read QuoteHeader RDD date and pass to section lever if ECSRD is blank from upstream - starts
		if(filter.getPoRcvDt() != null){
			LOG.info("PrepareOMUIQuote .convertToQuote >> PO RDDate"+filter.getPoRcvDt());
				try {
					GregorianCalendar reqDelDate = new GregorianCalendar();
					reqDelDate.setTime(filter.getPoRcvDt());
					quote.getCstmAtr().setCustRqstDlvryDt(CommonUtil.stripTimeZonePreserveDate(reqDelDate));
					LOG.info("PrepareOMUIQuote.convertToQuote >> Quote: " + quote.getAssetQuoteNrAndVrsn() + "  >> PO Request Header RDDate: "+quote.getCstmAtr().getCustRqstDlvryDt());
				} catch (Exception e) {
					LOG.error("PrepareOMUIQuote >> convertToQuote >> Error parsing CustomerRequestedDeliveryDate: " + e.getMessage(), e);
				}
			}
		//US-18521: IQ for aas ECSRD changes - To read QuoteHeader RDD date and pass to section lever if ECSRD is blank from upstream - ends
		quoteService.populateQidsFlags2NGQC(quote, qidsQuote.getQuoteFlagsList().getQuoteFlags(), QtFlagType.IQASSESCRD) ;   //US-18521: IQ for aas ECSRD
		applicationService.getOrderMgmtApplnType(quote).splitOrderBySize(quote);
		LOG.debug("PrepareOMUIQuote >> convertToQuote >> populateSectionId");
		applicationService.getOrderMgmtApplnType(quote).populateSectionId(quote);
		
		LOG.debug("PrepareOMUIQuote >> convertToQuote >> updateSectionIdOnWatsonItems");
		applicationService.getOrderMgmtApplnType(quote).updateSectionIdOnWatsonItems(quote);
		
		LOG.debug("PrepareOMUIQuote >> convertToQuote >> unsetCustomerAddressState");
		/*
		 *  US-17976 NGQC HWAAS_038 MULTISHIPTO FOR GLSS QUOTES
		 *  For Glss Quote, Customers and Address will be populated before this method call
		 *  so should not be resetting the state value for glss.
		 */
		if (!quoteService.checkFlagValueIsPresent(quote, QtFlagType.glssmultisite)) {
		applicationService.getOrderMgmtApplnType(quote).unsetCustomerAddressState(quote, qidsQuote, CustomerType.SHIPTO);
		}
		
		LOG.debug("PrepareOMUIQuote >> convertToQuote >> verifyEsdAndCarePackItems");
		applicationService.getOrderMgmtApplnType(quote).verifyEsdAndCarePackItems(quote);
		
		LOG.debug("PrepareOMUIQuote >> convertToQuote >> setCustomerPrefence");
		applicationService.getOrderMgmtApplnType(quote).setCustomerPrefence(quote);

		LOG.debug("PrepareOMUIQuote >> convertToQuote >> setBitWiseChar");
		applicationService.getOrderMgmtApplnType(quote).setBitWiseChar(quote);

		LOG.debug("PrepareOMUIQuote >> convertToQuote >> getConditionTypeForMccCode");
		
		if(applicationService.getOrderMgmtApplnType(quote).isTrustedPricingApplicable(quote)){
			// For MCC Code Update of MCC Code to be sent to Downstream system 
		    iMccConditionTypeService.getConditionTypeForMccCode(quote);
		}
		
		LOG.debug("PrepareOMUIQuote >> updateItemDetailsForSplitQuote >> updateObjectsRelatedToItemForSplit");
		applicationService.getOrderMgmtApplnType(quote).updateObjectsRelatedToItemForSplit(quote);
		
		/*
		 * IOrderMgmtServices orderMgmtServices =
		 * applicationService.getOrderMgmtApplnType(); try { String[] asyncTasks
		 * = {"verifyEsdAndCarePackItems", "setCustomerPrefence",
		 * "setBitWiseChar"}; List<Future<String>> responseArr = new
		 * ArrayList<Future<String>>(); responseArr.clear(); for (int i=0; i <
		 * asyncTasks.length; i++) { try { responseArr.add(
		 * taskExecutor.submit(new ConvertQuoteAsyncProcessor(asyncTasks[i],
		 * RequestContextHolder.currentRequestAttributes(), orderMgmtServices,
		 * quote)) ); } catch (TaskRejectedException re) { LOG.debug("Task "
		 * +asyncTasks[i]+" go sleep for a second to acquire thread");
		 * Thread.sleep(1000); responseArr.add( taskExecutor.submit(new
		 * ConvertQuoteAsyncProcessor(asyncTasks[i],
		 * RequestContextHolder.currentRequestAttributes(), orderMgmtServices,
		 * quote)) ); } } for (Future<String> response : responseArr) {
		 * LOG.debug(response.get() + " completed."); } } catch
		 * (InterruptedException | ExecutionException e) {
		 * LOG.throwException(new SystemApplicationException(8888,
		 * "Application System Exception", e)); }
		 */

		// OEM Changes
		if (StringUtils.equalsIgnoreCase(qidsQuote.getQuoteHeader().getOriginatingQuoteSystem(),
				Constants.PrepareConstantAttributes.NGQ)
				|| StringUtils.equalsIgnoreCase(qidsQuote.getQuoteHeader().getOriginatingQuoteSystem(),
						Constants.PrepareConstantAttributes.NGQ_PARTNER)) {
			addFlags(qidsQuote, quote);
			// setVistaFlags(qidsQuote, quote);
			setDynamicConfigValue(quote);
		}
		
		
		
		applicationService.getOrderMgmtApplnType(quote).splitCustomerAddressStreet(quote);
		
		
		//check box ticked in Ngqc if it is selected in NGQ 
		//applicationService.getOrderMgmtApplnType().setShipHandExemptnFlg(quote, qidsQuote);	
		quoteService.getOMHandler(quote).setShipHandExemptnFlg(quote, qidsQuote);
		//quoteService.getOMHandler(quote).setShippingAndHandlingAmount(quote);
		
		applicationService.getOrderMgmtApplnType(quote).setSplitFlags(quote);
		
		applicationService.getOrderMgmtApplnType(quote).setExternalAndQuoteCompleteComment(quote, qidsQuote);
		
		if ( !quote.getQtPrchOrdReqt().isSerpFlag()) {
			applicationService.getOrderMgmtApplnType(quote).setLoanNumberInCustomerComments(quote);}
		//IN:782202 US-17406 polymorphically routing to S4 WW and EMEA legacy flow          
        quoteService.getOMHandler(quote).setNokiaFlag(quote);
		
		applicationService.getOrderMgmtApplnType(quote).setNokiaFlag(quote);
		if(!quote.getQtPrchOrdReqt().isSerpFlag())
			applicationService.getOrderMgmtApplnType(quote).setSRCodeAndSalesOrg(quote);
		
		applicationService.getOrderMgmtApplnType(quote).setShippingDeliveryServices(quote);
		
		applicationService.getOrderMgmtApplnType(quote).setShippingSepcialtyServices(quote);
		
		//applicationService.getOrderMgmtApplnType(quote).setDefaultValue(quote);
		
		quoteService.getOMHandler(quote).setDefaultValue(quote);
		// US-18306 setTariffExemptFlag
		quoteService.getOMHandler(quote).setTariffExemptFlag(quote, qidsQuote);
		/**
		 * US-18224 EDUP Flag
		 * Setting the EDUPFlag in NGQC Quote
		 */
		quoteService.getOMHandler(quote).setEduPFlag(quote, qidsQuote);

		applicationService.getOrderMgmtApplnType(quote).setContextualQuoteDefaults(quote);
		
		quoteService.getOMHandler(quote).setCipPgmQuoteFlag(quote, qidsQuote);
		
		//Customer logic started		
		quoteService.getOMHandler(quote).setPartyIdAddressInfo(quote, qidsQuote, filter);		
		
		LOG.debug("PrepareOMUIQuote >> convertToQuote >> copyCustomersToSectionCustomers");
		applicationService.getOrderMgmtApplnType(quote).copyCustomersToSectionCustomers(quote);	
		
		quoteService.getOMHandler(quote).setPersonIdForMDCPContactInfo(quote, qidsQuote);
		
		quoteService.getOMHandler(quote).setTAAFields(quote, qidsQuote);
		
		//Setting FlgVl field of QtFlag to true for S4 Quote Item Care Pack
		quoteService.getOMHandler(quote).verifyEsdAndCarePackItems(quote);	
		
		quoteService.getOMHandler(quote).setRecyFeeExemptFlag(quote, qidsQuote);
		
		// US-14546 - S4 Orders Services-Self service change order capability _ CPQE2394
		if(quote.getQtPrchOrdReqt().isSerpFlag()) {
			applicationService.setItemRefNumber(quote.getItems());
			//quoteItemService.setItemReferenceNumber(quote.getItems());
		}
	//	getAllSelectedValidationSummary(quote) ;
		//frictionlessOrderService.getAllSelectedValidationSummary(quote);
					
		quoteService.getOMHandler(quote).setDeliveryTerms(quote, qidsQuote);
		
		quoteService.getOMHandler(quote).isCSPMandatory(quote);
		
		quoteService.getOMHandler(quote).copySoldToCspTab(quote);
		
		//getting HpeSolution and Green Lake Flag
		if(qidsQuote != null && qidsQuote.getQuoteHeader()!= null) {
		setHpeSolAndGlFlg(quote, qidsQuote, filter.getPoNr());
		// US-18651
			quote.setDealQuoteInfo(new DealQuoteInfo());

			// Set Deal Expiry Timestamp with UTC timezone handling
			if(qidsQuote.getQuoteHeader().getDealEndDate() != null) {
				GregorianCalendar dealEndDateCal = myWorkspaceService.getXMLGregorianCalendar(qidsQuote.getQuoteHeader().getDealEndDate());
				quote.getDealQuoteInfo().setDealExpiryTs(dealEndDateCal.getTime());
			}
		}

	return quote;
	}
	
	private void setHpeSolAndGlFlg(Quote quote, HPQuote qidsQuote, String poNr) {
		if (qidsQuote.getQuoteHeader().getHpeSolution() != null) {
			quote.getQtPrchOrdReqt().setHpeSolution(qidsQuote.getQuoteHeader().getHpeSolution());
		}
//Aldea #1547820 NGQC GL flag
		/*
		 * CR 167822 Brin Quote to honor from Quote and not based on FCS
		 */
		if (CommonUtil.checkBRIMFlag(qidsQuote)) {
			quoteService.getOMHandler(quote).setGLflag(quote, qidsQuote) ;
		} else {
			String poStartsWith = CacheConfig.getValuee("ENABLE_GL_FLAG_WITH_PO_NUMBER"); // FCS~BaaS
			if (!StringUtils.isEmpty(poStartsWith)) {
				String[] searchStrings = poStartsWith.split("~");
				//Iterating through a list of Strings and ignoring the case of the prefix
				for (String prefix : searchStrings) {
					if (StringUtils.startsWithIgnoreCase(poNr, prefix)) {
						QtFlag qtFlag = new QtFlag();
						qtFlag.setFlgVl("true");
						qtFlag.setSlsQtnId(quote.getSlsQtnId());
						qtFlag.setSlsQtnVrsnSqnNr(quote.getSlsQtnVrsnSqnNr());
						qtFlag.setQtFlagType(QtFlagType.GLFLAG);
						quote.getFlags().put(QtFlagType.GLFLAG, qtFlag);
						break;
					}
				}
			}
		}

	}
		
	private boolean getSerpFlagLocal(QuoteFlags qidsFlagList) {
		boolean retFlg = false;

		if (qidsFlagList == null || qidsFlagList.getQuoteFlags() == null) {
			return retFlg;
		}
		for (QuoteFlagType qtFlag : qidsFlagList.getQuoteFlags()) {

			if (qtFlag != null && StringUtils.equalsIgnoreCase(qtFlag.getFlagType(), "serpFlag")
					&& StringUtils.equalsIgnoreCase(qtFlag.getFlagValue(), "true")) {

				retFlg = true;
				break;

			}
		}
		return retFlg;
	}
	
	private void setPricingFlg(Quote quote, HPQuote qidsQuote) {

		QtFlag qtFlag = new QtFlag();
		qtFlag.setQtFlagType(QtFlagType.FALLBACKLOGICENABLED);
		qtFlag.setFlgVl("false");

		if (qidsQuote.getQuoteFlagsList() != null) {
			List<QuoteFlagType> quoteFlagsList = qidsQuote.getQuoteFlagsList().getQuoteFlags();
			for (QuoteFlagType quoteFlagType : quoteFlagsList) {
				if (quoteFlagType != null
						&& StringUtils.equalsIgnoreCase(quoteFlagType.getFlagType(), "fallBackLogicEnabled")) {
					qtFlag.setFlgVl(quoteFlagType.getFlagValue());
				}
			}
		}
		quote.getFlags().put(QtFlagType.FALLBACKLOGICENABLED, qtFlag);

	}
	
	private void setBaseHeartItemNumber(List<QuoteItem> items) {
		List<QuoteItem> mainItemList = new ArrayList<QuoteItem>() ;
		List<QuoteItem> childItemList = new ArrayList<QuoteItem>() ;
		for (QuoteItem eachItem : items) {
			//if (CommonUtil.greaterThanZero( CommonUtil.zeroOnNull( eachItem.getLclUntNtAmt()) ) ) 
			{
				if (StringUtils.isNotEmpty(eachItem.getHeartLineItemNr()) 
						/*&&	( StringUtils.equalsIgnoreCase( eachItem.getProductClassCode(), "HW" ) 
								|| 	StringUtils.equalsIgnoreCase( eachItem.getProductClassCode(), "SW" ) ) */ ) {
					mainItemList.add(eachItem) ;
				}
				if (StringUtils.isEmpty(eachItem.getHeartLineItemNr() ) ) { 
					childItemList.add(eachItem) ;
				}	
			}
		}		
		
		for (QuoteItem mainItem : mainItemList) {
			mainItem.setBaseItemHeartLineItemNr(mainItem.getHeartLineItemNr());
			for (QuoteItem childItem : childItemList) {
				if (StringUtils.equalsIgnoreCase( childItem.getProductNr() , mainItem.getProductNr() )
						&&  StringUtils.equalsIgnoreCase( mainItem.getSlsQtnItmSqnNr(),childItem.getCnfgnParentLineItemId() ) ) {
					childItem.setBaseItemHeartLineItemNr(mainItem.getHeartLineItemNr());
				}
			}
		}
	}

	private void captureHeaderFanDetails(Quote quote, HPQuote qidsQuote) {
		if (quote == null || qidsQuote == null || qidsQuote.getCommentsList() == null) {
			return;
		}

		for (CommentType cmtType : qidsQuote.getCommentsList().getComments()) {
			if (cmtType != null) {
				if (NGQConstants.HEADERFANID.equalsIgnoreCase(cmtType.getType())) {
					quote.setFanNr(cmtType.getComment());
					quote.setSrcFanNr(cmtType.getComment());
					break;
				}
			}
		}
	}

	// added for OEM bundle change
	private void addFlags(HPQuote qidsQuote, Quote quote) {

		if (qidsQuote.getCommentsList() != null) {
			Comments qidComments = qidsQuote.getCommentsList();
			if (qidComments != null && qidComments.getComments() != null && !qidComments.getComments().isEmpty()) {

				for (CommentType type : qidComments.getComments()) {
					if (StringUtils.equalsIgnoreCase(type.getType(), Constants.PrepareConstantAttributes.C2BFLAG)) {
						QtFlag flagValue = new QtFlag();
						flagValue.setQtFlagType(QtFlagType.fromString(type.getType()));
						// flagValue.setFlgVl(BooleanUtils.toString(type.isExternalFlag(),
						// "true", "false"));
						String comments = Constants.PrepareConstantAttributes.STR_FALSE;
						if (type.getComment() != null) {
							if (StringUtils.equalsIgnoreCase(type.getComment(), "Y")) {
								comments = Constants.PrepareConstantAttributes.STR_TRUE;
							} else {
								comments = Constants.PrepareConstantAttributes.STR_FALSE;
							}
						}
						flagValue.setFlgVl(comments);
						quote.getFlags().put(QtFlagType.fromString(type.getType()), flagValue);
						break;
					}
				}
			}
		}

	}

	private void setVistaFlags(HPQuote qidsQuote, Quote quote) {
		
		List<String> mdcpOrgIdList=dropDownListService.getMdcpOrgIdList();
		if((qidsQuote.getQuoteHeader() != null && StringUtils.equalsIgnoreCase(qidsQuote.getQuoteHeader().getOriginatingQuoteSystem(), Constants.PrepareConstantAttributes.NGQ)) 
				&& qidsQuote.getQuoteHeader().getSoldTo()!=null && qidsQuote.getQuoteHeader().getSoldTo().getCompany()!=null && qidsQuote.getQuoteHeader().getSoldTo().getCompany().getMDCPOrgID()!=null && mdcpOrgIdList.contains(qidsQuote.getQuoteHeader().getSoldTo().getCompany().getMDCPOrgID().toString())){
			QtFlag flagValue = new QtFlag();
			flagValue.setQtFlagType(QtFlagType.fromString(Constants.PrepareConstantAttributes.VISTA));
			flagValue.setFlgVl("true");
			quote.getFlags().put(QtFlagType.fromString(Constants.PrepareConstantAttributes.VISTA), flagValue);
		}
	}
	
	/*private void setVistaFlags(HPQuote qidsQuote, Quote quote) {
		String origAssetRef="";
		
		Map<String,String> omuiSrvcCtrlMap=dropDownListService.getOmuiservicekeyValues();
		origAssetRef=omuiSrvcCtrlMap.get("VISTA_ORIG_ASSET");
		
		if(StringUtils.isEmpty(origAssetRef)){
			origAssetRef=Constants.PrepareConstantAttributes.VISTA_ASSET;
		}
		
		if((qidsQuote.getQuoteHeader() != null && StringUtils.equalsIgnoreCase(qidsQuote.getQuoteHeader().getOriginatingQuoteSystem(), Constants.PrepareConstantAttributes.NGQ)) && StringUtils.contains(origAssetRef,"~"+qidsQuote.getQuoteHeader().getOriginatingReferenceAsset()+"~") ){
			QtFlag flagValue = new QtFlag();
			flagValue.setQtFlagType(QtFlagType.fromString(Constants.PrepareConstantAttributes.VISTA));
			flagValue.setFlgVl("true");
			quote.getFlags().put(QtFlagType.fromString(Constants.PrepareConstantAttributes.VISTA), flagValue);
		}
	}*/

	/*
	 * private void addPOAttach(HPQuote qidsQuote, Quote quote) {
	 * 
	 * Attachments qidsAtts= qidsQuote.getAttachmentsList();
	 * 
	 * if(qidsAtts.getAttachments()!=null &&
	 * !qidsAtts.getAttachments().isEmpty() ) { for(AttachmentType type :
	 * qidsAtts.getAttachments() ) {
	 * 
	 * } }
	 * 
	 * }
	 */

	/*
	 * private void addComments(HPQuote qidsQuote, Quote quote) {
	 * 
	 * if(qidsQuote.getCommentsList() !=null) {
	 * 
	 * Comments qidComments= qidsQuote.getCommentsList(); if(qidComments !=null
	 * && qidComments.getComments()!=null &&
	 * !qidComments.getComments().isEmpty() ) { Map<QtCmtType,QtCmt>
	 * omuiComments = new HashMap<QtCmtType,QtCmt>();
	 * 
	 * for(CommentType type : qidComments.getComments()) { QtCmt cmt = new
	 * QtCmt(); cmt.setCmtTxt1(type.getComment()); //type.isExternalFlag();
	 * omuiComments.put(QtCmtType.fromString(type.getType()), cmt);
	 * 
	 * } quote.setComments(omuiComments); }
	 * 
	 * }
	 * 
	 * }
	 */

	/*
	 * private void addFlags(HPQuote qidsQuote, Quote quote) {
	 * 
	 * if(qidsQuote.getQuoteFlagsList() != null) { QuoteFlags flags=
	 * qidsQuote.getQuoteFlagsList(); if(flags !=null &&
	 * flags.getQuoteFlags()!=null && !flags.getQuoteFlags().isEmpty() ) {
	 * Map<QtFlagType,QtFlag> omuiFlags = new HashMap<QtFlagType,QtFlag>();
	 * 
	 * for(QuoteFlagType type : flags.getQuoteFlags()) { QtFlag flagValue= new
	 * QtFlag(); flagValue.setFlgVl(type.getFlagValue());
	 * omuiFlags.put(QtFlagType.fromString(type.getFlagType()), flagValue);
	 * 
	 * } quote.setFlags(omuiFlags); } }
	 * 
	 * }
	 */

	public Quote convertQuote(QuoteHeader qidsQuote, QidsFilter filter, boolean serpFlag) {
		Quote quote = new Quote();
		quote.getQtPrchOrdReqt().setSerpFlag(serpFlag);
		quote.getQtPrchOrdReqt().setNewQuote(true);
		quote.getQtPrchOrdReqt().setFlowType(filter.getFlowType());
		if (qidsQuote == null) {
			LOG.debug("ERROR: Do not expect a null quote object when converting from qids.");
			return quote;
		}
		if (qidsQuote.getOriginatingSystemQuoteID() == null) {
			LOG.throwException(new BusinessApplicationException(3024, "Read of quote from QIDS failed. "));
		}

		if (qidsQuote.getTotalWeight() != null) {
			quote.setAirpackedTotalweight(String.valueOf(qidsQuote.getTotalWeight()));
		}
		applicationService.getWeightHandler().setWeightAttributes(quote);
	//	applicationService.getOrderMgmtApplnType(quote).setQuoteCountry(quote, qidsQuote.getQuoteCountry());
		
		quote.setCountryCd(qidsQuote.getQuoteCountry());
		
		quote.setAmpId(qidsQuote.getAmpID());
		// applicationService.getOrderMgmtApplnType().setOMTypCd(quote) ;
		// quote.setCountryCd(qidsQuote.getQuoteCountry());
		// quote.setApplCodeHorizontal(qidsQuote.getApplCodeHorizontal());
		quote.setApplCodeVertical(qidsQuote.getApplCodeVertical());
		quote.setAssetInstance(qidsQuote.getAssetInstance());
		quote.setAssetQuoteNr(qidsQuote.getOriginatingSystemQuoteID());
		// US-17359 NGQC Segmentation : Setting Business Group from QIDS
		quote.setBusinessGroup(qidsQuote.getBusinessGroup());
		quote.setAssetQuoteNrAndVrsn(qidsQuote.getOriginatingSystemQuoteAndVersionID());
		quote.setAssetQuoteVrsn(qidsQuote.getOriginatingSystemQuoteVersion());
		quote.setCommercialityCode(qidsQuote.getCommercialityCode());
		quote.setCompletionTs(CommonUtil
				.convertToDateTime(myWorkspaceService.getXMLGregorianCalendar(qidsQuote.getQuoteCompletionDate())));
		quote.setContractEndDate(CommonUtil
				.convertToDateTime(myWorkspaceService.getXMLGregorianCalendar(qidsQuote.getContractEndDate())));
		quote.setContractStartDate(CommonUtil
				.convertToDateTime(myWorkspaceService.getXMLGregorianCalendar(qidsQuote.getContractStartDate())));
		quote.setCreatedBy(qidsQuote.getCreatedBy());
		quote.setCreationPersonId(qidsQuote.getCreatedBy());
		quote.setModifiedTs(CommonUtil.convertToDateTime(
				myWorkspaceService.getXMLGregorianCalendar(qidsQuote.getOriginatingAssetModificationDate())));
		quote.setModifiedPersonId(qidsQuote.getModifiedBy());
		// quote.setCreatedTs(CommonUtil.convertToDateTime(qidsQuote.getQuoteStartDate()!=null?qidsQuote.getQuoteStartDate().toGregorianCalendar():null));
		quote.setCreatedTs(CommonUtil.convertToDateTime(
				myWorkspaceService.getXMLGregorianCalendar(qidsQuote.getOriginatingAssetCreationDate())));
		quote.setDealNr(qidsQuote.getEclipseDealId());
		quote.setDealVersionNr(qidsQuote.getDealVersion());
		quote.setDistributorTotal(CommonUtil.roundTo2Decimals(qidsQuote.getTotalNetPricewithBenefits()));
		quote.setDealPhase(qidsQuote.getDealPhase());
		quote.setDeliveryServices(qidsQuote.getDeliveryServices());
		quote.setSpecialtyServices(CommonUtil.getValueFromJSON(qidsQuote.getAdditionalInfo(), "specialityServiceDesc"));
		quote.setDealSource(CommonUtil.getValueFromJSON(qidsQuote.getAdditionalInfo(), "dealSource"));
		//quote.setDeliveryTerms(qidsQuote.getTermsOfDelivery());
		setDeliverySpeed(quote, qidsQuote);
		quote.setDiscountAmt(CommonUtil.roundTo2Decimals(qidsQuote.getDiscountAmt()));
		quote.setDiscountPct(CommonUtil.roundTo2Decimals(qidsQuote.getDiscountPct()));
		quote.setExpiringContractNr(qidsQuote.getExpiringContractNumber());
		quote.setGroupContractNr(qidsQuote.getGroupContract());
		quote.setLastModifiedBy(qidsQuote.getModifiedBy());
		quote.setLanguageCd(qidsQuote.getLanguageCd());
		// quote.setLastPricedDt(CommonUtil.convertToDateTime(qidsQuote.getLastPriceDate()).toGMTString());
		Date lastPricedDt = CommonUtil
				.convertToDateTime(myWorkspaceService.getXMLGregorianCalendar(qidsQuote.getLastPriceDate()));
		quote.setLastPricedDt(CommonUtil.convertDateFormat(lastPricedDt, "dd-MMM-yy"));
		quote.setLastPricedDate(lastPricedDt);
		quote.setMultiyearInd(qidsQuote.getMultiyearIndicator());
		quote.setName(qidsQuote.getQuoteName());
		quote.setOpportunityId(qidsQuote.getOpportunityId());
		quote.setOrigAsset(qidsQuote.getOriginatingQuoteSystem());
        if ("eprime".equalsIgnoreCase(qidsQuote.getOriginatingQuoteSystem())) {
            AssetType origAssetType = AssetType.fromString(qidsQuote.getOriginatingQuoteSystem()) ;
            if (origAssetType != null) {
                   quote.setOrigAsset(origAssetType.getCode());
            }
        }
		/* if ("eprime".equalsIgnoreCase(qidsQuote.getOriginatingQuoteSystem())) {
			 quote.setOrigAsset("NGQ");
		 }else{
			 quote.setOrigAsset(qidsQuote.getOriginatingQuoteSystem());
		 } */
		
		
		quote.setOriginatingReferenceAsset(qidsQuote.getOriginatingReferenceAsset());
		quote.setSfdcStatus(qidsQuote.getQuoteStatus());
		quote.setPriceTermCd(qidsQuote.getPriceTermCode());
        //quoteService.getOMHandler(quote).setPriceTermCd(quote, qidsQuote);

		QuoteCategory quoteCategory = applicationService.getQuoteType()
				.determineQuoteCategory(AssetType.fromString(quote.getOrigAsset()));

		if (quoteCategory != null)
			quote.setQuoteCategory(quoteCategory);

		quote.setPaCac(qidsQuote.getPACAC());
		quote.setPaNr(qidsQuote.getPANumber());
		
		//applicationService.getOrderMgmtApplnType().setPaymentTerm(quote, qidsQuote.getPaymentTerm());
		// applicationService.getOrderMgmtApplnType().setQuoteDefaultsByRule(quote,
		// filter) ;
		quote.setPdfTemplateId(qidsQuote.getPdfTemplateId());
		quote.setPriceGeo(qidsQuote.getPriceGeo());
		/*String hpIdCd= applicationService.getOrderMgmtApplnType().getHpIdCode(quote.getPriceGeo());
		quote.getQtPrchOrdReqt().setHpIdCd(hpIdCd);*/
		
		//quote.setPriceListType(qidsQuote.getPriceListType());
		quote.setSrcPriceListType(qidsQuote.getPriceListType());
		// applicationService.getOrderMgmtApplnType().setPriceListType(quote,
		// qidsQuote.getPriceListType());
		quote.setPrmryQuoteUrl(qidsQuote.getQuoteDetailURL());
		quote.setProfileId(qidsQuote.getProfileID());
		quote.setRegionCd(transformUtilService.tranformValueWithDefault("REGION_CD",
				qidsQuote.getOriginatingQuoteSystem(), "AOE", qidsQuote.getRegionCd()));

		quote.setRequestId(qidsQuote.getRequestId());
		quote.setRoundDigit(qidsQuote.getRoundDigit());
		quote.setServiceAgrmntId(qidsQuote.getServiceAgreementID());
		quote.setShippingAndHandlingAmt(CommonUtil.roundTo2Decimals(qidsQuote.getShippingAndHandlingAmt()));

		//US-18337: Set the total US Tariff Amount on the quote from QIDS
		quote.setTariffAmt(CommonUtil.roundTo2Decimals(qidsQuote.getTotalTariffFee()));
		quote.setSignificantDigits(CommonUtil.convertToBigDecimal(qidsQuote.getSignificantDigits()));
		quote.setSpecialHandling(CommonUtil.roundTo2Decimals(qidsQuote.getSpecialHandling()));
		if (quote.getShippingAndHandlingAmt() != null && quote.getSpecialHandling() != null)
			quote.setShpgChrg(quote.getShippingAndHandlingAmt().subtract(quote.getSpecialHandling()).setScale(2,
					RoundingMode.FLOOR));
		quote.setTaxRate(CommonUtil.roundTo2Decimals(qidsQuote.getTaxRate()));
		quote.setTermsInMonths(CommonUtil.convertToLong(qidsQuote.getTermsInMonths()));
		quote.setTitlePassesAt(qidsQuote.getTitlePassesAt());
		if(StringUtils.equalsIgnoreCase(qidsQuote.getOriginatingQuoteSystem(), Constants.PrepareConstantAttributes.NGQ_PARTNER)) {
			quote.setTotalAmt(CommonUtil.roundTo2Decimals(qidsQuote.getTotalNetPricewithBenefits()));
		}else {
			quote.setTotalAmt(CommonUtil.roundTo2Decimals(qidsQuote.getGrandTotal()));
		}
		// US-18098 NGQC GLSS quote total
		if (qidsQuote.getGlssOrderBomAmt() != null && ("Y".equalsIgnoreCase(qidsQuote.getGlscFlag()))) {
			quote.setGlssOrderBomAmt(CommonUtil.roundTo2Decimals(qidsQuote.getGlssOrderBomAmt()));
		}
		quote.setTotalListPriceAmt(CommonUtil.roundTo2Decimals(qidsQuote.getTotalListPrice()));
		quote.setTotalPricePrevContract(CommonUtil.roundTo2Decimals(qidsQuote.getTotalPricePreviousContract()));
		quote.setTotalRegulatoryFee(CommonUtil.roundTo2Decimals(qidsQuote.getTotalRegulatoryFee()));
		quote.setTotalTaxAmt(CommonUtil.roundTo2Decimals(qidsQuote.getTotalTax()));
		// QUOTE_CID
		if (qidsQuote.getHpQuoteCIDList() != null) {
			for (HpQuoteCIDList cid : qidsQuote.getHpQuoteCIDList().getHpQuoteCIDs()) {
				quote.setCidNumber(cid.getCIDNumber());
			}
		}
		// SLS_QTN_VRSN
		quote.setEffectiveTs(CommonUtil
				.convertToDateTime(myWorkspaceService.getXMLGregorianCalendar(qidsQuote.getQuoteStartDate())));
		/*
		 * US-18609-HWaaS_262.2
		 * It retrieves Quote order booking end date from the QIDS quote header and sets it to quote order booking end date in OMUI.
		 * This field is required for HWaaS quotes to determine the order booking end date for the quote.
		 * For non-HWaaS quotes, this field can be null as it is not used in any logic in OMUI.
		 */
		quote.setExpiryTs(
				CommonUtil.convertToDateTime(myWorkspaceService.getXMLGregorianCalendar(qidsQuote.getQuoteEndDate())));
		boolean isOrderBookingEndDTblnk = qidsQuote.getOrderBookingEndDate() == null || qidsQuote.getOrderBookingEndDate().toString().isBlank() || qidsQuote.getOrderBookingEndDate().toString().isEmpty();
		quote.setOrderBookingEndDate(CommonUtil.convertToDateTime(myWorkspaceService.getXMLGregorianCalendar(isOrderBookingEndDTblnk ? qidsQuote.getQuoteEndDate() : qidsQuote.getOrderBookingEndDate())));
		quote.setCurrencyCd(qidsQuote.getCurrencyIsoCode());
		String priceDescriptor = "" ;
		if (StringUtils.isNotEmpty(quote.getPriceGeo())) {
			priceDescriptor = priceDescriptor.concat(quote.getPriceGeo()) ;
		}
		if (StringUtils.isNotEmpty(quote.getCurrencyCd())) {
			priceDescriptor = priceDescriptor.concat (quote.getCurrencyCd()) ;
		}
		if (quote.getSrcPriceListType() != null && quote.getSrcPriceListType().length() > 2 && StringUtils.isNotEmpty(quote.getPriceTermCd())) {
			priceDescriptor = priceDescriptor.concat (quote.getPriceTermCd()) ;
		} else {
			if (quote.getSrcPriceListType() != null && quote.getSrcPriceListType().length() == 2) {
				priceDescriptor = priceDescriptor.concat (quote.getSrcPriceListType()) ;
			}
		}
		if (StringUtils.isEmpty(quote.getPriceDescriptor()) 
				&& StringUtils.isNotEmpty(priceDescriptor)) {
			quote.setPriceDescriptor(priceDescriptor);
		}
		quote.setSlsChnlCd(qidsQuote.getSalesChannelCode());
		quote.setSlsQtnId(qidsQuote.getSlsQtnID());
		quote.setSlsQtnVrsnSqnNr(qidsQuote.getSlsQtnVrsnSqnNr());
		quote.setTotalTaxAmt(CommonUtil.roundTo2Decimals(qidsQuote.getTotalTax()));
		quote.setLanguageCd(qidsQuote.getLanguageCd());
		// QIDS R8 release
		quote.setCompletionTs(CommonUtil
				.convertToDateTime(myWorkspaceService.getXMLGregorianCalendar(qidsQuote.getQuoteCompletionDate())));
		quote.setContractStartDate(CommonUtil
				.convertToDateTime(myWorkspaceService.getXMLGregorianCalendar(qidsQuote.getContractStartDate())));
		quote.setContractEndDate(CommonUtil
				.convertToDateTime(myWorkspaceService.getXMLGregorianCalendar(qidsQuote.getContractEndDate())));
		//quote.setTrmsAndCndUrl(qidsQuote.getTermsAndConditions());

		// quote.setSaasScpOfWrkUrl(qidsQuote.getSaasScpOfWrkUrl);
		quote.setPrmryQuoteUrl(qidsQuote.getQuoteDetailURL());
		/*
		 * quote.seteDlvryEmail1(qidsQuote.getElectronicDeliveryEmailAddress1())
		 * ;
		 * quote.seteDlvryEmail2(qidsQuote.getElectronicDeliveryEmailAddress2())
		 * ;
		 * quote.seteDlvryEmail3(qidsQuote.getElectronicDeliveryEmailAddress3())
		 * ;
		 * quote.seteDlvryEmail4(qidsQuote.getElectronicDeliveryEmailAddress4())
		 * ;
		 * quote.seteDlvryEmail5(qidsQuote.getElectronicDeliveryEmailAddress5())
		 * ; quote.setApptusApproverId(qidsQuote.getAPPTUSApproverID());
		 * quote.setActiveInformalApproverId(qidsQuote.
		 * getActiveInformalApproverID());
		 * quote.setVsoeCompliance(qidsQuote.getVSOECompliance());
		 * quote.setVsoeApproverId(qidsQuote.getVSOEApproverID());
		 * quote.setRenewalFlag(qidsQuote.getRenewalFlag());
		 */
		quote.setSlsQtnVrsnSttsCd(qidsQuote.getStatusCode());

		/*
		 * Change for Electronic Order reading
		 */
		if (qidsQuote.getVSOEApproverID() != null) {
			quote.setVsoeApproverId(qidsQuote.getVSOEApproverID());
			if ("E-ORDER-AUTO".equalsIgnoreCase(quote.getVsoeApproverId())
					|| "E-ORDER-RR".equalsIgnoreCase(quote.getVsoeApproverId())) {

				// Populating PO Number
				if (quote.getPrchOrdAtachmt() != null) {
					quote.getPrchOrdAtachmt().setPoNr(qidsQuote.getPurchaseOrderNumber());
				} else {
					PrchOrdAtachmt prchOrdAtachmt = new PrchOrdAtachmt();
					prchOrdAtachmt.setPoNr(qidsQuote.getPurchaseOrderNumber());
					quote.setPrchOrdAtachmt(prchOrdAtachmt);
				}

				// Populating PO Status Code
				if (quote.getQtPrchOrdReqt() != null) {
					quote.getQtPrchOrdReqt().setPoStatusCD(qidsQuote.getPurchaseOrderStatusCode());
				} else {
					QtPrchOrdReqt qtPrchOrdReqt = new QtPrchOrdReqt();
					qtPrchOrdReqt.setPoStatusCD(qidsQuote.getPurchaseOrderStatusCode());
					quote.setQtPrchOrdReqt(qtPrchOrdReqt);
				}

			}
		}
		quote.setSubTotalAmt(CommonUtil.roundTo2Decimals(qidsQuote.getTotal()));
		quote.setPaExpiryTs(
				CommonUtil.convertToDateTime(myWorkspaceService.getXMLGregorianCalendar(qidsQuote.getPaExpiryDate())));
		applicationService.getOrderMgmtApplnType(quote).setSfdcStatus(quote, qidsQuote);
		quote.setCompletionSource(qidsQuote.getQuoteCompletionSource());
		quote.setOriginalRefQuoteId(qidsQuote.getReferenceQuoteID());
		// tenant code added

		quote.setTenantCd(qidsQuote.getTenantCd().value());
		quote.setMcCharge(qidsQuote.getMCCode());
		quote.setDiscountBand(qidsQuote.getDiscountBand());
		quote.setOpportunityType(qidsQuote.getOpportunityType());
		quote.setBillingSchdl(qidsQuote.getBillingSchedule());
		quote.setAprvrCd(qidsQuote.getApproverCd());
		quote.setInfluencer(qidsQuote.isInfluencer());
		quote.setRtm(filter.getRtm());
		quote.setPsContractId(qidsQuote.getMasterContractID());
		quote.setPsContractName(qidsQuote.getMasterContractName());
		applicationService.getOrderMgmtApplnType(quote).setRtm(quote, qidsQuote);
        quoteService.getOMHandler(quote).setExchangeRate(quote, qidsQuote);
		String standardTextComments = qidsQuote.getStandardTextComments();

		QtCmt qtCmt = new QtCmt(QtCmtType.STANDARDTEXTCOMMENTS, true);
		qtCmt.setCmtTxt1(standardTextComments);

		quote.getComments().put(QtCmtType.STANDARDTEXTCOMMENTS, qtCmt);

	/*	QtCmt qtCmtAddnlInfo = new QtCmt(QtCmtType.ADDITIONALINFO, true);
		qtCmtAddnlInfo.setCmtTxt1(qidsQuote.getAdditionalInfo());
		quote.getComments().put(QtCmtType.ADDITIONALINFO, qtCmtAddnlInfo);*/

		quote.getCstmAtr().setQtTyp(qidsQuote.getType());
		//quote.getCstmAtr().setCsSolnTyp(TBD);
		// quote.setFanNr(qidsQuote.getFAN());

		// TODO processDerivedDelvDt
		quote.setQuotePublishedFlag(qidsQuote.getQuotePublishedFlag());
		quote.setLoanNumber(qidsQuote.getLoanNumber());	
		
		
		if(qidsQuote.getNimbleTimelessUpgradeFlag() != null) {
			quote.setNimbleTimelessUpgradeFlag(qidsQuote.getNimbleTimelessUpgradeFlag());
		}
		
		if (quote != null && quote.getRegionCd() != null && StringUtils.equalsIgnoreCase("LA", quote.getRegionCd())
				&& StringUtils.equalsIgnoreCase("BR", quote.getCountryCd())) {
			quote.getQtPrchOrdReqt().setBrazilUsage(qidsQuote.getCustomerUsage());
			quote.getQtPrchOrdReqt().setBrazilOperation(qidsQuote.getTaxExemptions());
		}
		if(filter.getRegionCD() != null && filter.getRegionCD().equalsIgnoreCase("LA")) {
			if(qidsQuote.getFinancialFactor() != null) {
				quote.setFinFactor(qidsQuote.getFinancialFactor());
			}
		}
		if(qidsQuote.getCloudPortalId() != null)
			{
			quote.setCloudPortalId(qidsQuote.getCloudPortalId());
			}
			if (qidsQuote.getCloudPortalName() != null) {
				quote.setCloudPortalName(qidsQuote.getCloudPortalName());
			}
			return quote;
	}

	private void setSalesOrgFromQids(Quote quote, QuoteHeader qidsQuote) {
		if (quote.getQtPrchOrdReqt().isSerpFlag() && quote.getCstmAtr().getSlsOrg() == null && StringUtils.isEmpty(quote.getCstmAtr().getSlsOrg())) {
			LocalizationFilter filterbase = applicationService.getLocalizationFilterByCookies(quote);
			filterbase.setOmTypCd("SERP");
			AugmentedLocalizationFilter augmentedLocalizationFilter = new AugmentedLocalizationFilter();
			augmentedLocalizationFilter.setFilterBase(filterbase);
			List<SalesOrgModel> salesOrgList = dropDownListService.getS4SalesOrgsList(augmentedLocalizationFilter);
			if (CollectionUtils.isNotEmpty(salesOrgList)) {
				String salesOrg = qidsQuote.getSalesOrg() ; //CommonUtil.getValueFromJSON(qidsQuote.getAdditionalInfo(), "salesOrg") ;
				if (Iterables.any(salesOrgList, predicateForSalesOrg(salesOrg))) {
					quote.setSalesOrg(salesOrg);
					quote.getCstmAtr().setSlsOrg(salesOrg);
				}
			}
		} /*else {
			quote.setSalesOrg(qidsQuote.getSalesOrg());
		}*/

	}
	

	private Predicate<SalesOrgModel> predicateForSalesOrg(final String slsOrg) {

		return new Predicate<SalesOrgModel>() {
			@Override
			public boolean apply(final SalesOrgModel input) {

				return StringUtils.equalsIgnoreCase(input.getSalesOrgCd(), slsOrg);
			}
		};

	}	


	public QuoteCustomerAddress convertCustomerAddress(CustomerType customerType,
			com.hp.om.service.qids.readquote.xjc.generated.CustomerType qidsCustomerType) {
		QuoteCustomerAddress customerAddress = new QuoteCustomerAddress();

		customerAddress.setCustomerType(CustomerType.fromString(customerType.getCode()));
		customerAddress.setCustomerType(CustomerType.fromString(customerType.getCode()));
		if (qidsCustomerType.getAddress() == null) {
			LOG.debug("ERROR: Do not expect a null " + customerType.getCode()
					+ " customer address object when converting from qids.");
			return customerAddress;
		}

		customerAddress.setUpdated(true);
		customerAddress.setCity(qidsCustomerType.getAddress().getCity());
		customerAddress.setCityarea(qidsCustomerType.getAddress().getCityArea());
		customerAddress.setCountry(qidsCustomerType.getAddress().getCountry());
		if (qidsCustomerType.getContact() != null) {
			customerAddress.setEmail(qidsCustomerType.getContact().getEmail());
			customerAddress.setEmail2(qidsCustomerType.getContact().getEmail2());
			customerAddress.setPhone(qidsCustomerType.getContact().getPhoneNumber());
			customerAddress.setPhoneExt(qidsCustomerType.getContact().getPhoneNumberExt());
			/*
			 * Add for AOE>>Sprint6>>US7284 keep quote customer data for Canada,
			 * UI mapping
			 */
			customerAddress.setFaxNo(qidsCustomerType.getContact().getFax());
			customerAddress.setMobileNo(qidsCustomerType.getContact().getPhoneNumber());
		}

		customerAddress.setPostalcode(qidsCustomerType.getAddress().getPostalCode());
		if(!StringUtils.equalsIgnoreCase("APJ", qidsCustomerType.getAddress().getRegion())){
			customerAddress.setRegion(qidsCustomerType.getAddress().getRegion());			
		}
		// customerAddress.setSlsQtnId(qidsCustomerAddress.getSlsQtnId());
		// customerAddress.setSlsQtnVrsnSqnNr(qidsCustomerAddress.getSlsQtnVrsnSqnNr());
		customerAddress.setState(qidsCustomerType.getAddress().getStateProvinceCode());
		customerAddress.setStreet(qidsCustomerType.getAddress().getAddress1());
		customerAddress.setStreet2(qidsCustomerType.getAddress().getAddress2());
		customerAddress.setStreet3(qidsCustomerType.getAddress().getAddress3());
		/* Added for FVO>>R4 Task 286 */
		customerAddress.setName(qidsCustomerType.getCompany().getName());

		/*
		 * Add for AOE>>Sprint6>>US7284 keep quote customer data for Canada, UI
		 * mapping
		 */
		/*customerAddress.setFaxNo(qidsCustomerType.getContact().getFax());
		customerAddress.setMobileNo(qidsCustomerType.getContact().getPhoneNumber());*/
		
		
		return customerAddress;
	}

	public QuoteCustomer convertCustomer(CustomerType customerType,
			com.hp.om.service.qids.readquote.xjc.generated.CustomerType qidsCustomer) {
		QuoteCustomer customer = new QuoteCustomer();

		customer.setCustomerType(CustomerType.fromString(customerType.getCode()));

		if (qidsCustomer.getCompany() == null) {
			LOG.debug("ERROR: Do not expect a null " + customerType.getCode()
					+ " customer object when converting from qids.");
			return customer;
		}
		customer.setUpdated(true);
		customer.setCompanyName(qidsCustomer.getCompany().getName());
		customer.setCompanyNr(qidsCustomer.getCompany().getCompanyNumber());
		// customer.setCrsId(qidsCustomer.getCrsID());
		if (qidsCustomer.getCompany().getCrsID() == null || "".equals(qidsCustomer.getCompany().getCrsID())) {
			customer.setCrsId(qidsCustomer.getCompany().getCompanyNumber());
		} else {
			customer.setCrsId(qidsCustomer.getCompany().getCrsID());
		}
		customer.setDivision(qidsCustomer.getCompany().getDivision());
		// customer.seteDeliveryEmail(qidsCustomer.getDeliveryEmail());
		customer.setMdcpOrgId(qidsCustomer.getCompany().getMDCPOrgID());
		customer.setPartnerId(qidsCustomer.getCompany().getPartnerId());
		customer.setStaId(qidsCustomer.getCompany().getStaId());
		customer.setOtrPrtySiteInsnId(qidsCustomer.getCompany().getMDCPOpsiID());
	//	customer.setOtrPrtySiteInsnId(qidsCustomer.getCompany().getOtherPartySiteId());
		// customer.setSlsQtnId(qidsCustomer.getSlsQtnId());
		// customer.setSlsQtnVrsnSqnNr(qidsCustomer.getSlsQtnVrsnSqnNr());

		return customer;
	}

	public QuoteCustomerContact convertCustomerContact(CustomerType customerType,
			CustomerContactType qidsCustomerContact) {
		QuoteCustomerContact customerContact = new QuoteCustomerContact();

		if (qidsCustomerContact == null) {
			LOG.debug("ERROR: Do not expect a null " + customerType.getCode()
					+ " customer contact object when converting from qids.");
			return customerContact;
		}

		customerContact.setCustomerType(CustomerType.fromString(customerType.getCode()));
		customerContact.setDepartment(qidsCustomerContact.getDepartment());
		customerContact.setEmail(qidsCustomerContact.getEmail());
		customerContact.setEmail2(qidsCustomerContact.getEmail2());
		customerContact.setFax(qidsCustomerContact.getFax());
		customerContact.setFaxExt(qidsCustomerContact.getFaxExt());
		customerContact.setFirstName(qidsCustomerContact.getFirstName());
		customerContact.setLastName(qidsCustomerContact.getLastName());
		customerContact.setMiddleName(qidsCustomerContact.getMiddleName());
		customerContact.setPhone(qidsCustomerContact.getPhoneNumber());
		customerContact.setPhoneExt(qidsCustomerContact.getPhoneNumberExt());
		customerContact.setPhone2(qidsCustomerContact.getPhoneNumber2());
		customerContact.setPhone2Ext(qidsCustomerContact.getPhoneNumber2Ext());
		// customerContact.setSlsQtnId(qidsCustomerContact.getSlsQtnId());
		// customerContact.setSlsQtnVrsnSqnNr(qidsCustomerContact.getSlsQtnVrsnSqnNr());
		customerContact.setTitle(qidsCustomerContact.getTitle());
		customerContact.setContactId(qidsCustomerContact.getContactID());
		customerContact.setPersonId(qidsCustomerContact.getPersonID());
		customerContact.setCountry(qidsCustomerContact.getCountry());

		return customerContact;
	}

	public QuoteContact convertContact(ContactType contactType,
			com.hp.om.service.qids.readquote.xjc.generated.ContactType qidsContact, QuoteHeader quoteHeader) {
		QuoteContact contact = new QuoteContact();

		contact.setContactType(ContactType.fromString(contactType.getCode()));

		if (qidsContact == null) {
			LOG.debug("ERROR: Do not expect a null " + contactType.getCode()
					+ " contact object when converting from qids.");
			return contact;
		}

		contact.setDepartment(qidsContact.getDepartment());
		contact.setEmail(qidsContact.getEmail());
		contact.setEmail2(qidsContact.getEmail2());
		contact.setFax(qidsContact.getFax());
		contact.setFaxExt(qidsContact.getFaxExt());
		contact.setFirstName(qidsContact.getFirstName());
		contact.setLastName(qidsContact.getLastName());
		contact.setMiddleName(qidsContact.getMiddleName());
		contact.setPhone(qidsContact.getPhoneNumber());
		contact.setPhoneExt(qidsContact.getPhoneNumberExt());
		contact.setPhone2(qidsContact.getPhoneNumber2());
		contact.setPhone2Ext(qidsContact.getPhoneNumberExt());
		contact.setEmail(qidsContact.getEmail());
		contact.setEmail2(qidsContact.getEmail2());
		// contact.setSlsQtnId(qidsContact.getSlsQtnId());
		// contact.setSlsQtnVrsnSqnNr(qidsContact.getSlsQtnVrsnSqnNr());
		contact.setTitle(qidsContact.getTitle());
		contact.setSlsQtnId(quoteHeader.getSlsQtnID());
		contact.setContactId(qidsContact.getEmployeeNumber());
		return contact;
	}

	public QuoteItem convertLineItems(HpQuoteLineItemList qidsItem, String origAsset, Quote quote) {

		QuoteItem item = new QuoteItem();
		QuoteItemModel itemModel = new QuoteItemModel();	
		

		if (qidsItem == null) {
			LOG.debug("ERROR: Do not expect a null quote item object when converting from qids.");
			return item;
		}

		//by default section id is 1
		item.setSectionId("1");
		item.setAssetItemNr(qidsItem.getAssetItemNumber());
		if(StringUtils.isEmpty(qidsItem.getAssetItemNumber()) && StringUtils.isNotEmpty(qidsItem.getLineItemNumber()))
		{
			item.setAssetItemNr(qidsItem.getLineItemNumber());
		}
		item.setCnfgnParentLineItemId(qidsItem.getConfigParentLineItemNumber());
		item.setAirpackedUnitweight(qidsItem.getUnitWeight());
		item.setBundleId(qidsItem.getBundleID());
		item.setBundleItemNr(CommonUtil.convertToLong(qidsItem.getBundleItemNr()));
		item.setCnfgnSolId(qidsItem.getCfgSolutionId());
		item.setCnfgnSysId(qidsItem.getCfgSystemId());
		item.setCnfgnSystemName(qidsItem.getCfgSystemName());
		item.setClin(qidsItem.getCLIN());
		item.setDealDiscountAmount(CommonUtil.roundTo2Decimals(qidsItem.getDealDiscountAmount()));
		item.setDealDiscountPercent(CommonUtil.roundTo2Decimals(qidsItem.getDealDiscountPercent()));
		item.setDealGeo(qidsItem.getDealGeo());
		item.setDealNr(qidsItem.getDealId());
		item.setDealVersionNr(CommonUtil.convertToLong(qidsItem.getDealVersionNr()));
		item.setDiscountAmount(CommonUtil.roundTo2Decimals(qidsItem.getDiscountAmount()));
		item.setDiscountPercent(CommonUtil.roundTo2Decimals(qidsItem.getDiscountPercent()));
		item.setHeartLineItemNr(qidsItem.getPresentationLineNr());
		item.setMasterContractId(qidsItem.getMasterContractId());
		item.setPaBuyerType(qidsItem.getPABuyerType());
		item.setPaCac(qidsItem.getPACAC());
		item.setPaCustomerName(qidsItem.getPABuyerName());
		item.setPaDiscountAmount(CommonUtil.roundTo2Decimals(qidsItem.getPADiscountAmount()));		
		if(qidsItem.getExtendedPADiscountAmount() != null) {
			item.setExtendedPADiscountAmount(CommonUtil.roundTo2Decimals(qidsItem.getExtendedPADiscountAmount()));			
		}		
		if(qidsItem.getUnitQuantity() != null) {		
		item.setUnitQuantity(CommonUtil.convertToLong(qidsItem.getUnitQuantity()));
		}	
		item.setPaDiscountPercent(CommonUtil.roundTo2Decimals(qidsItem.getPADiscountPercent()));
		item.setPaDiscountRate(CommonUtil.roundTo2Decimals(qidsItem.getPaDiscountRate()));
		item.setBundleDisplayCode(qidsItem.getBundleDisplayCode());
		
		
		if (!StringUtils.equalsIgnoreCase(qidsItem.getBundleDisplayCode(), null)
				&& StringUtils.equalsIgnoreCase(qidsItem.getLineType(), "Bundle"))
			setBundleTyp(item, qidsItem.getBundleDisplayCode());

		item.setPaExpiryDt(
				CommonUtil.convertToDateTime(myWorkspaceService.getXMLGregorianCalendar(qidsItem.getPaExpiryDate())));
		if (qidsItem.getPaExpiryDate() == null)
			item.setPaExpiryDt(CommonUtil
					.convertToDateTime(myWorkspaceService.getXMLGregorianCalendar(qidsItem.getPAExpirationDate())));

		item.setPaNr(qidsItem.getPANumber());
		item.setPriceSourceCode(qidsItem.getPriceSourceCode());
		item.setPricingRate(CommonUtil.roundTo2Decimals(qidsItem.getPricingRate()));
		item.setProductDescription(qidsItem.getProductDescription());
		item.setProductId(qidsItem.getProductID());
		item.setProductLine(qidsItem.getProductLine());
		item.setProductNr(qidsItem.getProductNumber());
		item.setCustomerProductNr(qidsItem.getCustomerProductNumber());
		item.setProductOption(qidsItem.getProductOption());
		item.setRegulatoryFee(CommonUtil.roundTo2Decimals(qidsItem.getRegulatoryFee()));
		// item.setReturnToSprtFl(qidsItem.getReturnToSprtFl());
		item.setRoundingAmount(CommonUtil.roundTo2Decimals(qidsItem.getRoundingAmount()));
		item.setSalesforce(qidsItem.getSalesForce());
		// item.setSlsQtnId(qidsItem.getSlsQtnId());
		// item.setSlsQtnItmSqnNr(qidsItem.getSlsQtnItmSqnNr());
		// item.setSlsQtnVrsnSqnNr(qidsItem.getSlsQtnVrsnSqnNr());
		// item.setSourceConfigId(qidsItem.getSourceConfigId());
		item.setTierDiscountAmount(CommonUtil.roundTo2Decimals(qidsItem.getTierDiscountAmount()));
		item.setTierDiscountPercent(CommonUtil.roundTo2Decimals(qidsItem.getTierDiscountPercent()));
		item.setTotalCost(CommonUtil.roundTo2Decimals(qidsItem.getTotalCost()));
		item.setUcLineItemId(CommonUtil.convertToLong(qidsItem.getUCIDLineItemID()));
		item.setUcParentlineitemId(CommonUtil.convertToLong(qidsItem.getUCIDParentLineItemID()));
		item.setUcSubConfigId(qidsItem.getUCIDSubConfigID());
		item.setUcid(qidsItem.getUCID() != null ? qidsItem.getUCID().trim() : null);
		String sourceApplnOCA=  CommonUtil.getValueFromJSON(qidsItem.getAdditionalInfo(), NGQConstants.SOURCE_APPLN) ;
		String ocaSourceSystem = CommonUtil.getValueFromJSON(qidsItem.getAdditionalInfo(), NGQConstants.LINEFANID) ;
		// SLS_QTN_ITEM
		item.setProductClassCode(qidsItem.getProductClassificationCode());
		//US-18112 setting the orderbom value for lineitems from quote
		item.setOrderBom(qidsItem.getOrderBom());
		item.setPriceConditionTypeAmtLineItem(CommonUtil.roundTo2Decimals(qidsItem.getLinePCTAmt()));	// US-18277 - NNLFlag & PCT changes
		item.setQuantity(CommonUtil.convertToLong(qidsItem.getQuantity()));
		item.setOriginalQty(CommonUtil.convertToLong(qidsItem.getQuantity()));
		item.setSourceQty(CommonUtil.convertToLong(qidsItem.getQuantity()));		
		item.setLclUntLstAmt(CommonUtil.roundTo2Decimals(qidsItem.getUnitListAmount()));	
		item.setQuotedLclUntLstAmtOld(CommonUtil.roundTo2Decimals(qidsItem.getUnitListAmount()));
		if(StringUtils.equalsIgnoreCase(origAsset, Constants.PrepareConstantAttributes.NGQ_PARTNER) && !("H3C".equalsIgnoreCase(quote.getQtPrchOrdReqt().getSourceSystemPartnerId())
				&& StringUtils.equalsIgnoreCase("AP", quote.getRegionCd()) && StringUtils.startsWith(quote.getAssetQuoteNr(), "D"))) {
			item.setLclUntNtAmt(CommonUtil.roundTo2Decimals(qidsItem.getNetPriceWithBenefits()));
			if (qidsItem.getProgramBenefits() != null  ) {
				if (item.getDealDiscountAmount() != null) {
					BigDecimal newDiscount = item.getDealDiscountAmount().abs().add(new BigDecimal ( qidsItem.getProgramBenefits() ).abs() ) ;
					if(null != newDiscount) {
						newDiscount = newDiscount.setScale(2, BigDecimal.ROUND_HALF_UP);
						if (BigDecimal.ZERO.compareTo(item.getDealDiscountAmount()) > 0 ) {
							item.setDealDiscountAmount(newDiscount.negate()) ;
						} else {
							item.setDealDiscountAmount(newDiscount) ;
						}
					}					
					
				}
				if (item.getDiscountAmount() != null) {
					BigDecimal newDiscount = item.getDiscountAmount().abs().add(new BigDecimal ( qidsItem.getProgramBenefits() ).abs() ) ;
					if(null != newDiscount) {
						newDiscount = newDiscount.setScale(2, BigDecimal.ROUND_HALF_UP);
						if (BigDecimal.ZERO.compareTo(item.getDiscountAmount()) > 0 ) {
							item.setDiscountAmount(newDiscount.negate()) ;
						} else {
							item.setDiscountAmount(newDiscount) ;
						}
					}	
				}
			}
		}else {
			item.setLclUntNtAmt(CommonUtil.roundTo2Decimals(qidsItem.getUnitNetAmount()));
		}
		item.setLclXtndNtAmt(CommonUtil.roundTo2Decimals(qidsItem.getExtendedNetAmount()));
		item.setShippingHandlingAmt(CommonUtil.convertToBigDecimalVal(qidsItem.getShippingAndLogisticsAmount()));
		//US-18337 Populate Trade Tariff Amount QIDS quote item  to NGQC Quote item.
		item.setTradeTariffAmt(CommonUtil.convertToBigDecimalVal(qidsItem.getTariffFeeAmt()));
		// QIDS R8 release
		// item.setRtsAdminFeePct(CommonUtil.convertToBigDecimal(qidsItem.getRTSAdminFeePercent()));
		item.setSlsQtnItmSqnNr(qidsItem.getLineItemNumber());
		// item.setVariantId(qidsItem.getLineVariantID().longValue());
		// item.setUpgrDiscountAmount(CommonUtil.convertToBigDecimal(qidsItem.getUpgradeDiscountAmount()));
		// item.setUpgrDiscountPercent(CommonUtil.convertToBigDecimal(qidsItem.getUpgradeDiscountPercent()));
		item.setEnpwrDiscountAmount(CommonUtil.roundTo2Decimals(qidsItem.getEmpowermentDiscountAmount()));
		item.setEnpwrDiscountPercent(CommonUtil.roundTo2Decimals(qidsItem.getEmpowermentDiscountPercent()));
		/*
		 * item.setContractStartDate(CommonUtil.convertToDateTime(qidsItem.
		 * getContractStartDate()!=null?qidsItem.getContractStartDate().
		 * toGregorianCalendar():null));
		 * item.setContractEndDate(CommonUtil.convertToDateTime(qidsItem.
		 * getContractEndDate()!=null?qidsItem.getContractEndDate().
		 * toGregorianCalendar():null));
		 * item.setBillingStartDate(CommonUtil.convertToDateTime(qidsItem.
		 * getBillingStartDate()!=null?qidsItem.getBillingStartDate().
		 * toGregorianCalendar():null));
		 * item.setBillingEndDate(CommonUtil.convertToDateTime(qidsItem.
		 * getBillingEndDate()!=null?qidsItem.getBillingEndDate().
		 * toGregorianCalendar():null));
		 * item.setBsnArCd(qidsItem.getBusinessAreaCode());
		 */
		item.setDeleteInd(false);
		item.setLineTypeCd(qidsItem.getLineType());
		item.setPaDiscountGeo(qidsItem.getDiscountGeo());
		item.setPricingRateType(qidsItem.getPricingRateType());
		item.setOmsLineNr(qidsItem.getPresentationLineNr());		
		item.setSupplyingDiv(qidsItem.getSupplyingDiv());
		item.setDistributorTotal(CommonUtil.roundTo2Decimals(qidsItem.getExtendedNetPriceWithBenefits()));
		item.setIconId(qidsItem.getIconID());
		String preferredSuppliers = applicationService.getOrderMgmtApplnType(quote).setPreferredSuppliers(qidsItem);
		item.setPrefSupplyingDiv(preferredSuppliers);	
		// item.setUserCustomGroupLabel(qidsItem.getUserCustomGroupLabel());
		// item.setUserCustomGroupID(qidsItem.getUserCustomGroupID());
		
		item.setSupportFor(qidsItem.getSupportFor());
		item.setSupportedBy(qidsItem.getSupportedBy());
		item.setInstalledBy(qidsItem.getInstalledBy());
		
		item.setCrossBUSolution(qidsItem.getCrossBUId());
		item.setCrossBUSolutionType(qidsItem.getCrossBUSolutionType());
		
		if (qidsItem.getHpQuoteLineItemMCCList() != null) {
			for (HpQuoteLineItemMCCList itemMcc : qidsItem.getHpQuoteLineItemMCCList().getHpQuoteLineItemMCC()) {
				item.add(convertItemMCC(itemMcc, item, quote));
			}
		}

		/*
		 * if (qidsItem.getQuoteLineItemCnfgCharList() != null) { for
		 * (QuoteLineItemCnfgCharList itemCnfgCharList :
		 * qidsItem.getQuoteLineItemCnfgCharList().getQuoteLineItemCnfgChars())
		 * { item.add(convertItemCnfgChar(itemCnfgCharList,item)); } }
		 * 
		 * if (qidsItem.getQuoteLineItemEntitlementList() != null) { for
		 * (QuoteLineItemEntitlementList itemEntitlementList :
		 * qidsItem.getQuoteLineItemEntitlementList().
		 * getQuoteLineItemEntitlements()) {
		 * item.add(convertItemEntitlement(itemEntitlementList,item)); } }
		 */

		// OEM Change
		if (StringUtils.equalsIgnoreCase(origAsset, Constants.PrepareConstantAttributes.NGQ) || StringUtils.equalsIgnoreCase(origAsset, Constants.PrepareConstantAttributes.NGQ_PARTNER) || AssetType.EPRIME.getCode().equalsIgnoreCase(quote.getOrigAsset())) {
			createAdditionalInfoObj(item, qidsItem.getAdditionalInfo());
		}
		
		
		if (qidsItem.getQuoteFlagsList() != null) {
			List<QuoteFlagType> quoteFlagsList = qidsItem.getQuoteFlagsList().getQuoteFlags();
			List<QuoteLineItemFlags> lineItemFlagsList = new ArrayList<>();
			for (QuoteFlagType quoteFlagType : quoteFlagsList) {
				if (quoteFlagType != null && StringUtils.equalsIgnoreCase(quoteFlagType.getFlagType(), "CiFlag")) {
					item.setCidFlagVal(quoteFlagType.getFlagValue());
					if ("E".equalsIgnoreCase(quoteFlagType.getFlagValue())) {
						quote.setCiFlag(true);
					}
				}
				if (quoteFlagType != null && StringUtils.equalsIgnoreCase(quoteFlagType.getFlagType(), "updateItemflg")
						&& StringUtils.equalsIgnoreCase(quoteFlagType.getFlagValue(), "true")) {
					item.setItemUpgradable(true);
				}
				//US-18504 Scalable Pricing scpItemFlag
				// If the quoteFlagType is not null, and its flag type is "scalablePricing" with a value of "true" (case-insensitive),
				// then set the scalable pricing flag on the item to indicate that scalable pricing is enabled for this item.
				if(quoteFlagType !=null && StringUtils.equalsIgnoreCase(quoteFlagType.getFlagType(),"scalablePricing") && StringUtils.equalsIgnoreCase(quoteFlagType.getFlagValue(), "true")) {
					item.setScpItemFlag(quoteFlagType.getFlagValue().equalsIgnoreCase("true") ? "Y" : "N");
				}
				
				if (quoteFlagType != null && StringUtils.equalsIgnoreCase(quoteFlagType.getFlagType(), "C2BXmlFlag")
						&& (StringUtils.equalsIgnoreCase(quoteFlagType.getFlagValue(), "true") || StringUtils.equalsIgnoreCase(quoteFlagType.getFlagValue(), "Y"))
						&&MapUtils.getObject(quote.getFlags(), QtFlagType.C2BFLAG) !=null &&
						StringUtils.equalsIgnoreCase(quote.getFlags().get(QtFlagType.C2BFLAG).getFlgVl(), "false")) {
					QtFlag c2bXmlflg=new QtFlag();
					c2bXmlflg.setFlgVl("true");
					c2bXmlflg.setQtFlagType(QtFlagType.C2BFLAG);
					c2bXmlflg.setSlsQtnId(quote.getSlsQtnId());
					c2bXmlflg.setSlsQtnRvsnSqnNr(quote.getSlsQtnRvsnSqnNr());
					c2bXmlflg.setSlsQtnVrsnSqnNr(quote.getSlsQtnVrsnSqnNr());
					c2bXmlflg.setNewBean(true);
					c2bXmlflg.setUpdated(true);
					quote.put(QtFlagType.C2BFLAG, c2bXmlflg);
				}
				//US-17223 IN:839523 - Capturing Hybris Config Flag for HYBRIS Quote Based Orders
				if (quoteFlagType != null && StringUtils.equalsIgnoreCase(quoteFlagType.getFlagType(), "isHybrisConfig")
						&& StringUtils.equalsIgnoreCase(quoteFlagType.getFlagValue(), "true")) {
					item.setPrcssAcnBit(CommonUtil.doOR(item.getPrcssAcnBit(), AdditionalCharBitConstants.DCE_CONFIG));
				}
				/*
				 * US-18661 Cost Relief Flag - extract costReliefEligible from QIDS line item flags.
				 * Sets the flag value on QuoteItem; used later by prepareCostReliefItemFlag()
				 * to send the CRFL downstream instruction for eligible line items.
				 */
				if (quoteFlagType != null && StringUtils.equalsIgnoreCase(quoteFlagType.getFlagType(), "costReliefEligible")) {
					item.setCostReliefEligible(quoteFlagType.getFlagValue());
				}
				
				QuoteLineItemFlags lineItemFlags = new QuoteLineItemFlags();
				lineItemFlags.setFlgVl(quoteFlagType.getFlagValue());
				lineItemFlags.setSlsQtnId(item.getSlsQtnId());
				lineItemFlags.setSlsQtnVrsnSqnNr(item.getSlsQtnVrsnSqnNr());
				lineItemFlags.setFlgTyp(quoteFlagType.getFlagType());
				lineItemFlagsList.add(lineItemFlags);
			}
			item.setItemFlags(lineItemFlagsList);
		}
		
		
		item.setAdditionalInfo(qidsItem.getAdditionalInfo());
		String lineItemFan = CommonUtil.getValueFromJSON(qidsItem.getAdditionalInfo(), NGQConstants.LINEFANID) ;
		Map<String, String> omuiSrvcCtrlMap = dropDownListService.getOmuiservicekeyValues();
		String possibleNullFanId = "~N/A~n/a~NA~na~" ;
		if ( omuiSrvcCtrlMap.get("NULL_FAN_ID") != null ) {
			possibleNullFanId = omuiSrvcCtrlMap.get("NULL_FAN_ID") ;
		}
		if (lineItemFan != null && possibleNullFanId != null && possibleNullFanId.contains("~" + lineItemFan + "~")) {
			lineItemFan = "" ;
		}
		item.setFanNr(lineItemFan);
		
		itemModel.setCnfgnSystemName(qidsItem.getCfgSystemName());
		itemModel.setLineTypeCd(qidsItem.getLineType());
		if (quote.getQuoteItemModelMap() == null) {
			Map<String, QuoteItemModel> quoteItemMap = new HashMap<String, QuoteItemModel>();
			quote.setQuoteItemModelMap(quoteItemMap);
		}
		quote.getQuoteItemModelMap().put(item.getAssetItemNr(), itemModel);
		if(StringUtils.equalsIgnoreCase("UDST",qidsItem.getLineType()) || StringUtils.equalsIgnoreCase("Comment",qidsItem.getLineType())){
			quote.setUdstOrCommentExist(true);
		}
		
		if (StringUtils.isNotEmpty(quote.getQtPrchOrdReqt().getCidCreator())) {
			quote.getQtPrchOrdReqt().setCidCreator(qidsItem.getCidCreator());
		}
		// Demo Changes fields added
		item.setZi10Reference(qidsItem.getFillupOrderNumber());
		item.setOrderLineNumber(qidsItem.getFillupOrderLineNumber());
		
		item.setProgramBenefits(CommonUtil.roundTo2Decimals(qidsItem.getProgramBenefits()));
		item.setProgramBenefitPct(CommonUtil.roundTo2Decimals(qidsItem.getPrgmBenefitPct()));
		
		if (quote.getRegionCd() != null && StringUtils.equalsIgnoreCase("LA", quote.getRegionCd())
				&& StringUtils.equalsIgnoreCase("BR", quote.getCountryCd())) {
			item.setTaxProductId(qidsItem.getTaxProductId());
			item.setHigherLevelItemIndicator(qidsItem.getHigherLevelItemIndicator());
			item.setFiscalHigherLevelItemIndicator(qidsItem.getFiscalHigherLevelItemIndicator());
			item.setDeliveryPlant(qidsItem.getDeliveryPlant());
			item.setMaterialOrigin(qidsItem.getMaterialOrigin());
			item.setTaxAmount(CommonUtil.roundTo2Decimals(qidsItem.getTaxAmount()));
		}
		//Added Dragon requiremnet.
		item.setSolutionObjectLineItem(qidsItem.getSolutionObjectLineItem());
		
		// US-17976 NGQC HWAAS_038 MULTISHIPTO FOR GLSS QUOTES
		item.setShipToId(  qidsItem.getShipToId() );
		item.setAssetLocationId(qidsItem.getAssetLocationId() );

		//US-18521: IQ for aas ECSRD chnages - Timezone-safe conversion to strip time and preserve source date
		GregorianCalendar ecsrDateCal = myWorkspaceService.getXMLGregorianCalendar(qidsItem.getEcsrDate());
		if (ecsrDateCal != null) {
			try {
				item.setEcsrDate(CommonUtil.stripTimeZonePreserveDate(ecsrDateCal));
			} catch (Exception e) {
				LOG.error("PrepareOMUIQuote >> populateItem >> Error parsing EcsrDate: " + e.getMessage(), e);
			}
		}
		//US-18504 setting the SubConfigId and SVCPenRate value for lineitems from quote

		item.setSubConfigId(qidsItem.getSubConfigId());
		item.setSvcPenRate(BigDecimal.valueOf(qidsItem.getSclbPenRate()));
		return item;
	}

	private void setBundleTyp(QuoteItem item, String bundleDisplayCode) {
		BundleType bundleTyp = dropDownListService.getBundleDisplayCode(bundleDisplayCode);
		item.setDerivedBundleType(bundleTyp.getBundleTypCd());
	}

	private void createAdditionalInfoObj(QuoteItem item, String additionalInfo) {

		if (StringUtils.isNotEmpty(additionalInfo)) {
			JSONObject object = null;
			Map<String, Object> additionMap = new HashMap<String, Object>();
			try {
				object = new JSONObject(additionalInfo);
				additionMap = CommonUtil.toMap(object);
			} catch (org.json.JSONException e) {
				LOG.throwException(new SystemApplicationException(5525, "faild parsing from String to JSON ", e));
				e.printStackTrace();
			}

			// try{
			if (additionMap.get("cidNumber") != null) {
				item.setCidNumber(additionMap.get("cidNumber").toString());
				LOG.debug(" cidNumber Value : " + item.getCidNumber());
			}
			
			if(additionMap.get("loanNumber")!= null && !additionMap.get("loanNumber").toString().equals("null")) {
				item.setLoanNumber(additionMap.get("loanNumber").toString());
			}

			/*
			 * } catch (org.json.JSONException e) { LOG.throwException(new
			 * SystemApplicationException(5525,
			 * "faild parsing from String to JSON ", e)) ; e.printStackTrace();
			 * }
			 */
			/*
			 * String[] keys = JSONObject.getNames(object); for (String key :
			 * keys) { try { Object value = object.get(key);
			 * 
			 * System.out.println("key : "+ key +" value :"+value);
			 * 
			 * } catch (org.json.JSONException e) { LOG.throwException(new
			 * SystemApplicationException(5525,
			 * "faild parsing from String to JSON ", e)) ; } }
			 */
		}

	}

	public QuoteItemMcc convertItemMCC(HpQuoteLineItemMCCList qidsItemMcc, QuoteItem item, Quote quote) {
		QuoteItemMcc itemMcc = new QuoteItemMcc();

		if (qidsItemMcc == null) {
			LOG.debug("ERROR: Do not expect a null quote item mcc object when converting from qids.");
			return itemMcc;
		}
		
		if(qidsItemMcc.getPriceCondType().equals("60B") && quote.getVsoeApproverId() == null) {
			return itemMcc;
		}

		/* US-15516 - NGQC HPFS Asset Management
		 * Setting mcc 04B only if HPEFS flag is true to send it to downstream*/
		if(("04B").equalsIgnoreCase(qidsItemMcc.getPriceCondType()) && !quoteService.checkFlagValueIsPresent(quote, QtFlagType.HPEFSFLAG)) {
			return itemMcc;	
		}	
			
		itemMcc.setEclipseFl(CommonUtil.sqlStrToBoolean(qidsItemMcc.getIsEclipse()));
		itemMcc.setGpsyBundledMccFl(CommonUtil.sqlStrToBoolean(qidsItemMcc.getIsGpsyBundledMCC()));
		itemMcc.setOverrideFl(CommonUtil.sqlStrToBoolean(qidsItemMcc.getIsOverride()));
		itemMcc.setPriceCondType(qidsItemMcc.getPriceCondType());
		itemMcc.setProductClass(qidsItemMcc.getProdClass());
		itemMcc.setProductLine(qidsItemMcc.getProdLine());
		itemMcc.setPublicSectorFl(CommonUtil.sqlStrToBoolean(qidsItemMcc.getIsPublicSector()));
		itemMcc.setRateFl(CommonUtil.sqlStrToBoolean(qidsItemMcc.getIsRate()));
		itemMcc.setRateoramount(CommonUtil.convertToBigDecimalVal(qidsItemMcc.getRateOrAmount()));
		itemMcc.setQuotedRateoramount(CommonUtil.convertToBigDecimalVal(qidsItemMcc.getRateOrAmount()));
		itemMcc.setRegulatoryFl(CommonUtil.sqlStrToBoolean(qidsItemMcc.getIsRegulatory()));
		// itemMcc.setSlsQtnId(qidsItemMcc.getSlsQtnId());
		itemMcc.setSlsQtnItmSqnNr(item.getSlsQtnItmSqnNr());
		
		// US 8932 code commented
		/*QtFlag fallbackLogicEnabled = (QtFlag) MapUtils.getObject(quote.getFlags(), QtFlagType.FALLBACKLOGICENABLED);
		if (fallbackLogicEnabled != null && "true".equalsIgnoreCase(fallbackLogicEnabled.getFlgVl())) {			
		getFusionPriceConditionType(eachQuoteItemMcc,  eachItem,  quote)
		}*/
		
		
	
		

		// itemMcc.setPriceCondType(qidsItemMcc.getPriceCondType());
		// itemMcc.setRateFl(StringUtils.equalsIgnoreCase(qidsItemMcc.getIsRate(),
		// "0"));
		// qidsItemMcc.getIsRate();
		// itemMcc.setSlsQtnVrsnSqnNr(qidsItemMcc.getSlsQtnVrsnSqnNr());

		return itemMcc;
	}
	
	

	/*private void getFusionPriceConditionType(QuoteItemMcc eachQuoteItemMcc, QuoteItem eachItem, Quote quote) {
		
		LOG.debug("getConditionTypeForMccCode >> getAllMccProductTypes");
		Map<String, String> mccProductTypes = mccConditionTypeDao.getAllMccProductTypes();

		LOG.debug("getConditionTypeForMccCode >> getAllMccOptionTypes");
		Map<String, String> mccOptionTypes = mccConditionTypeDao.getAllMccOptionTypes();
		
		

		if (productLineTypeCheck(eachItem)) {

			String mccCodeFrmQuoteP = eachQuoteItemMcc.getPriceCondType();

			eachQuoteItemMcc.setPriceCondType(mccCodeFrmQuoteP);
			
			// String mccCodeForFusn =
			// mccConditionTypeDao.getPtoductTypeForMccCode(mccCodeFrmQuoteP);
			String mccCodeForFusn = mccProductTypes.get(mccCodeFrmQuoteP);

			eachQuoteItemMcc.setFusnPriceCondType(mccCodeForFusn);

		}
		if (optionLineTypeCheck(eachItem)) {

			String mccCodeFrmQuoteO = eachQuoteItemMcc.getPriceCondType();
			
			 * this is for EMEA order .. Value for option also has
			 * to sent as Y**
			 
			if (StringUtils.equalsIgnoreCase("EU", quote.getRegionCd())) {

				String mccCodeForFusn = mccProductTypes.get(mccCodeFrmQuoteO);
				eachQuoteItemMcc.setFusnPriceCondType(mccCodeForFusn);

			} else {

				// String mccCodeForFusn =
				// mccConditionTypeDao.getOptionTypeForMccCode(mccCodeFrmQuoteO);
				String mccCodeForFusn = mccOptionTypes.get(mccCodeFrmQuoteO);

				eachQuoteItemMcc.setFusnPriceCondType(mccCodeForFusn);
			}
		}	
		
	}*/


	/*
	 * public QuoteItemCnfgChrc convertItemCnfgChar(QuoteLineItemCnfgCharList
	 * qidsItemCnfgChar,QuoteItem item) { QuoteItemCnfgChrc itemCnfgChrc = new
	 * QuoteItemCnfgChrc();
	 * 
	 * if (qidsItemCnfgChar == null) { LOG.debug(
	 * "ERROR: Do not expect a null quote item characteristic object when converting from qids."
	 * ); return itemCnfgChrc; }
	 * 
	 * itemCnfgChrc.setChrcCategoryCd(qidsItemCnfgChar.
	 * getCharacteristicCategoryCD());
	 * itemCnfgChrc.setChrcId(CommonUtil.convertToLong(qidsItemCnfgChar.
	 * getCharacteristicID()));
	 * itemCnfgChrc.setChrcQty(CommonUtil.convertToLong(qidsItemCnfgChar.
	 * getCharacteristicQuantity()));
	 * itemCnfgChrc.setChrcText(qidsItemCnfgChar.getCharacteristicTypeName());
	 * itemCnfgChrc.setChrcTypId(CommonUtil.convertToLong(qidsItemCnfgChar.
	 * getCharacteristicTypeID())); //R1B
	 * itemCnfgChrc.setChrcNm(qidsItemCnfgChar.getCharacteristicName());
	 * itemCnfgChrc.setSlsQtnItmSqnNr(item.getSlsQtnItmSqnNr()); return
	 * itemCnfgChrc; }
	 */

	/*
	 * public QuoteItemEntitlement
	 * convertItemEntitlement(QuoteLineItemEntitlementList
	 * qidsItemEntitlement,QuoteItem item) { QuoteItemEntitlement
	 * itemEntitlement = new QuoteItemEntitlement();
	 * 
	 * if (qidsItemEntitlement == null) { LOG.debug(
	 * "ERROR: Do not expect a null quote item entitlement object when converting from qids."
	 * ); return itemEntitlement; }
	 * 
	 * itemEntitlement.setEntitlementId(CommonUtil.convertToLong(
	 * qidsItemEntitlement.getEntitlementID()));
	 * itemEntitlement.setSlsQtnItmSqnNr(item.getSlsQtnItmSqnNr()); return
	 * itemEntitlement; }
	 */

	public void processQtCstmAtr(Quote quote, QidsQuoteheader quoteHeader) {

		// String country =
		// applicationService.getOrderMgmtApplnType().getQuoteCountryForCstmAtr(quoteHeader)
		// ;
		// quoteHeader.getSoldTo() != null ?
		// (quoteHeader.getSoldTo().getAddress() != null ?
		// quoteHeader.getSoldTo().getAddress().getCountry(): null ) : null;
		// processQtCstmAtr (quote, country, quoteHeader.getQuoteType(),
		// quoteHeader.getRegionCd(),quoteHeader) ;

		String country = applicationService.getOrderMgmtApplnType(quote).getQuoteCountryForCstmAtr(quoteHeader);

		// processQtCstmAtr (quote, country, quoteHeader.getQuoteType(),
		// quoteHeader.getRegionCd(),quoteHeader) ;
	}

	public void processQtCstmAtr(Quote quote, String countryCd, String qtTyp, String regionCd,
			QidsQuoteheader quoteHeader) {

		QtSorgDcDivFilter qtSorgDcDivFilter = new QtSorgDcDivFilter();
		qtSorgDcDivFilter.setCountryCd(countryCd);
		qtSorgDcDivFilter.setQuoteCategory(quote.getQuoteCategory());
		qtSorgDcDivFilter.setQuoteType(applicationService.getQuoteType());
		qtSorgDcDivFilter.setRegionCd(regionCd);
		QtSorgDcDiv qtSorgDcDiv = myWorkspaceService.getQtSorgDcDiv(qtSorgDcDivFilter);
		if (qtSorgDcDiv != null) {
			QtCstmAtr cstmAtr = quote.getCstmAtr();
			cstmAtr.setDstrbtnChnl(qtSorgDcDiv.getDstrbtnChnlCd());
			cstmAtr.setQtTyp(!StringUtils.isEmpty(qtSorgDcDiv.getQtTypCd()) ? qtSorgDcDiv.getQtTypCd() : qtTyp);
			cstmAtr.setSlsOrg(qtSorgDcDiv.getSlsOrgCd());
			cstmAtr.setDvsn(qtSorgDcDiv.getDvsnCd());
			cstmAtr.setSlsOfc(qtSorgDcDiv.getSlsOfc());
			cstmAtr.setCustomerConditionGroup(qtSorgDcDiv.getCustomerConditionGroup());
			// quote.setCstmAtr(cstmAtr);
		}
		if (quoteHeader != null)
			applicationService.getOrderMgmtApplnType(quote).setSlsOrg(quoteHeader, quote);
	}

	@Override
	public DealQuoteInfo convertToDealQuoteInfo(HPQuote qidsQuote) {

		DealQuoteInfo quote = null;

		if (qidsQuote.getQuoteHeader() != null) {
			quote = convertDealQuoteInfo(qidsQuote.getQuoteHeader());

			if (qidsQuote.getQuoteHeader().getEndCustomer() != null) {
				// quote.getCustomerAddresses().put(CustomerType.ENDCUSTOMER,convertCustomerAddress(CustomerType.ENDCUSTOMER,qidsQuote.getQuoteHeader().getEndCustomer()));
				quote.getCustomerAddresses().put(CustomerType.QIDSENDCUSTOMER, convertCustomerAddress(
						CustomerType.QIDSENDCUSTOMER, qidsQuote.getQuoteHeader().getEndCustomer()));
			}

		}

		return quote;
	}

	@Override
	public DealQuoteInfo convertToSWDealQuoteInfo(com.hp.om.service.qids.sw.readquote.xjc.generated.HPQuote hpQuote) {

		// DealQuoteInfo quote = null;
		DealQuoteInfo quote = new DealQuoteInfo(true);

		if (hpQuote.getQuoteHeader() != null) {

			com.hp.om.service.qids.sw.readquote.xjc.generated.QuoteHeader header = hpQuote.getQuoteHeader();

			if (header.getOriginatingSystemQuoteID() == null) {
				LOG.throwException(new BusinessApplicationException(3024, "Read of deal quote from QIDS failed. "));
			}

			quote.setCreatedBy(header.getCreatedBy());
			quote.setCreatedTs(CommonUtil.convertToDateTime(
					myWorkspaceService.getXMLGregorianCalendar(header.getOriginatingAssetCreationDate())));
			quote.setDealNr(header.getEclipseDealId());
			quote.setDealOpportunityId(header.getOpportunityId());
			quote.setDealExpiryTs(
					CommonUtil.convertToDateTime(myWorkspaceService.getXMLGregorianCalendar(header.getQuoteEndDate())));
			quote.setDealCurrencyCd(header.getCurrencyIsoCode());
			quote.setSlsQtnId(header.getSlsQtnID());
			quote.setSlsQtnVrsnSqnNr(header.getSlsQtnVrsnSqnNr());
			quote.setUpdated(true);

			if (hpQuote.getQuoteHeader().getEndCustomer() != null) {
				// quote.getCustomerAddresses().put(CustomerType.ENDCUSTOMER,convertCustomerAddress(CustomerType.ENDCUSTOMER,qidsQuote.getQuoteHeader().getEndCustomer()));
				// quote.getCustomerAddresses().put(CustomerType.QIDSENDCUSTOMER,convertCustomerAddress(CustomerType.QIDSENDCUSTOMER,hpQuote.getQuoteHeader().getEndCustomer()));
			}

		}

		return quote;
	}

	private DealQuoteInfo convertDealQuoteInfo(QuoteHeader qidsQuote) {
		DealQuoteInfo quote = new DealQuoteInfo(true);

		if (qidsQuote == null) {
			LOG.debug("ERROR: Do not expect a null quote object when converting from qids.");
			return quote;
		}
		if (qidsQuote.getOriginatingSystemQuoteID() == null) {
			LOG.throwException(new BusinessApplicationException(3024, "Read of deal quote from QIDS failed. "));
		}

		quote.setCreatedBy(qidsQuote.getCreatedBy());
		quote.setCreatedTs(CommonUtil.convertToDateTime(
				myWorkspaceService.getXMLGregorianCalendar(qidsQuote.getOriginatingAssetCreationDate())));
		quote.setDealNr(qidsQuote.getEclipseDealId());
		quote.setDealOpportunityId(qidsQuote.getOpportunityId());
		quote.setDealExpiryTs(
				CommonUtil.convertToDateTime(myWorkspaceService.getXMLGregorianCalendar(qidsQuote.getQuoteEndDate())));
		quote.setDealStartDate(
				CommonUtil.convertToDateTime(myWorkspaceService.getXMLGregorianCalendar(qidsQuote.getQuoteStartDate())));
		quote.setDealCurrencyCd(qidsQuote.getCurrencyIsoCode());
		quote.setSlsQtnId(qidsQuote.getSlsQtnID());
		quote.setSlsQtnVrsnSqnNr(qidsQuote.getSlsQtnVrsnSqnNr());
		quote.setUpdated(true);
		return quote;
	}

	public void setDynamicConfigValue(Quote quote) {

		Map<String, List<String>> manusfractionInstructionMap = new HashMap<String, List<String>>();
		Map<String, String> configIdMap = new HashMap<String, String>();
		List<String> configSysIdList = null;
		for (QuoteItem quoteItem : quote.getItems()) {

			if (manusfractionInstructionMap.get(quoteItem.getCnfgnSystemName()) != null) {
				if (StringUtils.isNotEmpty(quoteItem.getHeartLineItemNr())) {
					configSysIdList.add(quoteItem.getHeartLineItemNr());
					manusfractionInstructionMap.put(quoteItem.getCnfgnSystemName(), configSysIdList);
				}
				if (quoteItem.getUcid() != null && !quoteItem.getUcid().isEmpty() && (configIdMap != null && !StringUtils
						.equals(quoteItem.getUcid(), configIdMap.get(quoteItem.getCnfgnSystemName())))) {
					configIdMap.put(quoteItem.getCnfgnSystemName(), quoteItem.getUcid());
				}
			} else {
				configSysIdList = new ArrayList<String>();
				if (StringUtils.isNotEmpty(quoteItem.getHeartLineItemNr())
						&& !quoteItem.getHeartLineItemNr().isEmpty()) {
					configSysIdList.add(quoteItem.getHeartLineItemNr());
					manusfractionInstructionMap.put(quoteItem.getCnfgnSystemName(), configSysIdList);
				}
				if (quoteItem.getUcid() != null && !quoteItem.getUcid().isEmpty()) {
					manusfractionInstructionMap.put(quoteItem.getCnfgnSystemName(), configSysIdList);
					configIdMap.put(quoteItem.getCnfgnSystemName(), quoteItem.getUcid());
				}
			}
		}

		StringBuilder sb = new StringBuilder();
		
		if(manusfractionInstructionMap.size()>0 && configIdMap.size()>0)
		{
			for (Map.Entry<String, List<String>> entry : manusfractionInstructionMap.entrySet()) {
				// if(StringUtils.isNotEmpty(configIdMap.get(entry.getKey())) &&
				// configIdMap.get(entry.getKey()) != "null"){
				Set<String> mfgInstructionsSet = new TreeSet<String>(entry.getValue());
				List<String> mfgList = new ArrayList<String>(mfgInstructionsSet);
				if (mfgList.size() > 1) {
					sb.append("line ");
					sb.append(mfgList.get(0));
					sb.append(" - ").append(mfgList.get(mfgList.size() - 1));
					sb.append(" UCID:  ");
					sb.append(configIdMap.get(entry.getKey())).append(" ");
				}
				
			}
		}

		if (sb.toString().length() != 0) {
			QtCmt qtcmt = new QtCmt(QtCmtType.DYNAMICCONFIGID, true);
			qtcmt.setCmtTxt1(sb.toString());
			qtcmt.setQtCmtType(QtCmtType.DYNAMICCONFIGID);
			quote.getComments().put(QtCmtType.DYNAMICCONFIGID, qtcmt);
		}
	}
	

	private void setInternalDemoFlag(Quote quote) {		
		quote.setBitWiseQtChar(String.valueOf(NumberUtils.toLong(quote.getBitWiseQtChar()) | QuoteCharBitConstants.INTERNAL_DEMOBUYOUT));

		/*QtFlag qtFlag = new QtFlag();
		qtFlag.setFlgVl("true");			
		qtFlag.setSlsQtnId(quote.getSlsQtnId());
		qtFlag.setSlsQtnVrsnSqnNr(quote.getSlsQtnVrsnSqnNr());			
		qtFlag.setQtFlagType(QtFlagType.INTERNALDEMPBUYOUT);				
		quote.getFlags().put(QtFlagType.INTERNALDEMPBUYOUT, qtFlag);*/

	}
	
	private void setContractOrderFlag(Quote quote){
		quote.setBitWiseQtChar(String.valueOf(NumberUtils.toLong(quote.getBitWiseQtChar()) | QuoteCharBitConstants.CONTRACT_ORDER));
	}
	
	private boolean ischeckProdcutNumber(QuoteItem quoteItem, String productNr) {
		boolean returnValue = false;

		String prodNr = quoteItem.getProductNr();

		if (productNr.startsWith("FANDL:#")) {
			// "CONSTANT:#CONST#H4396B, U*E~H*E~U*PE~H*PE"
			//"FANDL:#FIRST#HA~HL~H9~H0~H1~H2~H6~H7~H8~HG~HH~HT~HU, U*E~U*PE"

			String product = StringUtils.replace(productNr, "FANDL:#", "");

			List<String> constantWithFL = CommonUtil.smartSplit(product, ",");

			for (String constWFL : constantWithFL) {

				if (constWFL != null && constWFL.startsWith("FIRST#")) {
					
					String list = StringUtils.replace(constWFL, "FIRST#", "");
					
					List<String> firstCharsOnly = CommonUtil.smartSplit(list, "~");

					for (String listFirstChar : firstCharsOnly) {
						if (listFirstChar != null && StringUtils.startsWith(prodNr, listFirstChar)) {
							returnValue = true;
							break;

						}
					}

				} else {

					List<String> firstAndLastchar1 = CommonUtil.smartSplit(constWFL, "~");

					for (String listFL : firstAndLastchar1) {

						String firstChar = StringUtils.substringBefore(listFL, "*");
						String lastChar = StringUtils.substringAfter(listFL, "*");

						if (firstChar != null && lastChar != null && prodNr != null) {
							if (StringUtils.startsWith(prodNr, firstChar) && StringUtils.endsWith(prodNr, lastChar)) {
								returnValue = true;
								break;
							}
						}
					}
				}

			}

		} else if (productNr.startsWith("FL:#")) {
			String product = StringUtils.replace(productNr, "FL:#", "");
			// "FL:#U*E~H*E~U*PE~H*PE";

			List<String> firstAndLastchar = CommonUtil.smartSplit(product, "~");

			for (String list : firstAndLastchar) {

				String firstChar = StringUtils.substringBefore(list, "*");
				String lastChar = StringUtils.substringAfter(list, "*");

				if (firstChar != null && lastChar != null && prodNr != null) {

					if (StringUtils.startsWith(prodNr, firstChar) && StringUtils.endsWith(prodNr, lastChar)) {
						returnValue = true;
						break;
					}
				}
			}

		} else if (productNr.startsWith("CONSTANT:#")) {
			String product = StringUtils.replace(productNr, "CONSTANT:#", "");
			if (product != null && StringUtils.equalsIgnoreCase(product, prodNr)) {
				returnValue = true;
			}

		}
		return returnValue;

	}
	
	private boolean isZeroPriceItem(QuoteItem quoteItem){
		BigDecimal zeroPrice = new BigDecimal("0");
		
		if(quoteItem.getQuotedLclUntLstAmt() == null || quoteItem.getQuotedLclUntLstAmt().compareTo(zeroPrice) == 0){
			if(quoteItem.getQuotedLclUntLstAmt() == null || quoteItem.getQuotedLclUntLstAmt().compareTo(zeroPrice) == 0){
				return true;
			}
		}else if(quoteItem.getLclUntLstAmt() == null || quoteItem.getLclUntLstAmt().compareTo(zeroPrice) == 0){
			if(quoteItem.getLclUntNtAmt() == null || quoteItem.getLclUntNtAmt().compareTo(zeroPrice) == 0){
				return true;
			}
		}
		
		return false;
	}
	
	private boolean productLineTypeCheck(QuoteItem lineItem) {
		if (StringUtils.equalsIgnoreCase(lineItem.getLineTypeCd(), "Product")
				|| StringUtils.equalsIgnoreCase(lineItem.getLineTypeCd(), "PN")) {

			return true;
		}

		return false;
	}
	
	private boolean optionLineTypeCheck(QuoteItem lineItem) {
		if (StringUtils.equalsIgnoreCase(lineItem.getLineTypeCd(), "Option")
				|| StringUtils.equalsIgnoreCase(lineItem.getLineTypeCd(), "OP")) {

			return true;
		}

		return false;
	}

	/*
	private void getAllSelectedValidationSummary(Quote quote) {
		RulesFilter rFilter = RuleUtil.createNewRulesFilter(quote, "FVO_VLDT_PRCSS");
		List<IGenericDisplayResponse> vSummaryLIst = new ArrayList<IGenericDisplayResponse>();
		FVOProcessValidationRequestList fvoProcessValidationRqstList = checklistService.getAllValidationsList(quote,
				rFilter);
		for (FVOProcessValidationRequest fvoProcReqObj : fvoProcessValidationRqstList) {
			try {
				quote.getvSummaryLIst().add(checklistService.getValidationSummaryByQuote(fvoProcReqObj, quote));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}*/
	private void setDeliverySpeed(Quote quote, QuoteHeader qidsQuote)
	{
		if (!CommonUtil.isEmptyString(qidsQuote.getDeliverySpeed() ) ) {
			boolean matchFound=false;
			quote.setDeliverySpeed(  transformUtilService.tranformValueWithDefault("DLVRY_SPD", "S4", "AOE",  qidsQuote.getDeliverySpeed()) ) ;
			LocalizationFilter filterbase = applicationService.getLocalizationFilterByCookies(quote);
			List<DlvrySpeed> dlvrySpeedList = dropDownListService.getDlvrySpeed(filterbase);		
			if (dlvrySpeedList != null ) {
				for (DlvrySpeed dlvrySpd : dlvrySpeedList) {
					if (quote.getDeliverySpeed().equalsIgnoreCase(dlvrySpd.getDlvrySpeed())) {
						matchFound = true ;
						break ;
					} 
				}
			}
			if (!matchFound) {
				quote.setDeliverySpeed(null);
			}
		}
	}
	
	private Quote setAdditionalFieldsForBRIMQuote(QuoteHeader quoteHeader, Quote quote) {
		
		// Reading from the header
		/*
		 * String solnId = qidsQuote.getSolnId(); 
		 * String statOrderId = qidsQuote.getStatOrderId(); 
		 * String aaSSupplyChainHandling = qidsQuote.getAsupChHandl(); 
		 */

		//Dragon requirement changes
		String  solnId  = quoteHeader.getSolutionObjectID();
		String statOrderId  = quoteHeader.getStatisticalOrderID();
		String aaSSupplyChainHandling = quoteHeader.getAaSSupplyChainHandling();
		String poDate  = CommonUtil.getValueFromJSON(quoteHeader.getAdditionalInfo(), "poDate").toString();
		String custPurAgtNm = CommonUtil.getValueFromJSON(quoteHeader.getAdditionalInfo(), "custPurAgtNm").toString();
		String custPurAgtEm = CommonUtil.getValueFromJSON(quoteHeader.getAdditionalInfo(), "custPurAgEm").toString();
		String poType = CommonUtil.getValueFromJSON(quoteHeader.getAdditionalInfo(), "poemail").toString();
		// Dragon-R2 new fields mapping - start
		quote.getQtPrchOrdReqt().setMasterAgreement(quoteHeader.getMasterAgreement());
		quote.getQtPrchOrdReqt().setChangeOrderScenarioID(quoteHeader.getChangeOrderScenarioID());
		// Dragon-R2 new fields mapping - end
		
		quote.getQtPrchOrdReqt().setCstmrPrchsAgntFirstName(custPurAgtNm);
		quote.getQtPrchOrdReqt().setCstmrPrchsAgntEmail(custPurAgtEm);
		quote.getPrchOrdAtachmt().setPoCrtDt(CommonUtil.fromISO8601UTC(poDate));
		quote.getQtPrchOrdReqt().setAaSSupplyChainHandling(aaSSupplyChainHandling);
		quote.getQtPrchOrdReqt().setSolutionObjectId(solnId);
		quote.getQtPrchOrdReqt().setStatisticalOrderId(statOrderId);
		quote.setOrigAsset(poType);
		
		return quote;
	}

	// US-18175 - code changes to STOP EC & RESELLER validations between EDI PAYLOAD
	// AND QIDSQUOTE - STARTS
	/**
	 * method provided to prepare Customer And CustomerAddress details based on
	 * given customerType of either EndCustomer or Reseller
	 * 
	 * @params quote, qidsQuote
	 */
	public void prepareECAndResellerAddressFromQids(Quote quote, HPQuote qidsQuote) {

		LOG.info("Inside PrepareOMUIQuote.prepareECAndResellerAddressFromQids method" );
		
		if (!StringUtils.equalsIgnoreCase(quote.getQtPrchOrdReqt().getOrigPOCategory(), "STCK")
				&& StringUtils.equalsIgnoreCase("EDI", quote.getQtPrchOrdReqt().getOrderSource())
				&& (StringUtils.startsWith(quote.getAssetQuoteNr(), "D"))) {
			if (quote.getCustomers().get(CustomerType.ENDCUSTOMER) != null
					|| quote.getCustomers().get(CustomerType.RESELLER) != null) {
				setECAndResellerAddressFromQids(quote, qidsQuote);
			}
		}

		else if (!StringUtils.equalsIgnoreCase(quote.getQtPrchOrdReqt().getOrigPOCategory(), "STCK")
				&& StringUtils.equalsIgnoreCase("EDI", quote.getQtPrchOrdReqt().getOrderSource())) {
			setECAndResellerAddressFromQids(quote, qidsQuote);
		}
	}
	// US-18175 - code changes to STOP EC & RESELLER validations between EDI PAYLOAD
	// AND QIDSQUOTE - ENDS

	// US-18175 - code changes to STOP EC & RESELLER validations between EDI PAYLOAD
	// AND QIDSQUOTE - STARTS
	/**
	 * method provided to set Customer And CustomerAddress details based on given
	 * customerType of either EndCustomer or Reseller
	 * 
	 * @params quote, qidsQuote
	 */
	public void setECAndResellerAddressFromQids(Quote quote, HPQuote qidsQuote) {
		
		LOG.info("Inside PrepareOMUIQuote.setECAndResellerAddressFromQids method" );

        String sldToPartyId;

		if ((qidsQuote.getQuoteHeader().getEndCustomer() != null
				&& qidsQuote.getQuoteHeader().getEndCustomer().getCompany() != null
				&& qidsQuote.getQuoteHeader().getEndCustomer().getCompany().getPartyID() != null) ||

				(qidsQuote.getQuoteHeader().getResellerInfo() != null
						&& qidsQuote.getQuoteHeader().getResellerInfo().getCompany() != null
						&& qidsQuote.getQuoteHeader().getResellerInfo().getCompany().getPartyID() != null)) {

            sldToPartyId = quote.getCustomers().get(CustomerType.SOLDTO).getPartyId(); //fetching SLDTOPARTYID from EDI payload

			LOG.info("PrepareOMUIQuote.setECAndResellerAddressFromQids ::::: EDI_PAYLOAD_SLDTOPARTYID  :: "
					+ sldToPartyId );

			Set<String> inclusivePartyIdsSet = checklistService.getInclusivePartyIds();
			LOG.info("PrepareOMUIQuote.setECAndResellerAddressFromQids ::::: InclusivePartyIds :: "
					+ inclusivePartyIdsSet);

			if (inclusivePartyIdsSet.contains(sldToPartyId)) {
				List<InclusivePartyIdModel> inclusivePartyIdModels = dropDownListService.getInclusivePartyIds();
				for (InclusivePartyIdModel inclusivePartyIdModel : inclusivePartyIdModels) {
					if ((StringUtils.equalsIgnoreCase(sldToPartyId, inclusivePartyIdModel.getSoldToPartyId()))
							&& StringUtils.equalsIgnoreCase("T", inclusivePartyIdModel.getEndCustomer())) {

						quote.getCustomerAddresses().put(CustomerType.ENDCUSTOMER, convertCustomerAddress(
								CustomerType.ENDCUSTOMER, qidsQuote.getQuoteHeader().getEndCustomer()));
						quote.getCustomers().put(CustomerType.ENDCUSTOMER,
								convertECAndResellerCustomer(CustomerType.ENDCUSTOMER, qidsQuote));
					}
					if ((StringUtils.equalsIgnoreCase(sldToPartyId, inclusivePartyIdModel.getSoldToPartyId()))
							&& StringUtils.equalsIgnoreCase("T", inclusivePartyIdModel.getReseller())) {

						quote.getCustomerAddresses().put(CustomerType.RESELLER, convertCustomerAddress(
								CustomerType.RESELLER, qidsQuote.getQuoteHeader().getResellerInfo()));
						quote.getCustomers().put(CustomerType.RESELLER,
								convertECAndResellerCustomer(CustomerType.RESELLER, qidsQuote));
					}
				}
			}
		}
	}
	// US-18175 - code changes to STOP EC & RESELLER validations between EDI PAYLOAD
	// AND QIDSQUOTE - ENDS

	// US-18175 - code changes to STOP EC & RESELLER validations between EDI PAYLOAD
	// AND QIDSQUOTE - STARTS
	/**
	 * method provided to convert customerType based on given customerType of either
	 * EndCustomer or Reseller
	 * 
	 * @params customerType, qidsQuote
	 * @return method to return QuoteCustomer
	 */
	public QuoteCustomer convertECAndResellerCustomer(CustomerType customerType, HPQuote qidsQuote) {
		
		LOG.info("Inside PrepareOMUIQuote.convertECAndResellerCustomer method" );
		
		QuoteCustomer customer = new QuoteCustomer();

		if (ObjectUtils.equals(customerType, CustomerType.ENDCUSTOMER)) {

			//US-18175 - Added to fetch EC partyId and  address info details if EC PartyID is not null in QIDS response.
			if ((qidsQuote.getQuoteHeader().getEndCustomer() != null
					&& qidsQuote.getQuoteHeader().getEndCustomer().getCompany() != null
					&& qidsQuote.getQuoteHeader().getEndCustomer().getCompany().getPartyID() != null)) {
				customer = convertCustomer(CustomerType.ENDCUSTOMER, qidsQuote.getQuoteHeader().getEndCustomer());
				customer.setPartyId(qidsQuote.getQuoteHeader().getEndCustomer().getCompany().getPartyID());
			}
			//US-18175 - Added to fetch only address info details if EC PartyID is null in QIDS response.
			else if ((qidsQuote.getQuoteHeader().getEndCustomer() != null
					&& qidsQuote.getQuoteHeader().getEndCustomer().getCompany() != null)) {
				customer = convertCustomer(CustomerType.ENDCUSTOMER, qidsQuote.getQuoteHeader().getEndCustomer());
			}
		}

		if (ObjectUtils.equals(customerType, CustomerType.RESELLER)) {

			//US-18175 - Added to fetch RES partyId and  address info details if RES PartyID is not null in QIDS response.
			if ((qidsQuote.getQuoteHeader().getResellerInfo() != null
					&& qidsQuote.getQuoteHeader().getResellerInfo().getCompany() != null
					&& qidsQuote.getQuoteHeader().getResellerInfo().getCompany().getPartyID() != null)) {
				customer = convertCustomer(CustomerType.RESELLER, qidsQuote.getQuoteHeader().getResellerInfo());
				customer.setPartyId(qidsQuote.getQuoteHeader().getResellerInfo().getCompany().getPartyID());
			}

			//US-18175 - Added to fetch only address info details if RES PartyID is null in QIDS response.
			else if ((qidsQuote.getQuoteHeader().getResellerInfo() != null
					&& qidsQuote.getQuoteHeader().getResellerInfo().getCompany() != null)) {
				customer = convertCustomer(CustomerType.RESELLER, qidsQuote.getQuoteHeader().getResellerInfo());
			}

		}
		eMDMService.setCompanyName(customer, customer.getCompanyName());//INC6826396 
		return customer;
	}
	// US-18175 - code changes to STOP EC & RESELLER validations between EDI PAYLOAD
	// AND QIDSQUOTE - ENDS

	//US-18346 - DQM Validation for S4 Deal Number - STARTS

	/**
	 * this method process the corresponding S4 deal number response as a JSON payload
	 * and extracts deal version information by comparing it with the QIDS quote version.
	 * applyRuleResponse the ApplyRuleResponse object to hold any validation errors
	 *
	 * @param quote the Quote object containing the S4 deal number and dealVersionNr
	 */
	public void processS4DealNumberForQuote(Quote quote) {
		LOG.info("Inside PrepareOMUIQuote.processS4DealNumberForQuote method - START");
		if (StringUtils.startsWith(quote.getAssetQuoteNr(), "D")) {
			ApplyRuleResponse applyRuleResponse = new ApplyRuleResponse();
			checklistService.processAndValidateDealVersionNr(quote, applyRuleResponse);
		}
		LOG.info("Inside PrepareOMUIQuote.processS4DealNumberForQuote method - END");
	}
	//US-18346 - DQM Validation for S4 Deal Number - ENDS
}