package com.hack17.poc;

import static com.hack17.hybo.util.DateTimeUtil.getDateMMMddyyyy;
import static org.junit.Assert.*;

import java.util.Date;
import java.util.List;

















import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;














//import org.apache.log4j.Logger;
import org.apache.log4j.RollingFileAppender;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;

import com.hack17.hybo.domain.Action;
import com.hack17.hybo.domain.Allocation;
import com.hack17.hybo.domain.CreatedBy;
import com.hack17.hybo.domain.Fund;
import com.hack17.hybo.domain.IncomeTaxSlab;
import com.hack17.hybo.domain.InvestorProfile;
import com.hack17.hybo.domain.Portfolio;
import com.hack17.hybo.domain.RiskTolerance;
import com.hack17.hybo.domain.TLHAdvice;
import com.hack17.hybo.domain.TLHRunPortfolioHistory;
import com.hack17.hybo.domain.Transaction;
import com.hack17.hybo.repository.FundRepository;
import com.hack17.hybo.repository.IncomeTaxSlabRepository;
import com.hack17.hybo.repository.InvestorRepository;
import com.hack17.hybo.repository.PortfolioRepository;
import com.hack17.hybo.repository.ReferenceDataRepository;
import com.hack17.hybo.repository.TLHAdvisorRepository;
import com.hack17.hybo.service.DBLoggerService;
import com.hack17.hybo.service.TLHAdvisorService;
import com.hack17.hybo.util.DateTimeUtil;
import com.hack17.poc.util.ReportUtil;


@RunWith(SpringRunner.class)
@ContextConfiguration(classes={TestTaxHarvestingPocApplication.class})
public class SimpleSellAndBuyThresholdStrategyTests {
	private static Logger logger = LoggerFactory.getLogger(SimpleSellAndBuyThresholdStrategyTests.class);
	//private static Logger logger = Logger.getLogger(SimpleSellAndBuyThresholdStrategyTests.class);

	@Autowired
	InvestorRepository investorRepo;
	
	@Autowired
	FundRepository fundRepo;
	
	@Autowired
	ReferenceDataRepository refDataRepo;
	
	@Autowired
	PortfolioRepository portfolioRepo;
	@Autowired
	private TLHAdvisorService tlhAdvisorService;
	@Autowired
	private TLHAdvisorRepository tlhAdivsorRepo;
	
	@Autowired
	private IncomeTaxSlabRepository incomeTaxSlabRepo;
	
	@Autowired
	private DBLoggerService dbLoggerService;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}
	

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}
	
	long investorProfileId;
	long portfolioId;

	@Before
	@Transactional
	public void setUp() throws Exception {
		InvestorProfile investorProfile = investorRepo.createProfile(getDateMMMddyyyy("Apr 7, 1972"), RiskTolerance.MODERATE, 120, getDateMMMddyyyy("Jan 1, 2012"), 200000);
		investorProfileId = investorProfile.getId();
		fundRepo.createFund("VTI");
		fundRepo.createFund("SCHB");
		Portfolio portfolio = new Portfolio();
		portfolio.setInvestorProfile(investorProfile);
		portfolioRepo.persist(portfolio);
		portfolioId = portfolio.getId();
		refDataRepo.createPrice("VTI", 73.23d, getDateMMMddyyyy("Nov 01, 2012"));
		refDataRepo.createCorrelatedFund("VTI", "SCHB");
		insertTaxSlabs();
	}

	private void insertTaxSlabs() {
		//single
		IncomeTaxSlab taxSlab = new IncomeTaxSlab(IncomeTaxSlab.TaxProfileType.SINGLE, 0d, 9325d, 10d);
		incomeTaxSlabRepo.persist(taxSlab);
		taxSlab = new IncomeTaxSlab(IncomeTaxSlab.TaxProfileType.SINGLE, 9326d, 37950d, 15d);
		incomeTaxSlabRepo.persist(taxSlab);
		taxSlab = new IncomeTaxSlab(IncomeTaxSlab.TaxProfileType.SINGLE, 37951d, 91900d, 25d);
		incomeTaxSlabRepo.persist(taxSlab);
		taxSlab = new IncomeTaxSlab(IncomeTaxSlab.TaxProfileType.SINGLE, 91901d, 191650d, 28d);
		incomeTaxSlabRepo.persist(taxSlab);
		taxSlab = new IncomeTaxSlab(IncomeTaxSlab.TaxProfileType.SINGLE, 191651d, 416700d, 33d);
		incomeTaxSlabRepo.persist(taxSlab);
		taxSlab = new IncomeTaxSlab(IncomeTaxSlab.TaxProfileType.SINGLE, 416701d, 418400d, 35d);
		incomeTaxSlabRepo.persist(taxSlab);
		taxSlab = new IncomeTaxSlab(IncomeTaxSlab.TaxProfileType.SINGLE, 418401d, null, 39.6d);
		incomeTaxSlabRepo.persist(taxSlab);
		
		//Married Filing Jointly / Qualifying Widow
		taxSlab = new IncomeTaxSlab(IncomeTaxSlab.TaxProfileType.MARRIED_FILING_JOINTLY_OR_QUALIFYING_WIDOW, 0d, 18650d, 10d);
		incomeTaxSlabRepo.persist(taxSlab);
		taxSlab = new IncomeTaxSlab(IncomeTaxSlab.TaxProfileType.MARRIED_FILING_JOINTLY_OR_QUALIFYING_WIDOW, 18651d, 75900d, 15d);
		incomeTaxSlabRepo.persist(taxSlab);
		taxSlab = new IncomeTaxSlab(IncomeTaxSlab.TaxProfileType.MARRIED_FILING_JOINTLY_OR_QUALIFYING_WIDOW, 75901d, 153100d, 25d);
		incomeTaxSlabRepo.persist(taxSlab);
		taxSlab = new IncomeTaxSlab(IncomeTaxSlab.TaxProfileType.MARRIED_FILING_JOINTLY_OR_QUALIFYING_WIDOW, 153101d, 233350d, 28d);
		incomeTaxSlabRepo.persist(taxSlab);
		taxSlab = new IncomeTaxSlab(IncomeTaxSlab.TaxProfileType.MARRIED_FILING_JOINTLY_OR_QUALIFYING_WIDOW, 233351d, 416700d, 33d);
		incomeTaxSlabRepo.persist(taxSlab);
		taxSlab = new IncomeTaxSlab(IncomeTaxSlab.TaxProfileType.MARRIED_FILING_JOINTLY_OR_QUALIFYING_WIDOW, 416701d, 470700d, 35d);
		incomeTaxSlabRepo.persist(taxSlab);
		taxSlab = new IncomeTaxSlab(IncomeTaxSlab.TaxProfileType.MARRIED_FILING_JOINTLY_OR_QUALIFYING_WIDOW, 470701d, null, 39.6d);
		incomeTaxSlabRepo.persist(taxSlab);
		
		//Married Filing Separately
		taxSlab = new IncomeTaxSlab(IncomeTaxSlab.TaxProfileType.MARRIED_FILING_SEPARATELY, 0d, 9325d, 10d);
		incomeTaxSlabRepo.persist(taxSlab);
		taxSlab = new IncomeTaxSlab(IncomeTaxSlab.TaxProfileType.MARRIED_FILING_SEPARATELY, 9326d, 37950d, 15d);
		incomeTaxSlabRepo.persist(taxSlab);
		taxSlab = new IncomeTaxSlab(IncomeTaxSlab.TaxProfileType.MARRIED_FILING_SEPARATELY, 37951d, 76550d, 25d);
		incomeTaxSlabRepo.persist(taxSlab);
		taxSlab = new IncomeTaxSlab(IncomeTaxSlab.TaxProfileType.MARRIED_FILING_SEPARATELY, 76551d, 116675d, 28d);
		incomeTaxSlabRepo.persist(taxSlab);
		taxSlab = new IncomeTaxSlab(IncomeTaxSlab.TaxProfileType.MARRIED_FILING_SEPARATELY, 116676d, 208350d, 33d);
		incomeTaxSlabRepo.persist(taxSlab);
		taxSlab = new IncomeTaxSlab(IncomeTaxSlab.TaxProfileType.MARRIED_FILING_SEPARATELY, 208351d, 235350d, 35d);
		incomeTaxSlabRepo.persist(taxSlab);
		taxSlab = new IncomeTaxSlab(IncomeTaxSlab.TaxProfileType.MARRIED_FILING_SEPARATELY, 235351d, null, 39.6d);
		incomeTaxSlabRepo.persist(taxSlab);
		
		//Head of Household
		taxSlab = new IncomeTaxSlab(IncomeTaxSlab.TaxProfileType.HEAD_OF_HOUSEHOLD, 0d, 13350d, 10d);
		incomeTaxSlabRepo.persist(taxSlab);
		taxSlab = new IncomeTaxSlab(IncomeTaxSlab.TaxProfileType.HEAD_OF_HOUSEHOLD, 13351d, 50800d, 15d);
		incomeTaxSlabRepo.persist(taxSlab);
		taxSlab = new IncomeTaxSlab(IncomeTaxSlab.TaxProfileType.HEAD_OF_HOUSEHOLD, 50801d, 131200d, 25d);
		incomeTaxSlabRepo.persist(taxSlab);
		taxSlab = new IncomeTaxSlab(IncomeTaxSlab.TaxProfileType.HEAD_OF_HOUSEHOLD, 131201d, 212500d, 28d);
		incomeTaxSlabRepo.persist(taxSlab);
		taxSlab = new IncomeTaxSlab(IncomeTaxSlab.TaxProfileType.HEAD_OF_HOUSEHOLD, 212501d, 416700d, 33d);
		incomeTaxSlabRepo.persist(taxSlab);
		taxSlab = new IncomeTaxSlab(IncomeTaxSlab.TaxProfileType.HEAD_OF_HOUSEHOLD, 416701d, 444550d, 35d);
		incomeTaxSlabRepo.persist(taxSlab);
		taxSlab = new IncomeTaxSlab(IncomeTaxSlab.TaxProfileType.HEAD_OF_HOUSEHOLD, 444551d, null, 39.6d);
		incomeTaxSlabRepo.persist(taxSlab);
	}


	@After
	public void tearDown() throws Exception {
//		refDataRepo.deleteAllCorrelatedFunds();
//		refDataRepo.deleteAllPrices();
//		portfolioRepo.deleteAllPorfolios();
//		fundRepo.deleteAllFunds();
//		investorRepo.deleteAllProfiles();
	}
	
	@Test 
	@Transactional
	public void testBasicDataCreation(){
		assertNotNull(investorRepo.getProfileById(investorProfileId));
		assertNotNull(fundRepo.findFund("VTI"));
		assertNotNull(portfolioRepo.getPortfolio(portfolioId));
		assertNotNull(refDataRepo.getLatestPrice("VTI"));
	}

	@Test 
	@Transactional
	public void noStockInlossBeyondMinThreshold() {
		Allocation alloc = new Allocation(fundRepo.findFund("VTI"),73.23,1000,50d, getDateMMMddyyyy("Nov 01, 2012"), .04,0, CreatedBy.PORT.toString(), getDateMMMddyyyy("Nov 01, 2012"));
		Portfolio portfolio = portfolioRepo.getPortfolio(portfolioId);
		portfolio.addAllocation(alloc);
		portfolioRepo.persist(portfolio);
		refDataRepo.createPrice("VTI", 73.15d, getDateMMMddyyyy("Dec 01, 2012"));
		Date dateOfExec = DateTimeUtil.getDatedd_MMM_yyyy("01-Dec-2012");
		TLHAdvice tlhAdvice = tlhAdvisorService.advise(portfolio, dateOfExec);
		assertEquals(0,  tlhAdvice.getRecommendations().size());
	}
	
	@Test 
	@Transactional
	public void recommendationForSellMinThresholdOnWages() {
		Allocation alloc = new Allocation(fundRepo.findFund("VTI"),73.23,1000,50d, getDateMMMddyyyy("Nov 01, 2012"), .04,0, CreatedBy.PORT.toString(), getDateMMMddyyyy("Nov 01, 2012"));
		Portfolio portfolio = portfolioRepo.getPortfolio(portfolioId);
		portfolio.addAllocation(alloc);
		portfolioRepo.persist(portfolio);
		refDataRepo.createPrice("VTI", 72.5d, getDateMMMddyyyy("Nov 30, 2012"));
		Date dateOfExec = DateTimeUtil.getDatedd_MMM_yyyy("30-Nov-2012");
		TLHAdvice tlhAdvice = tlhAdvisorService.advise(portfolio, dateOfExec);
		assertEquals(1,  tlhAdvice.getRecommendations().size());
		assertEquals(1000,  tlhAdvice.getRecommendations().get(0).getQuantity());
	}
	
	@Test 
	@Transactional
	public void twoRecommendationForSellMinThresholdOnWages() {
		fundRepo.createFund("VEA");
		fundRepo.createFund("SCHF");
		refDataRepo.createCorrelatedFund("VEA", "SCHF");
		Allocation alloc1 = new Allocation(fundRepo.findFund("VTI"),73.23,1000,50d, getDateMMMddyyyy("Nov 01, 2012"), .04,0, CreatedBy.PORT.toString(), getDateMMMddyyyy("Nov 01, 2012"));
		Allocation alloc2 = new Allocation(fundRepo.findFund("VEA"),33.62,1000,50d, getDateMMMddyyyy("Nov 01, 2012"), .04,0, CreatedBy.PORT.toString(), getDateMMMddyyyy("Nov 01, 2012"));
		Portfolio portfolio = portfolioRepo.getPortfolio(portfolioId);
		portfolio.addAllocation(alloc1);
		portfolio.addAllocation(alloc2);
		portfolioRepo.persist(portfolio);
		refDataRepo.createPrice("VTI", 72.5d, getDateMMMddyyyy("Nov 30, 2012"));
		refDataRepo.createPrice("VEA", 32.13d, getDateMMMddyyyy("Nov 30, 2012"));
		Date dateOfExec = DateTimeUtil.getDatedd_MMM_yyyy("30-Nov-2012");
		TLHAdvice tlhAdvice = tlhAdvisorService.advise(portfolio, dateOfExec);
		assertEquals(2,  tlhAdvice.getRecommendations().size());
		assertEquals(1000,  tlhAdvice.getRecommendations().get(0).getQuantity());
		assertEquals(1000,  tlhAdvice.getRecommendations().get(1).getQuantity());
	}
	
	@Test 
	@Transactional
	public void recommendationForSellMinThresholdOnLongCapitalGains() {
		fundRepo.createFund("VEA");
		fundRepo.createFund("SCHF");
		refDataRepo.createCorrelatedFund("VEA", "SCHF");
		Allocation alloc1 = new Allocation(fundRepo.findFund("VTI"),73.23,1000,50d, getDateMMMddyyyy("Nov 01, 2012"), .04,0, CreatedBy.PORT.toString(), getDateMMMddyyyy("Nov 01, 2012"));
		Allocation alloc2 = new Allocation(fundRepo.findFund("VEA"),33.62,1000,50d, getDateMMMddyyyy("Nov 01, 2012"), .04,0, CreatedBy.PORT.toString(), getDateMMMddyyyy("Nov 01, 2012"));
		Portfolio portfolio = portfolioRepo.getPortfolio(portfolioId);
		portfolio.addAllocation(alloc1);
		portfolio.addAllocation(alloc2);
		Allocation allocForTrans = new Allocation(fundRepo.findFund("VTI"),70.00,1000,50d, getDateMMMddyyyy("Nov 01, 2010"), .04,0, CreatedBy.PORT.toString(), portfolio);
		allocForTrans.setBuyDate(getDateMMMddyyyy("Nov 01, 2010"));
		dbLoggerService.logTransaction(allocForTrans, 73, getDateMMMddyyyy("Oct 15, 2012"), 500, Action.SELL, CreatedBy.TLH);
		portfolioRepo.persist(portfolio);
		refDataRepo.createPrice("VTI", 70.5d, getDateMMMddyyyy("Nov 30, 2012"));
		refDataRepo.createPrice("VEA", 31.13d, getDateMMMddyyyy("Nov 30, 2012"));
		Date dateOfExec = DateTimeUtil.getDatedd_MMM_yyyy("30-Nov-2012");
		TLHAdvice tlhAdvice = tlhAdvisorService.advise(portfolio, dateOfExec);
		assertEquals(2,  tlhAdvice.getRecommendations().size());
		assertEquals(1000,  tlhAdvice.getRecommendations().get(0).getQuantity());
		assertEquals(710,  tlhAdvice.getRecommendations().get(1).getQuantity());
	}
	
	@Test 
	@Transactional
	public void recommendationForSellMinThresholdOnShortCapitalGains() {
		fundRepo.createFund("VEA");
		fundRepo.createFund("SCHF");
		refDataRepo.createCorrelatedFund("VEA", "SCHF");
		Allocation alloc1 = new Allocation(fundRepo.findFund("VTI"),73.23,1000,50d, getDateMMMddyyyy("Nov 01, 2012"), .04,0, CreatedBy.PORT.toString(), getDateMMMddyyyy("Nov 01, 2012"));
		Allocation alloc2 = new Allocation(fundRepo.findFund("VEA"),33.62,1000,50d, getDateMMMddyyyy("Nov 01, 2012"), .04,0, CreatedBy.PORT.toString(), getDateMMMddyyyy("Nov 01, 2012"));
		Portfolio portfolio = portfolioRepo.getPortfolio(portfolioId);
		portfolio.addAllocation(alloc1);
		portfolio.addAllocation(alloc2);
		Allocation allocForTrans = new Allocation(fundRepo.findFund("VTI"),70.00,1000,50d, getDateMMMddyyyy("Nov 01, 2011"), .04,0, CreatedBy.PORT.toString(), portfolio);
		allocForTrans.setBuyDate(getDateMMMddyyyy("Nov 01, 2011"));
		dbLoggerService.logTransaction(allocForTrans, 73, getDateMMMddyyyy("Oct 15, 2012"), 500, Action.SELL, CreatedBy.TLH);
		portfolioRepo.persist(portfolio);
		refDataRepo.createPrice("VTI", 72.5d, getDateMMMddyyyy("Nov 30, 2012"));
		refDataRepo.createPrice("VEA", 32.13d, getDateMMMddyyyy("Nov 30, 2012"));
		Date dateOfExec = DateTimeUtil.getDatedd_MMM_yyyy("30-Nov-2012");
		TLHAdvice tlhAdvice = tlhAdvisorService.advise(portfolio, dateOfExec);
		assertEquals(2,  tlhAdvice.getRecommendations().size());
		assertEquals(1000,  tlhAdvice.getRecommendations().get(0).getQuantity());
		assertEquals(1000,  tlhAdvice.getRecommendations().get(1).getQuantity());
	}
	
	@Test 
	@Transactional
	public void recommendationForSellMinThresholdOnShortCapitalGainsDemoScenario() {
		fundRepo.createFund("VBR");
		fundRepo.createFund("IWN");
		refDataRepo.createCorrelatedFund("VBR", "IWN");
		Allocation alloc1 = new Allocation(fundRepo.findFund("VBR"),73.23,1000,50d, getDateMMMddyyyy("Nov 01, 2012"), .04,0, CreatedBy.PORT.toString(), getDateMMMddyyyy("Nov 01, 2012"));
		Allocation alloc2 = new Allocation(fundRepo.findFund("IWN"),33.62,1000,50d, getDateMMMddyyyy("Nov 01, 2012"), .04,0, CreatedBy.PORT.toString(), getDateMMMddyyyy("Nov 01, 2012"));
		Portfolio portfolio = portfolioRepo.getPortfolio(portfolioId);
		portfolio.addAllocation(alloc1);
		portfolio.addAllocation(alloc2);
		Allocation allocForTrans = new Allocation(fundRepo.findFund("VBR"),78.3,70,50d, getDateMMMddyyyy("Jan 01, 2015"), .04,0, CreatedBy.PORT.toString(), portfolio);
		allocForTrans.setBuyDate(getDateMMMddyyyy("Jan 01, 2015"));
		dbLoggerService.logTransaction(allocForTrans, 100, getDateMMMddyyyy("Feb 10, 2015"), 70, Action.SELL, CreatedBy.TLH);
		portfolioRepo.persist(portfolio);
		refDataRepo.createPrice("VBR", 72.5d, getDateMMMddyyyy("Feb 20, 2015"));
		refDataRepo.createPrice("IWN", 32.13d, getDateMMMddyyyy("Feb 20, 2015"));
		Date dateOfExec = DateTimeUtil.getDatedd_MMM_yyyy("20-Feb-2015");
		TLHAdvice tlhAdvice = tlhAdvisorService.advise(portfolio, dateOfExec);
		assertEquals(1,  tlhAdvice.getRecommendations().size());
		//assertEquals(1000,  tlhAdvice.getRecommendations().get(0).getQuantity());
		//assertEquals(1000,  tlhAdvice.getRecommendations().get(1).getQuantity());
	}
	
	@Test 
	@Transactional
	public void recommendationForSellMinThresholdOnLongCapitalLoss() {
		fundRepo.createFund("VEA");
		fundRepo.createFund("SCHF");
		refDataRepo.createCorrelatedFund("VEA", "SCHF");
		Allocation alloc1 = new Allocation(fundRepo.findFund("VTI"),73.23,5000,50d, getDateMMMddyyyy("Nov 01, 2012"), .04,0, CreatedBy.PORT.toString(), getDateMMMddyyyy("Nov 01, 2012"));
		Allocation alloc2 = new Allocation(fundRepo.findFund("VEA"),33.62,5000,50d, getDateMMMddyyyy("Nov 01, 2012"), .04,0, CreatedBy.PORT.toString(), getDateMMMddyyyy("Nov 01, 2012"));
		Portfolio portfolio = portfolioRepo.getPortfolio(portfolioId);
		portfolio.addAllocation(alloc1);
		portfolio.addAllocation(alloc2);
		Allocation allocForTrans = new Allocation(fundRepo.findFund("VTI"),70.00,1000,50d, getDateMMMddyyyy("Nov 01, 2009"), .04,0, CreatedBy.PORT.toString(), portfolio);
		allocForTrans.setBuyDate(getDateMMMddyyyy("Nov 01, 2009"));
		dbLoggerService.logTransaction(allocForTrans, 65, getDateMMMddyyyy("Oct 15, 2012"), 500, Action.SELL, CreatedBy.TLH);
		portfolioRepo.persist(portfolio);
		refDataRepo.createPrice("VTI", 70.5d, getDateMMMddyyyy("Nov 30, 2012"));
		refDataRepo.createPrice("VEA", 31.13d, getDateMMMddyyyy("Nov 30, 2012"));
		Date dateOfExec = DateTimeUtil.getDatedd_MMM_yyyy("30-Nov-2012");
		TLHAdvice tlhAdvice = tlhAdvisorService.advise(portfolio, dateOfExec);
		assertEquals(1,  tlhAdvice.getRecommendations().size());
		assertEquals(183,  tlhAdvice.getRecommendations().get(0).getQuantity());
	}
	
	@Test 
	@Transactional
	public void recommendationForSellMinThresholdOnLongCapitalLossMoreThanLongTermGain() {
		fundRepo.createFund("VEA");
		fundRepo.createFund("SCHF");
		refDataRepo.createCorrelatedFund("VEA", "SCHF");
		Allocation alloc1 = new Allocation(fundRepo.findFund("VTI"),73.23,5000,50d, getDateMMMddyyyy("Nov 01, 2012"), .04,0, CreatedBy.PORT.toString(), getDateMMMddyyyy("Nov 01, 2012"));
		Allocation alloc2 = new Allocation(fundRepo.findFund("VEA"),33.62,5000,50d, getDateMMMddyyyy("Nov 01, 2012"), .04,0, CreatedBy.PORT.toString(), getDateMMMddyyyy("Nov 01, 2012"));
		Portfolio portfolio = portfolioRepo.getPortfolio(portfolioId);
		portfolio.addAllocation(alloc1);
		portfolio.addAllocation(alloc2);
		Allocation allocForTrans = new Allocation(fundRepo.findFund("VTI"),70.00,1000,50d, getDateMMMddyyyy("Nov 01, 2009"), .04,0, CreatedBy.PORT.toString(), portfolio);
		allocForTrans.setBuyDate(getDateMMMddyyyy("Nov 01, 2009"));
		dbLoggerService.logTransaction(allocForTrans, 80, getDateMMMddyyyy("Oct 15, 2012"), 500, Action.SELL, CreatedBy.TLH);
		allocForTrans = new Allocation(fundRepo.findFund("VTI"),70.00,1000,50d, getDateMMMddyyyy("Nov 01, 2009"), .04,0, CreatedBy.PORT.toString(), portfolio);
		allocForTrans.setBuyDate(getDateMMMddyyyy("Nov 01, 2009"));
		dbLoggerService.logTransaction(allocForTrans, 65, getDateMMMddyyyy("Oct 15, 2012"), 500, Action.SELL, CreatedBy.TLH);
		portfolioRepo.persist(portfolio);
		refDataRepo.createPrice("VTI", 70.5d, getDateMMMddyyyy("Nov 30, 2012"));
		refDataRepo.createPrice("VEA", 31.13d, getDateMMMddyyyy("Nov 30, 2012"));
		Date dateOfExec = DateTimeUtil.getDatedd_MMM_yyyy("30-Nov-2012");
		TLHAdvice tlhAdvice = tlhAdvisorService.advise(portfolio, dateOfExec);
		assertEquals(1,  tlhAdvice.getRecommendations().size());
		assertEquals(2014,  tlhAdvice.getRecommendations().get(0).getQuantity());
	}
	
	@Test 
	@Transactional
	public void recommendationForSellAsWashSaleRulePassDueToNoTransaction() {
		Allocation alloc = new Allocation(fundRepo.findFund("VTI"),73.23,1000,50d, getDateMMMddyyyy("Nov 01, 2012"), .04,0, CreatedBy.TLH.toString(), getDateMMMddyyyy("Nov 01, 2012"));
		Portfolio portfolio = portfolioRepo.getPortfolio(portfolioId);
		portfolio.addAllocation(alloc);
		portfolioRepo.persist(portfolio);
		refDataRepo.createPrice("VTI", 72.5d, getDateMMMddyyyy("Nov 30, 2012"));
		Date dateOfExec = DateTimeUtil.getDatedd_MMM_yyyy("30-Nov-2012");
		TLHAdvice tlhAdvice = tlhAdvisorService.advise(portfolio, dateOfExec);
		assertEquals(1,  tlhAdvice.getRecommendations().size());
		assertEquals(1000,  tlhAdvice.getRecommendations().get(0).getQuantity());
	}
	
	@Test 
	@Transactional
	public void noRecommendationForSellAsWashSaleRuleDueToTransactionInLast30Days() {
		Fund fund = fundRepo.findFund("VTI");
		Allocation alloc = new Allocation(fund,73.23,1000,50d, getDateMMMddyyyy("Nov 01, 2012"), .04,0, CreatedBy.TLH.toString(), getDateMMMddyyyy("Nov 01, 2012"));
		Portfolio portfolio = portfolioRepo.getPortfolio(portfolioId);
		portfolio.addAllocation(alloc);
		portfolioRepo.persist(portfolio);
		refDataRepo.createPrice("VTI", 72.5d, getDateMMMddyyyy("Nov 30, 2012"));
		
		//create transaction
		Fund correlatedFund = fundRepo.findFund(refDataRepo.getCorrelatedTicker(fund.getTicker()));
		alloc = new Allocation(correlatedFund,31.63,50,50d, getDateMMMddyyyy("Jun 01, 2012"), .04,0, CreatedBy.PORT.toString(), getDateMMMddyyyy("Jun 01, 2012"));
		alloc.setPortfolio(portfolio);
		dbLoggerService.logTransaction(alloc, 32.50, getDateMMMddyyyy("Nov 15, 2012"), 40, Action.SELL, CreatedBy.TLH);
		
		Date dateOfExec = DateTimeUtil.getDatedd_MMM_yyyy("30-Nov-2012");
		TLHAdvice tlhAdvice = tlhAdvisorService.advise(portfolio, dateOfExec);
		assertEquals(0,  tlhAdvice.getRecommendations().size());
	}
	
	@Test  
	@Transactional
	public void executeRecommendationsForMinThreshold() {
		Allocation alloc = new Allocation(fundRepo.findFund("VTI"),73.23,1000,50d, getDateMMMddyyyy("Nov 01, 2012"), .04,0, CreatedBy.PORT.toString(), getDateMMMddyyyy("Nov 01, 2012"));
		Portfolio portfolio = portfolioRepo.getPortfolio(portfolioId);
		portfolio.addAllocation(alloc);
		portfolioRepo.persist(portfolio);
		refDataRepo.createPrice("VTI", 72.5d, getDateMMMddyyyy("Nov 30, 2012"));
		refDataRepo.createPrice("SCHB", 34.20d, getDateMMMddyyyy("Nov 30, 2012"));
		Date dateOfExec = DateTimeUtil.getDatedd_MMM_yyyy("30-Nov-2012");
		TLHAdvice tlhAdvice = tlhAdvisorService.advise(portfolio, dateOfExec);
		assertEquals(1,  tlhAdvice.getRecommendations().size());
		assertEquals(1000,  tlhAdvice.getRecommendations().get(0).getQuantity());
		tlhAdvisorService.execute(tlhAdvice);
		portfolio = portfolioRepo.getPortfolio(portfolioId);
		assertEquals(3, portfolio.getAllocations().size());
		assertEquals(CreatedBy.TLH.toString(), portfolio.getAllocations().get(1).getCreatedBy());
		assertEquals(CreatedBy.TLH.toString(), portfolio.getAllocations().get(2).getCreatedBy());
		assertEquals(2, refDataRepo.getAll(Transaction.class).size());
		Transaction transaction = refDataRepo.getAll(Transaction.class).get(0);
		assertEquals(73.23, transaction.getBuyPrice(),0);
		assertEquals(getDateMMMddyyyy("Nov 01, 2012"), transaction.getBuyDate());
		assertEquals(72.5, transaction.getSellPrice(),0);
		assertEquals(getDateMMMddyyyy("Nov 30, 2012"), transaction.getSellDate());
		assertEquals(1000, transaction.getQuantity(),0);
		assertEquals(Action.SELL, transaction.getAction());
		transaction = refDataRepo.getAll(Transaction.class).get(1);
		assertEquals(34.20, transaction.getBuyPrice(),0);
		assertEquals(getDateMMMddyyyy("Nov 30, 2012"), transaction.getBuyDate());
		assertEquals(0, transaction.getSellPrice(),0);
		assertNull(transaction.getSellDate());
		assertEquals(2119, transaction.getQuantity(),0);
		assertEquals(Action.BUY, transaction.getAction());
		
	}
	
	
	
	@Test @Ignore
	public void noStockInlossBeyondThreshold() {
		Date date1 = DateTimeUtil.getDatedd_MMM_yyyy("01-NOV-2012");
		Date date2 = DateTimeUtil.getDatedd_MMM_yyyy("01-MAY-2013");
		Portfolio portfolio = loadPortfolio();
		logger.info(ReportUtil.report(portfolio, date1, new TLHRunPortfolioHistory()));
		TLHAdvice tlhAdvice = tlhAdvisorService.advise(portfolio, date2);
		assertEquals(1,  tlhAdvice.getRecommendations().size());
		logger.info(ReportUtil.report(portfolio, date2,new TLHRunPortfolioHistory()));
	}
	
	@Test @Ignore
	@Transactional
	public void recommendationForSell() {
		Date date1 = DateTimeUtil.getDatedd_MMM_yyyy("30-OCT-2013");
		Portfolio portfolio = loadPortfolio();
		//logger.info(ReportUtil.format(portfolio, date1));
		TLHAdvice tlhAdvice = tlhAdvisorService.advise(portfolio, date1);
		assertEquals(1,  tlhAdvice.getRecommendations().size());
		assertEquals(Action.SELL, tlhAdvice.getRecommendations().get(0).getAction());
		tlhAdvisorService.execute(tlhAdvice);
		logger.info(ReportUtil.report(portfolio, date1,new TLHRunPortfolioHistory()));
		Date date2 = DateTimeUtil.getDatedd_MMM_yyyy("1-APR-2014");
		tlhAdvice = tlhAdvisorService.advise(portfolio, date2);
		//assertEquals(1,  tlhAdvice.getRecommendations().size());
		//assertEquals(Action.SELL, tlhAdvice.getRecommendations().get(0).getAction());
		tlhAdvisorService.execute(tlhAdvice);
		logger.info(ReportUtil.report(portfolio, date2,new TLHRunPortfolioHistory()));
	}
	
	@Test
	@Ignore
	public void recommendationForSellWithWashSale() {
		long portfolioId = 103;
		Portfolio portfolio = portfolioRepo.getPortfolio(portfolioId);
		TLHAdvice tlhAdvice = tlhAdvisorService.advise(portfolio, new Date());
		assertEquals(2,  tlhAdvice.getRecommendations().size());
		assertEquals(Action.HOLD, tlhAdvice.getRecommendations().get(0).getAction());
		assertEquals(Action.HOLD, tlhAdvice.getRecommendations().get(1).getAction());

	}
	
	private List<Allocation> getAllocations(long portfolioId) {
		Portfolio portfolio = portfolioRepo.getPortfolio(portfolioId);
		List<Allocation> allocations = portfolio.getAllocations();
		return allocations;
	}
	
	private Portfolio loadPortfolio(){
		return portfolioRepo.getAllPortfolios().get(0);
	}

}
